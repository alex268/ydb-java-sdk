package tech.ydb.topic.settings;

import java.util.function.BiConsumer;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.GrpcStreamRetrier;

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
    private final Codec codec;
    private final RetryConfig retryConfig;
    private final long maxSendBufferMemorySize;
    private final int maxSendBufferMessagesCount;

    private WriterSettings(Builder builder) {
        this.topicPath = builder.topicPath;
        this.producerId = builder.producerId;
        this.messageGroupId = builder.messageGroupId;
        this.partitionId = builder.partitionId;
        this.codec = builder.codec;
        this.retryConfig = builder.retryConfig;
        this.maxSendBufferMemorySize = builder.maxSendBufferMemorySize;
        this.maxSendBufferMessagesCount = builder.maxSendBufferMessagesCount;
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

    @Deprecated
    public BiConsumer<Status, Throwable> getErrorsHandler() {
        return null;
    }

    public Long getPartitionId() {
        return partitionId;
    }

    public Codec getCodec() {
        return codec;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
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
        private Codec codec = Codec.GZIP;
        private RetryConfig retryConfig = GrpcStreamRetrier.RETRY_ALL;
        private long maxSendBufferMemorySize = MAX_MEMORY_USAGE_BYTES_DEFAULT;
        private int maxSendBufferMessagesCount = MAX_IN_FLIGHT_COUNT_DEFAULT;

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
        public Builder setCodec(Codec codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Set {@link RetryConfig} to define behavior of the stream internal retries
         * @param config retry mode
         * @return settings builder
         */
        public Builder setRetryConfig(RetryConfig config) {
            this.retryConfig = config;
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

        /**
         * @param handler
         * @return builder
         * @deprecated use {@link Builder#setRetryConfig(tech.ydb.common.retry.RetryConfig)} instead
         */
        @Deprecated
        public Builder setErrorsHandler(BiConsumer<Status, Throwable> handler) {
            final RetryConfig currentConfig = retryConfig;
            retryConfig = new RetryConfig() {
                @Override
                public RetryPolicy getStatusRetryPolicy(Status status) {
                    handler.accept(status, null);
                    return currentConfig.getStatusRetryPolicy(status);
                }

                @Override
                public RetryPolicy getThrowableRetryPolicy(Throwable th) {
                    handler.accept(null, th);
                    return currentConfig.getThrowableRetryPolicy(th);
                }
            };
            return this;
        }

        public WriterSettings build() {
            return new WriterSettings(this);
        }
    }
}
