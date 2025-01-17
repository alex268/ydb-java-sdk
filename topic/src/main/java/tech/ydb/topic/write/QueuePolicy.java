package tech.ydb.topic.write;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueuePolicy {
    static int MAX_MEMORY_USAGE_BYTES_DEFAULT = 20 * 1024 * 1024; // 20 MB
    static int MAX_IN_FLIGHT_COUNT_DEFAULT = 100000;

    void acquire(int messageSize);
    void release(int messageSize);

    static QueuePolicy defaultPolicy() {
        return new BlockingQueuePolicy(MAX_MEMORY_USAGE_BYTES_DEFAULT, MAX_IN_FLIGHT_COUNT_DEFAULT);
    }

    static QueuePolicy blockingPolicy(int maxMemoryBytesUsage, int maxInFlyCount) {
        return new BlockingQueuePolicy(maxInFlyCount, maxMemoryBytesUsage);
    }

    static QueuePolicy nonBlockingPolicy(int maxMemoryBytesUsage, int maxInFlyCount) {
        return new NonBlockingQueuePolicy(maxInFlyCount, maxMemoryBytesUsage);
    }
}
