package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface Compressor {
    int getCodec();

    CompletableFuture<byte[]> compress(byte[] data);
}
