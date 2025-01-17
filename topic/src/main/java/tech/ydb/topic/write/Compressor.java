package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface Compressor {
    int getCodecCode();

    CompletableFuture<byte[]> compress(byte[] data);
}
