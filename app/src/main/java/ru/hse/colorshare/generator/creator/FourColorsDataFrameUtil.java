package ru.hse.colorshare.generator.creator;

import android.graphics.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

public class FourColorsDataFrameUtil {
    public static final class FourColorUnit {
        public final int color;
        public final int encodedValue;

        public FourColorUnit(int color, int encodedValue) {
            this.color = color;
            this.encodedValue = encodedValue;
        }
    }

    public static final int BITS_PER_UNIT = 2;

    public static final int UNITS_PER_BYTE = Byte.SIZE / BITS_PER_UNIT;

    public static final Map<Integer, Integer> FROM_BITS = new HashMap<>();
    public static final Map<Integer, Integer> FROM_COLORS = new HashMap<>();

    public static final List<FourColorUnit> ALL_TWO_BIT_UNITS = Arrays.asList(
            new FourColorUnit(Color.BLUE, 0b00),
            new FourColorUnit(Color.BLACK, 0b11),
            new FourColorUnit(Color.RED, 0b01),
            new FourColorUnit(Color.GREEN, 0b10));

    public static final int EMPTY_COLOR = Color.WHITE;

    static {
        for (FourColorUnit unit : ALL_TWO_BIT_UNITS) {
            FROM_BITS.put(unit.encodedValue, unit.color);
            FROM_COLORS.put(unit.color, unit.encodedValue);
        }
    }



    // TODO
    public static byte readColorsAsByte(int[] colors, int offset) {
        return 0;
    }
}
