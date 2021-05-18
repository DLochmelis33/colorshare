package ru.hse.colorshare.generator.preprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FileEncodingPreprocessor implements EncodingPreprocessor {
    private static final int MAX_BUFFER_SIZE = 4096;
    private final InputStream stream;
    private final long fileLength;
    private int currentOffset;

    private final byte[] innerBuffer = new byte[MAX_BUFFER_SIZE];

    public FileEncodingPreprocessor(InputStream stream, long fileLength) {
        this.stream = stream;
        this.fileLength = fileLength;
    }

    @Override
    public int readBytes(ByteBuffer buffer, int length) throws IOException {
        length = Math.min(length, buffer.remaining());
        int read = stream.read(innerBuffer, 0, length);
        currentOffset += read;
        buffer.put(innerBuffer, 0, read);
        return read;
    }

    @Override
    public long left() throws IOException {
        return fileLength - currentOffset;
    }
}
