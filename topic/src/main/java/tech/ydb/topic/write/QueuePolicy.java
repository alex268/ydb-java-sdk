package tech.ydb.topic.write;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueuePolicy {
    void accept(long bytesSize);
    void release(long bytesSize);
}
