package ru.hse.colorshare.coding.decoding;

public interface ByteDataFrame {
    byte[] getBytes();

    long getChecksum();
}
