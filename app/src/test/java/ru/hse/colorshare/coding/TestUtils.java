package ru.hse.colorshare.coding;

import java.util.Map;
import java.util.zip.Checksum;

import ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil;

public class TestUtils {

    public static final Map<Integer, Integer> map = FourColorsDataFrameUtil.FROM_BITS;

    public static int[] colorsFromBytes(int[] bytes) {
        return colorsFromBytes(bytes, bytes.length);
    }

    public static int[] colorsFromBytes(int[] bytes, int length) {
        int[] colors = new int[length];
        for (int i = 0; i < bytes.length; i++) {
            colors[i] = map.get(bytes[i]);
        }
        for (int i = bytes.length; i < length; i++) {
            colors[i] = FourColorsDataFrameUtil.EMPTY_COLOR;
        }
        return colors;
    }


    public static byte[] toBytes(int... ints) {
        int[] array = ints.clone();
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte) array[i];
        }
        return bytes;
    }


    public static class MockChecksum implements Checksum {
        long value = 0;

        @Override
        public void update(int b) {
            value += b;
        }

        @Override
        public void update(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) {
                value += b[off + i];
            }
        }

        @Override
        public long getValue() {
            return value;
        }

        @Override
        public void reset() {
            value = 0;
        }
    }

}
