package ru.hse.colorshare.transmitter;

import android.graphics.PointF;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public final class TransmissionParams {
    public final int unitSize;
    public final int rows;
    public final int cols;
    public final PointF leftTopOfGrid;
    public final Rect unitRect;
    public final int framesPerSecond;

    public static final int EXPECTED_FRAME_SIZE_IN_UNITS = 200;
    public static final int BORDER_SIZE = 10;
    public static final int MIN_UNITS_IN_LINE = 2 * LocatorMarkGraphic.SIDE_SIZE_IN_UNITS; // locator marks must not overlap

    public TransmissionParams(int unitSize, int rows, int cols, PointF leftTopOfGrid, int framesPerSecond) throws IllegalStateException {
        this.unitSize = unitSize;
        this.rows = rows;
        this.cols = cols;
        this.leftTopOfGrid = leftTopOfGrid;
        unitRect = new Rect(0, 0, unitSize, unitSize);
        getColorFrameSize();
        this.framesPerSecond = framesPerSecond;
    }

    public int getColorFrameSize() throws IllegalStateException { // in color units
        int frameSize = rows * cols - 3 * LocatorMarkGraphic.SIZE_IN_UNITS;
        if (frameSize <= 0) {
            throw new IllegalStateException("number of color units in frame <= 0");
        }
        return frameSize;
    }

    public long getOneFrameDelayInNanos() {
        return TimeUnit.SECONDS.toNanos(1) / framesPerSecond;
    }

    @Override
    public @NonNull
    String toString() {
        return "unit size = " + unitSize + "; rows = " + rows + "; cols = " + cols +
                "; leftTopOfGrid = (" + leftTopOfGrid.x + ";" + leftTopOfGrid.y + ")";
    }
}
