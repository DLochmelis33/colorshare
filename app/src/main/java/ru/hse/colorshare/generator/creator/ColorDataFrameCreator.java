package ru.hse.colorshare.generator.creator;

public interface ColorDataFrameCreator {

    default CreationResult create(byte[] buffer) {
        return create(buffer, 0, buffer.length);
    }

    CreationResult create(byte[] buffer, int offset, int length);

    int estimateBufferSize();
}
