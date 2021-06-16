package ru.hse.colorshare.coding.decoding.impl;

import java.io.IOException;
import java.io.OutputStream;

import ru.hse.colorshare.coding.decoding.DecodingPostprocessor;

public class SimpleDecodingPostprocessor implements DecodingPostprocessor {
    private final OutputStream outputStream;

    public SimpleDecodingPostprocessor(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        outputStream.write(bytes, offset, length);
    }
}
