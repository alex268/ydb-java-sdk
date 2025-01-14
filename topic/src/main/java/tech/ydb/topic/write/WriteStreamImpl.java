package tech.ydb.topic.write;

import java.util.List;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WriteStreamImpl implements WriteStream {

    @Override
    public void start(Handler handler) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(long codec, List<Message> messages) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
