package tech.ydb.coordination.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 *
 * @author Aleksandr Gorshenin
 */
class Stream implements GrpcReadWriteStream.Observer<SessionResponse> {
    private static final int SHUTDOWN_TIMEOUT_MS = 1000;
    private static final Logger logger = LoggerFactory.getLogger(Stream.class);

    private final ScheduledExecutorService scheduler;
    private final GrpcReadWriteStream<SessionResponse, SessionRequest> stream;
    private final CompletableFuture<Status> stopFuture = new CompletableFuture<>();
    private final CompletableFuture<Result<Long>> startFuture = new CompletableFuture<>();

    private final Map<Long, StreamMsg<?>> messages = new ConcurrentHashMap<>();

    Stream(Rpc rpc) {
        this.scheduler = rpc.getScheduler();
        this.stream = rpc.createSession(GrpcRequestSettings.newBuilder().build());
    }

    public CompletableFuture<Status> startStream() {
        stream.start(this).whenComplete((status, th) -> {
            if (th != null) {
                startFuture.completeExceptionally(th);
                stopFuture.completeExceptionally(th);
            }
            if (status != null) {
                startFuture.complete(Result.fail(status.isSuccess() ? Status.of(StatusCode.BAD_REQUEST) : status));
                stopFuture.complete(status);
            }
        });

        return stopFuture;
    }

    public Collection<StreamMsg<?>> getMessages() {
        return messages.values();
    }

    public void cancelStream() {
        logger.trace("stream {} cancel stream", hashCode());
        stream.cancel();
    }

    public CompletableFuture<Result<Long>> sendSessionStart(long reqId, String node, Duration timeout, ByteString key) {
        SessionRequest startMsg = SessionRequest.newBuilder().setSessionStart(
                SessionRequest.SessionStart.newBuilder()
                        .setSessionId(reqId)
                        .setPath(node)
                        .setTimeoutMillis(timeout.toMillis())
                        .setProtectionKey(key)
                        .build()
        ).build();

        logger.trace("stream {} send session start msg {}", hashCode(), reqId);
        stream.sendNext(startMsg);
        return startFuture;
    }

    public CompletableFuture<Status> stop() {
        if (stopFuture.isDone()) {
            return stopFuture;
        }

        SessionRequest stopMsg = SessionRequest.newBuilder().setSessionStop(
                SessionRequest.SessionStop.newBuilder().build()
        ).build();


        logger.trace("stream {} send session stop msg", hashCode());
        stream.sendNext(stopMsg);

        // schedule cancellation of grpc-stream
        // if server doesn't close stream by stop message - this timer cancels grpc stream
        final Future<?> timer = scheduler.schedule(this::cancelStream, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        stopFuture.whenComplete((st, ex) -> {
            if (!timer.isDone()) {
                timer.cancel(true);
            }
        });

        return stopFuture;
    }

    public void sendMsg(long requestId, StreamMsg<?> msg) {
        StreamMsg<?> oldMsg = messages.put(requestId, msg);

        SessionRequest request = msg.makeRequest(requestId);
        logger.trace("stream {} send message {}", hashCode(), TextFormat.shortDebugString(request));
        stream.sendNext(request);

        if (oldMsg != null) {
            oldMsg.handleError(Status.of(StatusCode.CLIENT_CANCELLED));
        }
    }

    @Override
    public void onNext(SessionResponse resp) {
        if (resp.hasFailure()) {
            onFail(resp.getFailure());
            return;
        }

        if (resp.hasSessionStarted()) {
            onSessionStarted(resp.getSessionStarted());
            return;
        }

        if (resp.hasSessionStopped()) {
            onSessionStopped(resp.getSessionStopped());
            return;
        }

        if (resp.hasPing()) {
            onPing(resp.getPing());
            return;
        }

        if (resp.hasPong()) {
            // ignore, just logging
            long opaque = resp.getPong().getOpaque();
            logger.trace("stream {} got pong msg {}", hashCode(), Long.toUnsignedString(opaque));
            return;
        }

        if (resp.hasAcquireSemaphorePending()) {
            // ignore, just logging
            long reqId = resp.getAcquireSemaphorePending().getReqId();
            logger.trace("stream {} got acquire semaphore pending msg {}", hashCode(), reqId);
            return;
        }

        if (resp.hasCreateSemaphoreResult()) {
            onNextMessage(resp.getCreateSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDeleteSemaphoreResult()) {
            onNextMessage(resp.getDeleteSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasUpdateSemaphoreResult()) {
            onNextMessage(resp.getUpdateSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDescribeSemaphoreResult()) {
            onNextMessage(resp.getDescribeSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasAcquireSemaphoreResult()) {
            onNextMessage(resp.getAcquireSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasReleaseSemaphoreResult()) {
            onNextMessage(resp.getReleaseSemaphoreResult().getReqId(), resp);
        }

        if (resp.hasDescribeSemaphoreChanged()) {
            onNextMessage(resp.getDescribeSemaphoreChanged().getReqId(), resp);
        }
    }

    public void onNextMessage(long reqId, SessionResponse resp) {
        StreamMsg<?> msg = messages.remove(reqId);
        if (msg != null && msg.handleResponse(resp)) {
            logger.trace("stream {} got response {}", hashCode(), TextFormat.shortDebugString(resp));
            StreamMsg<?> nextMsg = msg.nextMsg();
            if (nextMsg != null) {
                StreamMsg<?> old = messages.put(reqId, nextMsg);
                if (old != null) {
                    old.handleError(Status.of(StatusCode.CLIENT_CANCELLED));
                }
            }
        } else {
            logger.warn("stream {} lost response {}", hashCode(), TextFormat.shortDebugString(resp));
        }
    }

    private void onFail(SessionResponse.Failure msg) {
        Status status = Status.of(StatusCode.fromProto(msg.getStatus()), Issue.fromPb(msg.getIssuesList()));
        logger.trace("stream {} got fail message {}", hashCode(), status);
        stopFuture.complete(status);
        startFuture.complete(Result.fail(status));
    }

    private void onSessionStarted(SessionResponse.SessionStarted msg) {
        long id = msg.getSessionId();
        if (startFuture.complete(Result.success(id))) {
            logger.trace("stream {} started with id {}", hashCode(), id);
        } else {
            logger.warn("stream {} lost the start message with id {}", hashCode(), id);
        }
    }

    private void onSessionStopped(SessionResponse.SessionStopped msg) {
        logger.trace("stream {} stopped with id {}", hashCode(), msg.getSessionId());
        stream.close();
    }

    private void onPing(SessionResponse.PingPong msg) {
        long opaque = msg.getOpaque();
        SessionRequest pong = SessionRequest.newBuilder().setPong(
                SessionRequest.PingPong.newBuilder().setOpaque(opaque).build()
        ).build();

        logger.trace("stream {} got ping msg {}, sending pong msg", hashCode(), Long.toUnsignedString(opaque));
        stream.sendNext(pong);
    }
}
