package ru.hse.colorshare.receiver;

public interface BulkDecoder {
    void setBulkChecksums(long[] checksums);

    boolean isBulkEncoded();

    void testFrame(int[] colors);

    void setReceivingParameters(/* пока не знаю, но что-то сюда надо */);
}
