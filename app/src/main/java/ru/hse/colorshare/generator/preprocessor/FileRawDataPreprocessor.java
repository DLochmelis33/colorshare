package ru.hse.colorshare.generator.preprocessor;

import java.io.IOException;
import java.io.InputStream;

public class FileRawDataPreprocessor extends AbstractRawDataPreprocessor {
    private final InputStream stream;
    private final long fileLength;

    private long current = 0;

    private final byte[] buffer;
    private int bufferOffset = 0;
    private int bufferLength = 0;

    public FileRawDataPreprocessor(InputStream stream, long fileLength, int maxBufferSize) {
        this.stream = stream;
        this.fileLength = fileLength;
        this.buffer = new byte[maxBufferSize];
    }

    @Override
    public long left() throws IOException {
        return fileLength - current;
    }

    @Override
    public int getBytesImpl(byte[] bytes, int offset, int length) throws IOException {
        assert buffer.length >= length;

        int read = length - bufferOffset;
        if (read > 0) {
            length = bufferOffset + stream.read(buffer, bufferOffset, read);
        }

        System.arraycopy(buffer, 0, bytes, offset, length);

        current += length;
        bufferLength = length;

        return length;
    }

    @Override
    public void returnBytesImpl(int count) throws IOException {
        System.arraycopy(buffer, bufferLength - count, buffer, 0, count);
        current -= count;
        bufferOffset = count;
    }
}
