package ru.hse.colorshare.coding.decoding.impl;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;

public class SimpleByteDataFrame implements ByteDataFrame {
    private final byte[] bytes;
    private final long checksum;

    public SimpleByteDataFrame(byte[] bytes, long checksum) {
        this.bytes = bytes;
        this.checksum = checksum;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }
}
