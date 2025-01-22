package tech.ydb.topic.api;

import tech.ydb.topic.impl.BlockingQueuePolicy;
import tech.ydb.topic.impl.NonBlockingQueuePolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QueuePolicy {
    void acquire(int messageSize);
    void release(int messageSize);

    static QueuePolicy defaultPolicy() {
        return nonBlocking(20 * 1024 * 1024 /* 20 MB */, 100000);
    }

    static QueuePolicy blocking(int maxMemoryBytesUsage, int maxInFlyCount) {
        return new BlockingQueuePolicy(maxInFlyCount, maxMemoryBytesUsage);
    }

    static QueuePolicy nonBlocking(int maxMemoryBytesUsage, int maxInFlyCount) {
        return new NonBlockingQueuePolicy(maxInFlyCount, maxMemoryBytesUsage);
    }
}
