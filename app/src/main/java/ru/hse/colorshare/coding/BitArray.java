package ru.hse.colorshare.coding;

import java.util.BitSet;

public class BitArray {
    public final BitSet data;
    public final int length;

    public BitArray(int length) {
        this(new BitSet(length), length);
    }

    public BitArray(BitSet data, int length) {
        this.data = data;
        this.length = length;
    }

    public BitArray(byte[] array) {
        this(array, 0, array.length);
    }

    public BitArray(byte[] array, int offset, int length) {
        this.length = length * Byte.SIZE;
        this.data = new BitSet(this.length);
        for (int inArray = offset; inArray < array.length; inArray++) {
            for (int inByte = 0; inByte < Byte.SIZE; inByte++) {
                this.data.set(inArray * Byte.SIZE + inByte, ((array[inArray] >> inByte) & 1) != 0);
            }
        }
    }
}
