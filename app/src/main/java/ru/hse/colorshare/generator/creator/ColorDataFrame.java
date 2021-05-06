package ru.hse.colorshare.generator.creator;

/*
    Description of a data structure which is transferring to VIEW
 */

public interface ColorDataFrame {
    long getChecksum();

    int[] getChecksumAsColors();

    int[] getColors();
}
