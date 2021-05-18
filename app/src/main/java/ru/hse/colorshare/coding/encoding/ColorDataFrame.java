package ru.hse.colorshare.coding.encoding;

/*
    Description of a data structure which is transferring to VIEW
 */

public interface ColorDataFrame {
    long getChecksum();

    int[] getColors();
}
