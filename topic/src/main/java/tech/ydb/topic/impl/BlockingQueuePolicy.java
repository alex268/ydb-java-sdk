package tech.ydb.topic.impl;

import java.util.concurrent.Semaphore;

import tech.ydb.topic.api.QueuePolicy;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BlockingQueuePolicy implements QueuePolicy {
    private final Semaphore count;
    private final Semaphore size;
    private final int maxMessagesSize;

    public BlockingQueuePolicy(int maxQuerySize, int maxMessagesSize) {
        this.count = new Semaphore(maxQuerySize);
        this.size = new Semaphore(maxMessagesSize);
        this.maxMessagesSize = maxMessagesSize;
    }

    @Override
    public void acquire(int messageSize) {
        if (messageSize > maxMessagesSize) {
            throw new IllegalArgumentException(
                    "Message size " + messageSize + " is more than max size of queue " + maxMessagesSize
            );
        }

        try {
            size.acquire(messageSize);
            count.acquire(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", ex);
        }
    }

    @Override
    public void release(int messageSize) {
        count.release(1);
        size.release(messageSize);
    }
}
