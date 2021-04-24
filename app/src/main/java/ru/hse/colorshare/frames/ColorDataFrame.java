package ru.hse.colorshare.frames;

/*
    Description of a data structure which is transferring to VIEW
 */

public interface ColorDataFrame {
    long getChecksum();

    int[] getChecksumAsColors();

    int[] getColors();
}
