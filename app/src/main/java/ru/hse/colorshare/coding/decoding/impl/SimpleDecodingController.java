package ru.hse.colorshare.coding.decoding.impl;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.zip.CRC32;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;
import ru.hse.colorshare.coding.decoding.ColorDataFrameDecoder;
import ru.hse.colorshare.coding.decoding.DecodingController;
import ru.hse.colorshare.coding.decoding.DecodingPostprocessor;

public class SimpleDecodingController implements DecodingController {

    private static final String TAG = "SmplDecodingController";

    private final Map<Long, Integer> checksumToIndex = new HashMap<>();
    private final AtomicInteger receivedFramesCount = new AtomicInteger(0);
    private CountDownLatch receivedFramesLatch;
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private AtomicReferenceArray<byte[]> receivedBytes;

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

        receivedBytes = new AtomicReferenceArray<>(checksums.length);
        for (int i = 0; i < checksums.length; i++) {
            checksumToIndex.put(checksums[i], i);
        }
        receivedFramesLatch = new CountDownLatch(checksums.length);
    }

    @Override
    public boolean isBulkFullyEncoded() {
        return checksumToIndex.size() <= receivedFramesCount.get();
    }

    @Override
    public void awaitBulkFullyEncoded(long timeout, TimeUnit unit) throws InterruptedException {
        receivedFramesLatch.await(timeout, unit);
    }

    @Override
    public void testFrame(int[] colors) {
        ByteDataFrame dataFrame;
        try {
            dataFrame = decoder.decode(colors);
        } catch (NullPointerException e) {
            Log.w(TAG, "nullpointer");
            return;
        }
        Integer assumedIndex = checksumToIndex.get(dataFrame.getChecksum());
        if (assumedIndex == null) {
            Log.w(TAG, "unknown checksum: " + dataFrame.getChecksum());
        }
        if (assumedIndex != null && receivedBytes.get(assumedIndex) == null) {
            receivedBytes.compareAndSet(assumedIndex, null, dataFrame.getBytes());
            receivedFramesCount.incrementAndGet();
            receivedFramesLatch.countDown();
            Log.d(TAG, "decoded frame #" + assumedIndex);
        }
    }

    @Override
    public void setReceivingParameters() {

    }

    @Override
    public synchronized void flush() throws IOException {
        if (!isBulkFullyEncoded()) {
            throw new IllegalStateException("You cannot call flush before bulk is fully encoded");
        }
        if (flushed.getAndSet(true))
            return;
        for (int i = 0; i < receivedBytes.length(); i++) {
            preprocessor.writeBytes(receivedBytes.get(i));
        }
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
