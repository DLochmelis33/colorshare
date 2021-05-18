package ru.hse.colorshare.coding.decoding.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;
import ru.hse.colorshare.coding.decoding.ColorDataFrameDecoder;
import ru.hse.colorshare.coding.decoding.DecodingController;
import ru.hse.colorshare.coding.decoding.DecodingPreprocessor;
import ru.hse.colorshare.coding.exceptions.DecodingException;

public class SimpleDecodingController implements DecodingController {

    private final Map<Long, Integer> checksumToIndex = new HashMap<>();
    private int totalReceivedFrames = 0;
    private byte[][] receivedBytes;

    private final OutputStream outputStream;

    private ColorDataFrameDecoder decoder;
    private DecodingPreprocessor preprocessor;

    public SimpleDecodingController(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.decoder = new FourColorsDataFrameDecoder(new CRC32());
        this.preprocessor = new SimpleDecodingPreprocessor(outputStream);
    }

    @Override
    public void startNewBulkEncoding(long[] checksums) {
        checksumToIndex.clear();
        totalReceivedFrames = 0;
        receivedBytes = new byte[checksums.length][];

        for (int i = 0; i < checksums.length; i++) {
            checksumToIndex.put(checksums[i], i);
        }
    }

    @Override
    public boolean isBulkFullyEncoded() {
        return checksumToIndex.size() <= totalReceivedFrames;
    }

    @Override
    public void testFrame(int[] colors) throws DecodingException {
        ByteDataFrame dataFrame = decoder.decode(colors);
        Integer assumedIndex = checksumToIndex.get(dataFrame.getChecksum());
        if (assumedIndex != null && receivedBytes[assumedIndex] != null) {
            totalReceivedFrames += 1;
            receivedBytes[assumedIndex] = dataFrame.getBytes();
        }
        if (isBulkFullyEncoded()) {
            flushBytes();
        }
    }

    private void flushBytes() throws DecodingException {
        try {
            for (byte[] bytes : receivedBytes) {
                preprocessor.writeBytes(bytes);
            }
            outputStream.flush();
        } catch (IOException e) {
            throw new DecodingException(e);
        }
    }

    @Override
    public void setReceivingParameters() {

    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
