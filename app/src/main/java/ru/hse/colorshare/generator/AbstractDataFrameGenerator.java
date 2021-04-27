package ru.hse.colorshare.generator;

import ru.hse.colorshare.frames.BulkColorDataFrames;

public abstract class AbstractDataFrameGenerator implements DataFrameGenerator {
    protected BulkColorDataFrames previous;
    protected int currentBulkIndex;

    private enum State {
        GENERATING,
        WAITING,
        PREVIOUS,
        DONE
    }

    private State state = State.GENERATING;

    private BulkColorDataFrames previousAndChangeState() {
        assert state == State.GENERATING || state == State.PREVIOUS;
        state = State.WAITING;
        return previous;
    }

    protected abstract void processFurther();
    protected abstract boolean hasMore();

    @Override
    public BulkColorDataFrames getNextBulk() {
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

    @Override
    public void setSuccess(boolean success) {
        switch (state) {
            case PREVIOUS:
            case GENERATING:
                throw new IllegalStateException("You have to call get before");
            case DONE:
                throw new IllegalStateException("Supplier has already done");
            case WAITING:
                if (success && !hasMore()) {
                    state = State.DONE;
                } else if (success) {
                    state  = State.GENERATING;
                    currentBulkIndex++;
                } else {
                    state = State.PREVIOUS;
                }
        }
    }
}
