package ru.hse.colorshare;

import android.graphics.RectF;
import androidx.annotation.NonNull;

public final class TransmissionParams {
    private final RectF unitRect;
    private final int rows;
    private final int cols;

    public TransmissionParams(RectF unitRect, int rows, int cols) {
        this.unitRect = unitRect;
        this.rows = rows;
        this.cols = cols;
    }

    public RectF getUnitRect() {
        return unitRect;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    @Override
    public @NonNull
    String toString() {
        return "unit rect: height = " + unitRect.height() +
                ", width = " + unitRect.width() +
                "; rows = " + rows + "; cols = " + cols;
    }
}
