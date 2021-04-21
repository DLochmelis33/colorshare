package ru.hse.colorshare.coding.suppliers;

import ru.hse.colorshare.coding.ColorDataFrame;
import ru.hse.colorshare.coding.DataFrameSupplier;

public abstract class AbstractDataFrameSupplier implements DataFrameSupplier {
    protected ColorDataFrame previous;
    protected int currentFrameIndex;

    private enum State {
        GENERATING,
        WAITING,
        PREVIOUS,
        DONE
    }

    private State state = State.GENERATING;



    private ColorDataFrame previousAndChangeState() {
        assert state == State.GENERATING || state == State.PREVIOUS;
        state = State.WAITING;
        return previous;
    }

    protected abstract void processFurther();
    protected abstract boolean hasMore();

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
                    currentFrameIndex++;
                } else {
                    state = State.PREVIOUS;
                }
        }
    }
}
