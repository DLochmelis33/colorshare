package ru.hse.colorshare.coding.decoding;

public interface DecodingController {
    void setBulkChecksums(long[] checksums);

    boolean isBulkEncoded();

    void testFrame(int[] colors);

    void setReceivingParameters(/* пока не знаю, но что-то сюда надо */);
}
