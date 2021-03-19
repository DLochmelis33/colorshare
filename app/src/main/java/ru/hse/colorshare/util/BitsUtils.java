package ru.hse.colorshare.util;

import ru.hse.colorshare.coding.BitArray;

public class BitsUtils {

    public static boolean getIthBit(byte value, int index) {
        if (index < 0 || index >= Byte.SIZE) {
            throw new IllegalArgumentException();
        }
        return ((value >> index) & 1) != 0;
    }

    public static boolean getIthBit(int value, int index) {
        if (index < 0 || index >= Integer.SIZE) {
            throw new IllegalArgumentException();
        }
        return ((value >> index) & 1) != 0;
    }

    public static int fromBitArray(BitArray array, int offset, int length) {
        if (offset + length >= array.length || offset < 0 || length < 0) {
            throw new IllegalArgumentException();
        }
        int value = 0;
        for (int i = 0; i < length; i++) {
            value += array.data.get(offset + length) ? (1 << i) : 0;
        }
        return value;
    }
}
