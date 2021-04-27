package ru.hse.colorshare.frames;

public class BulkColorDataFrames {
    private final ColorDataFrame[] bulk;
    private int current = 0;

    public BulkColorDataFrames(ColorDataFrame[] bulk) {
        this.bulk = bulk;
        current = bulk.length;
    }

    public BulkColorDataFrames(int size) {
        bulk = new ColorDataFrame[size];
    }

    public void appendDataFrame(ColorDataFrame dataFrame) {
        if (current >= bulk.length)
            throw new IllegalStateException();
        bulk[current++] = dataFrame;
    }

    public ColorDataFrame[] getDataFrames() {
        return bulk;
    }
}
