package ru.hse.colorshare.coding;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorFrame {
    public static class Unit {
        private final int color;
        private final int bits;

        private Unit(int color, int bits) {
            this.color = color;
            this.bits = bits;
        }
    }

    private static final int UNIT_LENGTH = 3;

    private static final List<Unit> allUnits;
    private static final Map<Integer, Unit> fromColors = new HashMap<>();
    private static final Map<Integer, Unit> fromBits = new HashMap<>();

    static {
        allUnits = Arrays.asList(
                new Unit(Color.RED, 0b000),
                new Unit(Color.BLUE, 0b001),
                new Unit(Color.GREEN, 0b010),
                new Unit(Color.BLACK, 0b011),
                new Unit(Color.WHITE, 0b100),
                new Unit(Color.YELLOW, 0b101),
                new Unit(Color.MAGENTA, 0b110),
                new Unit(Color.GRAY, 0b111));
        for (Unit unit : allUnits) {
            fromColors.put(unit.color, unit);
            fromBits.put(unit.bits, unit);
        }
    }

    public static Unit unitOfColor(Integer color) {
        assert fromColors.containsKey(color);
        return fromColors.get(color);
    }

    public static Unit unitOfColor(int color) {
        assert fromColors.containsKey(color);
        return fromColors.get(color);
    }

    public static Unit unitOfBits(int value) {
        assert 0 <= value && value < 8;
        return fromBits.get(value);
    }


    private final List<Unit> units;

    public static ColorFrame ofColors(List<Integer> colors) {
        List<Unit> units = new ArrayList<>(colors.size());
        for (Integer color : colors) {
            units.add(unitOfColor(color));
        }
        return new ColorFrame(units);
    }

    public static ColorFrame ofBits(BitArray array) {
        assert array.length % UNIT_LENGTH == 0;
        List<Unit> units = new ArrayList<>(array.length / 3);
        for (int i = 0; i < array.length; i += UNIT_LENGTH) {
            int b = 0;
            for (int k = 0; k < UNIT_LENGTH; k++) {
                b += array.data.get(i + k) ? 1 << (UNIT_LENGTH - 1 - k) : 0;
            }
            units.add(unitOfBits(b));
        }
        return new ColorFrame(units);
    }

    public ColorFrame(List<Unit> units) {
        this.units = units;
    }
}
