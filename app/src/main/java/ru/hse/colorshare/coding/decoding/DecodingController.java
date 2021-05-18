package ru.hse.colorshare.coding.decoding;

import java.io.Closeable;

import ru.hse.colorshare.coding.exceptions.DecodingException;

public interface DecodingController extends Closeable {
    void startNewBulkEncoding(long[] checksums);

    boolean isBulkFullyEncoded();

    void testFrame(int[] colors) throws DecodingException;

    void setReceivingParameters(/* пока не знаю, но что-то сюда надо */);
}
