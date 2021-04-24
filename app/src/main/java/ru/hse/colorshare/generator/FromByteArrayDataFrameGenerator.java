package ru.hse.colorshare.generator;


import java.util.zip.Checksum;

import ru.hse.colorshare.frames.TwoBitsColorDataFrame;

import static java.lang.Math.min;

public final class FromByteArrayDataFrameGenerator extends AbstractDataFrameGenerator implements DataFrameGenerator {

    private final byte[] source;
    private int offset = 0;

    private final byte[] frame;
    private final Checksum checksum;

    public FromByteArrayDataFrameGenerator(byte[] source, Checksum checksum, int bytesPerFrame) {
        this.source = source;
        this.frame = new byte[bytesPerFrame];
        this.checksum = checksum;
    }

    private int readFrame() {
        int copied = min(frame.length, source.length - offset);
        System.arraycopy(source, offset, frame, 0, copied);
        offset += copied;
        return copied;
    }

    @Override
    protected void processFurther() {
        assert  offset < source.length;
        int read = readFrame();
        checksum.update(frame, 0, read);
        previous = new TwoBitsColorDataFrame(frame, 0, read, checksum.getValue());
        checksum.reset();
    }

    @Override
    protected boolean hasMore() {
        return offset < source.length;
    }


    @Override
    public long estimateSize() {
        return (source.length - offset) / frame.length;
    }

    @Override
    public int getFrameIndex() {
        return currentFrameIndex;
    }

    @Override
    public String getInfo() {
        return  "currentFrameIndex = " + currentFrameIndex;
    }


}
