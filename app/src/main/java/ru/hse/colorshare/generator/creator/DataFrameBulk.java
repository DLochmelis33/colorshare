package ru.hse.colorshare.generator.creator;

/*
    Здесь возможна дополнительная информация
 */

public class DataFrameBulk {
    private final ColorDataFrame[] bulk;
    private int current = 0;

    public DataFrameBulk(ColorDataFrame[] bulk) {
        this.bulk = bulk;
    }

    public ColorDataFrame[] getDataFrames() {
        return bulk;
    }

    public int[] getNextDataFrame() {
        int[] result = bulk[current].getColors();
        current = (current + 1) % bulk.length;
        return result;
    }

    public long[] getChecksums() {
        long[] checksums = new long[bulk.length];
        for (int i = 0; i < bulk.length; i++) {
            checksums[i] = bulk[i].getChecksum();
        }
        return checksums;
    }
}
