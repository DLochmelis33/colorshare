package ru.hse.colorshare.coding;


import java.util.zip.Checksum;

import static java.lang.Math.min;

public final class FromByteArrayDataFrameSupplier implements DataFrameSupplier {

    private ColorDataFrame previous;

    private enum State {
        GENERATING,
        WAITING,
        PREVIOUS,
        DONE
    }

    private State state;
    private int currentFrameIndex = 0;

    private final byte[] source;
    private int offset = 0;

    private final byte[] frame;
    private final Checksum checksum;

    public FromByteArrayDataFrameSupplier(byte[] source, Checksum checksum, int bytesPerFrame) {
        this.source = source;
        this.frame = new byte[bytesPerFrame];
        this.checksum = checksum;
        this.state = State.GENERATING;
    }

    private int readFrame() {
        System.arraycopy(source, offset, frame, 0, min(frame.length, source.length - offset));
        int copied = min(frame.length, source.length - offset);
        offset += copied;
        return copied;
    }

    private void processFurther() {
        assert state == State.GENERATING && offset < source.length;
        int read = readFrame();
        checksum.update(frame, 0, read);
        previous = new TwoBitsColorDataFrame(frame, 0, read, checksum.getValue());
        checksum.reset();
    }

    private ColorDataFrame previousAndChangeState() {
        assert state == State.GENERATING || state == State.PREVIOUS;
        state = State.WAITING;
        return previous;
    }

    @Override
    public void setSuccess(boolean success) {
        switch (state) {
            case PREVIOUS:
            case GENERATING:
                throw new IllegalStateException("You have to call get before");
            case DONE:
                throw new IllegalStateException("Supplier has already done");
            case WAITING:
                if (success && offset >= source.length) {
                    state = State.DONE;
                } else if (success) {
                    state = State.GENERATING;
                    currentFrameIndex++;
                } else {
                    state = State.PREVIOUS;
                }
        }
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
        return "state = " + state + ", currentFrameIndex = " + currentFrameIndex;
    }

    @Override
    public ColorDataFrame get() {
        switch (state) {
            case WAITING:
                throw new IllegalStateException("You have to call setSuccess before");
            case DONE:
                return null;
            case PREVIOUS:
                return previousAndChangeState();
            case GENERATING:
                processFurther();
                return previousAndChangeState();
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }
}
