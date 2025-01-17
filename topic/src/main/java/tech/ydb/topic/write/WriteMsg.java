package tech.ydb.topic.write;


import java.util.stream.Collectors;

import com.google.protobuf.ByteString;

import tech.ydb.core.utils.ProtobufUtils;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.WriteRequest.MessageData;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteMsg {
    private final MessageData proto;

    private WriteMsg(long seqNo, ByteString data, Metadata metadata, long uncompressedSize) {
        proto = MessageData.newBuilder()
                .setSeqNo(seqNo)
                .setData(data)
                .setUncompressedSize(uncompressedSize)
                .setCreatedAt(ProtobufUtils.instantToProto(metadata.getCreatedAt()))
                .addAllMetadataItems(
                        metadata.getItems().stream()
                                .map(item -> YdbTopic.MetadataItem.newBuilder()
                                .setKey(item.getKey())
                                .setValue(ByteString.copyFrom(item.getValue()))
                                .build())
                                .collect(Collectors.toList())
                ).build();
    }

    public YdbTopic.StreamWriteMessage.WriteRequest.MessageData toProto() {
        return proto;
    }

    public static WriteMsg uncompressed(long seqNo, byte[] data, Metadata metadata) {
        return new WriteMsg(seqNo, ByteString.copyFrom(data), metadata, data.length);
    }
}
