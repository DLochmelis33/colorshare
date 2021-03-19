package ru.hse.colorshare.coding;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.hse.colorshare.util.BitsUtils;

public final class ThreeBitColorFrame implements DataFrame {

    public static final class ThreeBitUnit implements DataFrame.Unit{
        private final int color;
        private final int bits;

        private ThreeBitUnit(int color, int bits) {
            this.color = color;
            this.bits = bits;
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public int getEncodedValue() {
            return bits;
        }
    }

    private static final int UNIT_BITS_COUNT = 3;

    private static final List<ThreeBitUnit> ALL_THREE_BIT_UNITS;
    private static final Map<Integer, ThreeBitUnit> FROM_COLORS = new HashMap<>();
    private static final Map<Integer, ThreeBitUnit> FROM_BITS = new HashMap<>();

    static {
        ALL_THREE_BIT_UNITS = Arrays.asList(
                new ThreeBitUnit(Color.RED, 0b000),
                new ThreeBitUnit(Color.BLUE, 0b001),
                new ThreeBitUnit(Color.GREEN, 0b010),
                new ThreeBitUnit(Color.BLACK, 0b011),
                new ThreeBitUnit(Color.WHITE, 0b100),
                new ThreeBitUnit(Color.YELLOW, 0b101),
                new ThreeBitUnit(Color.MAGENTA, 0b110),
                new ThreeBitUnit(Color.GRAY, 0b111));
        for (ThreeBitUnit unit : ALL_THREE_BIT_UNITS) {
            FROM_COLORS.put(unit.color, unit);
            FROM_BITS.put(unit.bits, unit);
        }
    }

    public static ThreeBitUnit unitOfColor(Integer color) {
        assert FROM_COLORS.containsKey(color);
        return FROM_COLORS.get(color);
    }

    public static ThreeBitUnit unitOfColor(int color) {
        assert FROM_COLORS.containsKey(color);
        return FROM_COLORS.get(color);
    }

    public static ThreeBitUnit unitOfBits(int value) {
        assert 0 <= value && value < 8;
        return FROM_BITS.get(value);
    }


    private final List<DataFrame.Unit> units;

    @Override
    public List<Unit> getUnits() {
        return units;
    }


    public static ThreeBitColorFrame ofColors(List<Integer> colors) {
        List<DataFrame.Unit> units = new ArrayList<>(colors.size());
        for (Integer color : colors) {
            units.add(unitOfColor(color));
        }
        return new ThreeBitColorFrame(units);
    }
    
    private static Unit interpretLastBits(BitArray array) {
        if (array.length % UNIT_BITS_COUNT == 0) {
            return unitOfBits(0);
        }
        if (array.length % UNIT_BITS_COUNT == 1) {
            return array.data.get(array.length - 1) ? unitOfBits(0b110) : unitOfBits(0b100);
        }
        boolean last = array.data.get(array.length - 1);
        boolean prevLast = array.data.get(array.length - 2);
        return unitOfBits((prevLast ? 1 << 2 : 0) + (last ? 1 << 1 : 0) + 1);
    }

    public static ThreeBitColorFrame ofBits(BitArray array) {
        List<Unit> units = new ArrayList<>(array.length / 3 + 1);
        for (int i = 0; i < array.length; i += UNIT_BITS_COUNT) {
            units.add(unitOfBits(
                    BitsUtils.fromBitArray(array, i, UNIT_BITS_COUNT)
            ));
        }
        units.add(interpretLastBits(array));
        return new ThreeBitColorFrame(units);
    }

    private ThreeBitColorFrame(List<Unit> units) {
        this.units = units;
    }
}
