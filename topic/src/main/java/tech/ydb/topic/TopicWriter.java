package tech.ydb.topic;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import tech.ydb.core.Status;
import tech.ydb.topic.write.Compressor;
import tech.ydb.topic.write.Metadata;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.QueuePolicy;
import tech.ydb.topic.write.WriteAck;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <T> type of message
 */
public interface TopicWriter<T> extends AutoCloseable {

    /**
     * Запись сообщения в топик. При вызове этого метода сообщение добавляется в очередь записи и будет записано при
     * первой возможности.
     *
     * @param message сообщение
     * @param metadata метаданные сообщения
     * @param ackListener опциональный получатель WriteAck после подтверждения записи
     *
     * @throws IllegalStateException если писатель был остановлен
     * @throws IllegalArgumentException если message или metadata равны null
     * @throws QueueOverflowException если используется неблокирующий {@link QueuePolicy} и очередь записи переполнена
     */
    void write(@Nonnull T message, @Nonnull Metadata metadata, @Nullable Consumer<WriteAck> ackListener);

    /**
     * Блокировка писателя до момента полной записи всех сообщений из очереди сообщений
     */
    void flush();

    /**
     * Остановка работы писателя.Метод неблокирущий и возвращает CompletableFuture с финальным статусом завершения
     * писателя. Параметр {@code interrupt} указывает писателю - нужно ли ему дожидаться успешной записи всех сообщений
     * или нет. В случае указания interrupt = true все получатели WriteAck получат соответствующее сообщение
     * ReaderStopped, это сообщение ни гарантирует ни того что сообщение было записано ни того что оно не было записано
     *
     * @param interrupt признак немедленной остановки писателя
     * @return CompletableFuture со статусом завершения писателя
     *
     */
    CompletableFuture<Status> stop(boolean interrupt);

    /**
     * Блокирующая остановка писателя для поддержки интерфейса {@link AutoCloseable}. Включает в себя остановку
     * писателя, ожидание успешной записи всех сообщений и проверку успешности завершения стрима записи
     */
    @Override
    default void close() {
        stop(false).join().expectSuccess("Topic writer was closed with error");
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

    interface Builder {
        <T> TopicWriter<T> addServerMetadata(byte[] key, byte[] value);

        <T> TopicWriter<T> withAckExecutor(Executor executor);
        <T> TopicWriter<T> withCompressor(Compressor compressor);
        <T> TopicWriter<T> withQueuePolicy(QueuePolicy policy);

        <T> TopicWriter<T> build(Function<T, byte[]> serializator);

        default TopicWriter<byte[]> toBytesWriter() {
            return build(Function.identity());
        }

        default TopicWriter<String> toStringWriter() {
            return build(s -> s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
