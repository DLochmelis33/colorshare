package ru.hse.colorshare.coding.decoding;

import java.io.IOException;

public interface DecodingPreprocessor {
    default void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, 0, bytes.length);
    }

    void writeBytes(byte[] bytes, int offset, int length) throws IOException;
}
