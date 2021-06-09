package ru.hse.colorshare.coding.encoding;

/*
    Класс, описывающий массив дата фрэймов.
 */

import androidx.annotation.NonNull;

import java.util.Arrays;

public class DataFrameBulk {
    private final ColorDataFrame[] bulk;
    private int current = 0;

    public DataFrameBulk(ColorDataFrame[] bulk) {
        this.bulk = bulk;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NonNull
    public DataFrameBulk clone() {
        return new DataFrameBulk(bulk.clone());
    }

    public ColorDataFrame[] getDataFrames() {
        return bulk;
    }

    public int getBulkIndex() { // TODO: move bulk index here from controller
        return 65;
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
