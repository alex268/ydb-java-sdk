package tech.ydb.topic.write;

import java.util.List;

import tech.ydb.core.Status;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface WriteStream {
    interface Handler {
        InitRequest createInitRequest();

        void onInit(long lastSeqNo, long partitionId, byte[] sessionId, int[] codecs);

        void onResponse(long partitionId, List<WriteAck> acks);

        boolean onStop(Status status);
    }

    void start(Handler handler);

    void stop();

    void write(long codec, List<Message> messages);
}
