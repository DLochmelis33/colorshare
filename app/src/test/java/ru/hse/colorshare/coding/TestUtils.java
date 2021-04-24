package ru.hse.colorshare.coding;

import java.util.Map;

import ru.hse.colorshare.frames.TwoBitsFrameUtil;

public class TestUtils {
    public static final Map<Integer, Integer> map = TwoBitsFrameUtil.FROM_BITS;

    public static int[] colorsFromBytes(int[] bytes) {
        int[] colors = new int[bytes.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = map.get(bytes[i]);
        }
        return colors;
    }


    public static byte[] toBytes(int... ints) {
        int[] array = ints.clone();
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte)array[i];
        }
        return bytes;
    }
}
