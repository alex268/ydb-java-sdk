package tech.ydb.topic.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.core.Status;
import tech.ydb.topic.write.Metadata;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <T> type of message
 */
public interface TypedWriter<T> extends AutoCloseable {

    /**
     * Add message with metadata to the write queue. Message will be written to the topic as soon as possible. Third
     * argument is optional listener of write result.
     *
     * @param message message content
     * @param metadata custom metadata for message
     * @param ackListener optional listener of write confirmation
     *
     * @throws IllegalStateException if the writer was stopped
     * @throws IllegalArgumentException if message or metadata is null
     * @throws QueueOverflowException can be thrown by some {@link QueuePolicy}, like
     * {@link QueuePolicy#nonBlocking(int, int)}
     */
    void write(@Nonnull T message, @Nonnull Metadata metadata, @Nullable Consumer<WriteAck> ackListener);

    /**
     * Blocks until all messages have been successful written.
     */
    void flush();

    /**
     * Initiates an orderly shutdown of the writer. Returns {@code CompletableFuture} with shutdown status.
     * Option {@code flushBefore} enables additional call of {@link TypedWriter#flush() } before complete stopping.
     * If {@code flushBefore = false} all WriteAck listeners will receive the corresponding status
     * ReaderStopped, this status doesn't guarantee that the message was recorded nor the message was not recorded
     *
     * @param flushBefore признак немедленной остановки писателя
     * @return CompletableFuture with shutdown status
     *
     */
    CompletableFuture<Status> stop(boolean flushBefore);

    /**
     * Default implementation for {@link AutoCloseable} interface support. Includes call of
     * {@link TypedWriter#stop(boolean)} with awaiting for all messages have been written and validate of finish writer
     * status
     */
    @Override
    default void close() {
        stop(true).join().expectSuccess("Topic writer was closed with error");
    }

    default void write(@Nonnull T message, @Nonnull Metadata metadata) {
        write(message, metadata, null);
    }

    default void write(@Nonnull T message) {
        write(message, Metadata.empty(), null);
    }

    default CompletableFuture<WriteAck> writeWithAckFuture(@Nonnull T message, @Nonnull Metadata metadata) {
        CompletableFuture<WriteAck> future = new CompletableFuture<>();
        write(message, metadata, future::complete);
        return future;
    }

    default CompletableFuture<WriteAck> writeWithAckFuture(@Nonnull T message) {
        return writeWithAckFuture(message, Metadata.empty());
    }
}
