package tech.ydb.topic.write.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.ExponentialBackoffRetry;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.api.WriteStream;
import tech.ydb.topic.write.InitRequest;
import tech.ydb.topic.write.WriteAck;
import tech.ydb.topic.write.WriteMsg;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStreamImpl implements WriteStream {
    private static final Logger logger = LoggerFactory.getLogger(WriteStream.class);

    private final TopicRpc rpc;
    private final RetryPolicy retryPolicy;
    private final AtomicReference<Handler> hdlr = new AtomicReference<>();
    private final AtomicInteger streamIdx = new AtomicInteger(0);
    private final String id = UUID.randomUUID().toString();

    private volatile GrpcReadWriteStream<FromServer, FromClient> grpcStream = null;
    private volatile String lastToken = null;
    private volatile Status lastStatus = null;
    private volatile boolean isStopped = false;

    public WriteStreamImpl(TopicRpc rpc) {
        this.rpc = rpc;
        this.retryPolicy = new DefaultRetryPolicy();
    }

    @Override
    public void start(Handler handler) {
        logger.debug("write stream[{}] start with handler {}", id, handler);
        if (!hdlr.compareAndSet(null, handler)) {
            throw new IllegalStateException("Stream was already started");
        }

        tryStart(0, System.currentTimeMillis());
    }

    private void tryStart(int retry, long startedAt) {
        grpcStream = null;

        GrpcReadWriteStream<FromServer, FromClient> stream = rpc.writeSession(id + "." + streamIdx.incrementAndGet());
        logger.debug("write stream[{}] retry #{} connecting...", id, retry);

        CompletableFuture<Status> finish = stream.start(this::handleMsg);
        if (finish.isDone()) {
            lastStatus = finish.join();
            logger.warn("write stream[{}] retry #{} cannot start with status {}", id, retry, lastStatus);
            if (hdlr.get().onStop(lastStatus)) {
                throw new UnexpectedResultException("Cannot start write stream", lastStatus);
            }

            long elapsed = System.currentTimeMillis() - startedAt;
            long nextRetryMs = retryPolicy.nextRetryMs(retry + 1, elapsed);

            logger.warn("write stream[{}] retry #{} schedule next retry in {}ms...", id, retry, nextRetryMs);
            rpc.getScheduler().schedule(() -> {
                if (!isStopped) {
                    tryStart(retry + 1, startedAt);
                }
            }, nextRetryMs, TimeUnit.MILLISECONDS);
        }

        finish.thenAccept(status -> {
            lastStatus = status;
            logger.warn("write stream[{}] finished with status {}", id, lastStatus);
            isStopped = isStopped || hdlr.get().onStop(status);
            grpcStream = null;

            if (!isStopped) { // try reconnect
                tryStart(0, System.currentTimeMillis());
            }
        });

        grpcStream = stream;
        lastToken = stream.authToken();

        InitRequest init = hdlr.get().createInitRequest();
        logger.trace("write stream[{}] send init {}", id, init);
        grpcStream.sendNext(FromClient.newBuilder().setInitRequest(init.toProtoMsg()).build());
    }

    @Override
    public void write(int codec, YdbTransaction tx, List<WriteMsg> messages) {
        GrpcReadWriteStream<FromServer, FromClient> local = grpcStream;

        if (local == null) {
            logger.error("write stream[{}] is not started", id);
            throw new IllegalStateException("WriteStream is not started");
        }
        if (tx != null && !tx.isActive()) {
            logger.error("write stream[{}] cannot use not started transaction", id);
            throw new IllegalStateException("WriteStream transaction is not started");
        }

        if (!Objects.equals(local.authToken(), lastToken)) {
            lastToken = local.authToken();
            local.sendNext(FromClient.newBuilder().setUpdateTokenRequest(
                    YdbTopic.UpdateTokenRequest.newBuilder().setToken(lastToken).build()
            ).build());
        }

        YdbTopic.StreamWriteMessage.WriteRequest.Builder req = YdbTopic.StreamWriteMessage.WriteRequest.newBuilder()
                .setCodec(codec);

        if (tx != null) {
            req.setTx(YdbTopic.TransactionIdentity.newBuilder()
                    .setSession(tx.getSessionId())
                    .setId(tx.getId())
                    .build()
            );
        }
        messages.forEach(msg -> req.addMessages(msg.toProto()));
        logger.trace("write stream[{}] send {} messages{}", id, messages.size(), tx != null ? " tx " + tx.getId() : "");
        local.sendNext(FromClient.newBuilder().setWriteRequest(req).build());
    }

    @Override
    public void stop() {
        isStopped = true;
        if (grpcStream != null) {
            grpcStream.close();
            grpcStream = null;
        }
    }

    private void handleMsg(FromServer msg) {
        Handler handler = hdlr.get();
        if (handler == null) {
            return;
        }

        Status status = Status.of(StatusCode.fromProto(msg.getStatus()), Issue.fromPb(msg.getIssuesList()));
        if (!status.isSuccess()) {
            logger.trace("write stream[{}] get error {}", id, status);
            handler.onStop(status);
        }

        if (msg.hasInitResponse()) {
            YdbTopic.StreamWriteMessage.InitResponse resp = msg.getInitResponse();
            logger.trace("write stream[{}] get init response {}", id, resp);
            int[] codecs = new int[resp.getSupportedCodecs().getCodecsCount()];
            for (int idx = 0; idx < codecs.length; idx++) {
                codecs[idx] = resp.getSupportedCodecs().getCodecs(idx);
            }
            handler.onInit(resp.getLastSeqNo(), resp.getPartitionId(), resp.getSessionId(), codecs);
        }

        if (msg.hasUpdateTokenResponse()) {
            logger.trace("write stream[{}] get update token response", id, msg.getUpdateTokenResponse());
            // skip
        }

        if (msg.hasWriteResponse()) {
            YdbTopic.StreamWriteMessage.WriteResponse resp = msg.getWriteResponse();
            long partitionId = resp.getPartitionId();
            long lastSeqNo = 0;
            List<WriteAck> acks = new ArrayList<>(resp.getAcksCount());
            for (YdbTopic.StreamWriteMessage.WriteResponse.WriteAck ack: resp.getAcksList()) {
                lastSeqNo = Math.max(lastSeqNo, ack.getSeqNo());
                if (ack.hasWritten()) {
                    WriteAck.Details details = new WriteAck.Details(ack.getWritten().getOffset());
                    acks.add(new WriteAck(ack.getSeqNo(), WriteAck.State.WRITTEN, details));
                }
                if (ack.hasSkipped()) {
                    switch (ack.getSkipped().getReason()) {
                        case REASON_ALREADY_WRITTEN:
                            acks.add(new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null));
                            break;
                        case REASON_UNSPECIFIED: // TODO: add new statuses
                        default:
                            acks.add(new WriteAck(ack.getSeqNo(), WriteAck.State.ALREADY_WRITTEN, null));
                            break;
                    }
                }
            }

            logger.trace("write stream[{}] get write response with partition id {} and last seq no {}", id,
                    partitionId, lastSeqNo);
            handler.onResponse(partitionId, acks);
        }
    }

    private static class DefaultRetryPolicy extends ExponentialBackoffRetry {
        private static final int EXP_BACKOFF_BASE_MS = 256;
        private static final int EXP_BACKOFF_MAX_POWER = 7;

        DefaultRetryPolicy() {
            super(EXP_BACKOFF_BASE_MS, EXP_BACKOFF_MAX_POWER);
        }

        @Override
        public long nextRetryMs(int retryCount, long elapsedTimeMs) {
            return backoffTimeMillis(retryCount);
        }
    }
}
