package ru.hse.colorshare.coding.decoding.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;
import ru.hse.colorshare.coding.decoding.ColorDataFrameDecoder;
import ru.hse.colorshare.coding.decoding.DecodingController;
import ru.hse.colorshare.coding.decoding.DecodingPostprocessor;

public class SimpleDecodingController implements DecodingController {

    private final Map<Long, Integer> checksumToIndex = new HashMap<>();
    private final AtomicInteger receivedFramesCount = new AtomicInteger(0);
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private byte[][] receivedBytes;

    private final OutputStream outputStream;

    private ColorDataFrameDecoder decoder;
    private DecodingPostprocessor preprocessor;

    public SimpleDecodingController(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.decoder = new FourColorsDataFrameDecoder(new CRC32());
        this.preprocessor = new SimpleDecodingPostprocessor(outputStream);
    }

    @Override
    public void startNewBulkEncoding(long[] checksums) {
        checksumToIndex.clear();
        receivedFramesCount.set(0);
        flushed.set(false);

        receivedBytes = new byte[checksums.length][];
        for (int i = 0; i < checksums.length; i++) {
            checksumToIndex.put(checksums[i], i);
        }
    }

    @Override
    public boolean isBulkFullyEncoded() {
        return checksumToIndex.size() <= receivedFramesCount.get();
    }

    @Override
    public void testFrame(int[] colors) {
        ByteDataFrame dataFrame = decoder.decode(colors);
        Integer assumedIndex = checksumToIndex.get(dataFrame.getChecksum());
        if (assumedIndex != null && receivedBytes[assumedIndex] != null) {
            receivedFramesCount.incrementAndGet();
            receivedBytes[assumedIndex] = dataFrame.getBytes();
        }
    }

    @Override
    public void setReceivingParameters() {

    }

    @Override
    public void flush() throws IOException {
        if (!isBulkFullyEncoded()) {
            throw new IllegalStateException("You cannot call flush before bulk is fully encoded");
        }
        if (flushed.getAndSet(true))
            return;
        preprocessor.writeBytes(receivedBytes);
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
