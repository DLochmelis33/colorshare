package ru.hse.colorshare.coding.encoding;

import java.nio.ByteBuffer;

public interface DataFrameEncoder {

    ColorDataFrame encode(ByteBuffer buffer);

    int estimateBufferSize();
}
