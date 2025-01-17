package tech.ydb.topic.write;

import javax.annotation.Nonnull;

import tech.ydb.proto.topic.YdbTopic;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class InitRequest {

    private final YdbTopic.StreamWriteMessage.InitRequest msg;

    private InitRequest(YdbTopic.StreamWriteMessage.InitRequest msg) {
        this.msg = msg;
    }

    public InitRequest withMetadata(String key, String value) {
        msg.getWriteSessionMetaMap().put(key, value);
        return this;
    }

    public YdbTopic.StreamWriteMessage.InitRequest toProtoMsg() {
        return msg;
    }

    public static InitRequest withoutDeduplication(@Nonnull String topicPath) {
        return new InitRequest(
                YdbTopic.StreamWriteMessage.InitRequest.newBuilder()
                        .setPath(topicPath)
                        .setGetLastSeqNo(false)
                        .build()
        );
    }

    public static InitRequest withProducer(@Nonnull String topicPath, @Nonnull String producerId) {
        return new InitRequest(
                YdbTopic.StreamWriteMessage.InitRequest.newBuilder()
                        .setPath(topicPath)
                        .setProducerId(producerId)
                        .setMessageGroupId(producerId)
                        .setGetLastSeqNo(true)
                        .build()
        );
    }

    public static InitRequest withProducerAndPartitionId(@Nonnull String topicPath, @Nonnull String producerId,
            long partitionId) {
        return new InitRequest(
                YdbTopic.StreamWriteMessage.InitRequest.newBuilder()
                        .setPath(topicPath)
                        .setProducerId(producerId)
                        .setPartitionId(partitionId)
                        .setGetLastSeqNo(true)
                        .build()
        );
    }

    public static InitRequest withProducerAndPartitionGeneration(@Nonnull String topicPath, @Nonnull String producerId,
            long partitionId, long generation) {
        return new InitRequest(
                YdbTopic.StreamWriteMessage.InitRequest.newBuilder()
                        .setPath(topicPath)
                        .setProducerId(producerId)
                        .setPartitionWithGeneration(YdbTopic.PartitionWithGeneration.newBuilder()
                                .setPartitionId(partitionId)
                                .setGeneration(generation)
                                .build())
                        .setGetLastSeqNo(true)
                        .build()
        );
    }
}
