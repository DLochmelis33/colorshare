package ru.hse.colorshare.coding;


import java.util.zip.Checksum;

import ru.hse.colorshare.coding.dto.DataFrame;

/*
    This class is a controller of encoding process. It takes as an input
    @param InputStream stream. Source of data to encode and transmit
    @param Encoder encode. Encoder of a data. It might be correcting code or something else.
    @param Function<BitArray, ? extends DataFrame> toDataFrame. Mapper to DataFrame

 */

public final class ColorFrameSupplier implements DataFrameSupplier {

    private DataFrame previous;

    private enum State {
        READY_TO_GENERATE,
        WAITING_FOR_RESULT
    }

    private State state;

    private final byte[] source;
    int offset = 0;

    private final byte[] frame;
    private final Checksum checksum;
    private final DataFrame.Creator creator;

    public ColorFrameSupplier(byte[] source, Checksum checksum, DataFrame.Creator creator, int bytesPerFrame) {
        this.source = source;
        this.frame = new byte[bytesPerFrame];
        this.checksum = checksum;
        this.creator = creator;
    }

    private void readFrame() {
        System.arraycopy(source, offset, frame, 0, frame.length);
    }

    private void processFurther() {
        assert state == State.READY_TO_GENERATE;
        readFrame();
        checksum.update(frame, 0, frame.length);
        previous = creator.ofBytes(frame, checksum.getValue());
        checksum.reset();
    }

    private DataFrame returnAndChangeState() {
        assert previous != null;
        state = State.WAITING_FOR_RESULT;
        return previous;
    }

    @Override
    public void setSuccess(boolean success) {
        if (state != State.WAITING_FOR_RESULT) {
            throw new IllegalStateException("You have to call get before");
        }
        if (success) {
            previous = null;
        }
        state = State.READY_TO_GENERATE;
    }

    @Override
    public long estimateSize() {
        return (source.length - offset) / frame.length;
    }

    @Override
    public DataFrame get() {
        if (state != State.READY_TO_GENERATE) {
            throw new IllegalStateException("You have to call setSuccess before");
        }
        state = State.WAITING_FOR_RESULT;
        if (previous != null) {
            return returnAndChangeState();
        }
        processFurther();
        return returnAndChangeState();
    }
}
