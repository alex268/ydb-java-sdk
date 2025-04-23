package tech.ydb.topic.settings;

import java.util.function.BiConsumer;

import tech.ydb.core.Status;
import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class WriterSettings {
    private static final long MAX_MEMORY_USAGE_BYTES_DEFAULT = 20 * 1024 * 1024; // 20 MB
    private static final int MAX_IN_FLIGHT_COUNT_DEFAULT = 100000;

    private final String topicPath;
    private final String producerId;
    private final String messageGroupId;
    private final Long partitionId;
    private final int codec;
    private final long maxSendBufferMemorySize;
    private final int maxSendBufferMessagesCount;
    private final BiConsumer<Status, Throwable> errorsHandler;

    private WriterSettings(Builder builder) {
        this.topicPath = builder.topicPath;
        this.producerId = builder.producerId;
        this.messageGroupId = builder.messageGroupId;
        this.partitionId = builder.partitionId;
        this.codec = builder.codec;
        this.maxSendBufferMemorySize = builder.maxSendBufferMemorySize;
        this.maxSendBufferMessagesCount = builder.maxSendBufferMessagesCount;
        this.errorsHandler = builder.errorsHandler;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getTopicPath() {
        return topicPath;
    }

    public String getProducerId() {
        return producerId;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }

    public BiConsumer<Status, Throwable> getErrorsHandler() {
        return errorsHandler;
    }

    public Long getPartitionId() {
        return partitionId;
    }

    public int getCodec() {
        return codec;
    }

    public long getMaxSendBufferMemorySize() {
        return maxSendBufferMemorySize;
    }

    public int getMaxSendBufferMessagesCount() {
        return maxSendBufferMessagesCount;
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private String topicPath = null;
        private String producerId = null;
        private String messageGroupId = null;
        private Long partitionId = null;
        private int codec = Codec.GZIP;
        private long maxSendBufferMemorySize = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private int maxSendBufferMessagesCount = MAX_IN_FLIGHT_COUNT_DEFAULT;
        private BiConsumer<Status, Throwable> errorsHandler = null;

        /**
         * Set path to a topic to write to
         * @param topicPath  path to a topic
         * @return settings builder
         */
        public Builder setTopicPath(String topicPath) {
            this.topicPath = topicPath;
            return this;
        }

        /**
         * Set producer ID (aka SourceId) to use
         * ProducerId and MessageGroupId should be equal (temp requirement)
         * @param producerId  producer ID
         * @return settings builder
         */
        public Builder setProducerId(String producerId) {
            this.producerId = producerId;
            return this;
        }

        /**
         * Set MessageGroup ID to use
         * Producer ID and MessageGroup ID should be equal (temp requirement)
         * @param messageGroupId  MessageGroup ID
         * @return settings builder
         */
        public Builder setMessageGroupId(String messageGroupId) {
            this.messageGroupId = messageGroupId;
            return this;
        }

        /**
         * Set partition ID.
         * Write to an exact partition. Generally server assigns partition automatically by message_group_id.
         * Using this option is not recommended unless you know for sure why you need it.
         * @param partitionId  partition ID
         * @return settings builder
         */
        public Builder setPartitionId(long partitionId) {
            this.partitionId = partitionId;
            return this;
        }

        /**
         * Set codec to use for data compression prior to write
         * @param codec  compression codec
         * @return settings builder
         */
        public Builder setCodec(int codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Set memory usage limit for send buffer.
         * Writer will not accept new messages if memory usage exceeds this limit.
         * Memory usage consists of raw data pending compression and compressed messages being sent.
         * @param maxMemoryUsageBytes  max memory usage in bytes
         * @return settings builder
         */
        public Builder setMaxSendBufferMemorySize(long maxMemoryUsageBytes) {
            this.maxSendBufferMemorySize = maxMemoryUsageBytes;
            return this;
        }

        /**
         * Set maximum messages accepted by writer but not written (with confirmation from server).
         * Writer will not accept new messages after reaching the limit.
         * @param maxMessagesCount  max message in-flight count
         * @return settings builder
         */
        public Builder setMaxSendBufferMessagesCount(int maxMessagesCount) {
            this.maxSendBufferMessagesCount = maxMessagesCount;
            return this;
        }

        public Builder setErrorsHandler(BiConsumer<Status, Throwable> handler) {
            this.errorsHandler = handler;
            return this;
        }

        public WriterSettings build() {
            return new WriterSettings(this);
        }
    }
}
