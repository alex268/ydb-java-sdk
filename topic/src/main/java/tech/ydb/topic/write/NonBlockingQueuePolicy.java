package tech.ydb.topic.write;

import java.util.concurrent.Semaphore;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class NonBlockingQueuePolicy implements QueuePolicy {
    private final Semaphore count;
    private final Semaphore size;
    private final int maxQuerySize;
    private final int maxMessagesSize;

    public NonBlockingQueuePolicy(int maxQuerySize, int maxMessagesSize) {
        this.count = new Semaphore(maxQuerySize);
        this.size = new Semaphore(maxMessagesSize);
        this.maxQuerySize = maxQuerySize;
        this.maxMessagesSize = maxMessagesSize;
    }

    @Override
    public void acquire(int messageSize) {
        if (messageSize > maxMessagesSize) {
            throw new IllegalArgumentException(
                    "Message size " + messageSize + " is more than max size of queue " + maxMessagesSize
            );
        }

        if (!size.tryAcquire(messageSize)) {
            int available = size.availablePermits();
            String errorMessage = "Rejecting a message of " + messageSize +
                    " bytes: not enough space in message queue. Buffer currently has " +
                    (maxMessagesSize - available) + " messages with " + available + " / " + maxMessagesSize +
                    " bytes available";
            throw new QueueOverflowException(errorMessage);
        }

        if (!count.tryAcquire(1)) {
            size.release(messageSize);
            throw new QueueOverflowException("Message queue in-flight limit of " + maxQuerySize + " reached");
        }
    }

    @Override
    public void release(int messageSize) {
        count.release(1);
        size.release(messageSize);
    }
}
