package ru.hse.colorshare.coding.decoding;

import java.io.IOException;

public interface DecodingPostprocessor {
    default void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, 0, bytes.length);
    }

    default void writeBytes(byte[][] bytes) throws IOException {
        for (byte[] arr : bytes) {
            writeBytes(bytes);
        }
    }

    void writeBytes(byte[] bytes, int offset, int length) throws IOException;
}
