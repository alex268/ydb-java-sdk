package tech.ydb.topic.api;

import java.util.List;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.write.InitRequest;
import tech.ydb.topic.write.WriteAck;
import tech.ydb.topic.write.WriteMsg;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface WriteStream {
    int CODEC_RAW = 1;
    int CODEC_GZIP = 2;
    int CODEC_LZOP = 3;
    int CODEC_ZSTD = 4;

    int CODEC_CUSTOM = 10000;

    void start(Handler handler);

    void stop();

    void write(int codec, YdbTransaction tx, List<WriteMsg> messages);

    default void write(int codec, List<WriteMsg> messages) {
        write(codec, null, messages);
    }

    interface Handler {
        InitRequest createInitRequest();

        void onInit(long lastSeqNo, long partitionId, String sessionId, int[] codecs);

        void onResponse(long partitionId, List<WriteAck> acks);

        boolean onStop(Status status);
    }
}
