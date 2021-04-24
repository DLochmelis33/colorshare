package ru.hse.colorshare.frames;

import android.graphics.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TwoBitsFrameUtil {
    public static final class TwoBitUnit {
        public final int color;
        public final int encodedValue;

        public TwoBitUnit(int color, int encodedValue) {
            this.color = color;
            this.encodedValue = encodedValue;
        }
    }
    public static final int UNIT_BITS_COUNT = 2;

    public static final Map<Integer, Integer> FROM_BITS = new HashMap<>();

    public static final List<TwoBitUnit> ALL_TWO_BIT_UNITS = Arrays.asList(
            new TwoBitUnit(Color.WHITE, 0b00),
            new TwoBitUnit(Color.BLACK, 0b11),
            new TwoBitUnit(Color.RED, 0b01),
            new TwoBitUnit(Color.GREEN, 0b10));

    static {
        for (TwoBitsFrameUtil.TwoBitUnit unit : ALL_TWO_BIT_UNITS) {
            FROM_BITS.put(unit.encodedValue, unit.color);
        }
    }

    public static void writeByteAsColors(int[] colors, int offset, byte toEncode) {
        for (int unit = 0; unit < Byte.SIZE / UNIT_BITS_COUNT; unit++, toEncode >>= UNIT_BITS_COUNT) {
            Integer color = TwoBitsFrameUtil.FROM_BITS.get(toEncode & ((1 << UNIT_BITS_COUNT) - 1));
            Objects.requireNonNull(color);
            colors[offset + unit] = color;
        }
    }

}
