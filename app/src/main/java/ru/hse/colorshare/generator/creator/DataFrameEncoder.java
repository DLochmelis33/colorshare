package ru.hse.colorshare.generator.creator;

import java.nio.ByteBuffer;

public interface DataFrameEncoder {

    ColorDataFrame encode(ByteBuffer buffer);

    int estimateBufferSize();
}
