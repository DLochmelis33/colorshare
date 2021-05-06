package ru.hse.colorshare.generator.preprocessor;

import java.io.IOException;

public abstract class AbstractRawDataPreprocessor implements RawDataPreprocessor {
    private boolean returned = false;

    protected abstract int getBytesImpl(byte[] buffer, int offset, int length) throws IOException;
    protected abstract void returnBytesImpl(int count) throws IOException;

    @Override
    public int getBytes(byte[] buffer, int offset, int length) throws IOException {
        returned = false;
        return getBytesImpl(buffer, offset, length);
    }

    @Override
    public void returnBytes(int count) throws IOException {
        if (returned)
            throw new IllegalStateException("Cannot return twice");
        returnBytesImpl(count);
    }
}
