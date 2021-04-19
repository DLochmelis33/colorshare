package ru.hse.colorshare.coding.algorithms.hamming;

import ru.hse.colorshare.util.BitsUtils;
import ru.hse.colorshare.util.TransformBoolean;

public class ShortBitArray {
    private int data, length;

    public ShortBitArray(int data, int length) {
        this.data = data;
        this.length = length;
    }

    public int length() {
        return length;
    }

    public int toInteger() {
        return data;
    }

    public void set(int index, boolean value) {
        assert 0 <= index && index < Integer.SIZE;
        data &= value ? 1 << index : ~(1 << index);
    }

    public void update(int index, TransformBoolean transform) {
        set(index, transform.apply(get(index)));
    }

    public boolean get(int index) {
        assert 0 <= index && index < Integer.SIZE;
        return BitsUtils.getIthBit(data, index);
    }

    public byte[] toByteArray() {
        byte[] array = new byte[(length + Byte.SIZE - 1) / Byte.SIZE];
        for (int inArray = 0; inArray < array.length; inArray++) {
            for (int inByte = 0; inByte < Byte.SIZE; inByte++) {
                array[inArray] |= BitsUtils.getIthBit(data, inArray * Byte.SIZE + inByte) ? (1 << inByte) : 0;
            }
        }
        return array;
    }

    public static ShortBitArray valueOf(byte[] array) {
        assert array.length <= Integer.SIZE / Byte.SIZE;
        int length = array.length * Byte.SIZE;
        int data = 0;
        for (int inArray = 0; inArray < array.length; inArray++) {
            for (int inByte = 0; inByte < Byte.SIZE; inByte++) {
                data |= BitsUtils.getIthBit(array[inArray], inByte) ? (1 << (inArray * Byte.SIZE + inByte)) : 0;
            }
        }
        return new ShortBitArray(data, length);
    }


}
