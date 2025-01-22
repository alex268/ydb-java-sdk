package tech.ydb.topic;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.function.Function;

import tech.ydb.topic.api.QueuePolicy;
import tech.ydb.topic.api.RetryPolicy;
import tech.ydb.topic.api.TypedIdempotentWriter;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface TopicIdempotentWriter extends TypedIdempotentWriter<byte[]> {

    interface Builder {
        Builder addServerMetadata(byte[] key, byte[] value);

        Builder withAckExecutor(Executor executor);
        Builder withQueuePolicy(QueuePolicy policy);
        Builder withRetryPolicy(RetryPolicy policy);

        TopicIdempotentWriter build();

        <T> TypedIdempotentWriter<T> toTypedSeqWriter(Function<T, byte[]> mapper);

        default TypedIdempotentWriter<String> toStringSeqWriter() {
            return toTypedSeqWriter(s -> s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
