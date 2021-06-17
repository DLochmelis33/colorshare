package ru.hse.colorshare.coding.decoding;

public interface ColorDataFrameDecoder {
    ByteDataFrame decode(int[] colors);

    int estimateBufferSize(int colorsCount);
}
