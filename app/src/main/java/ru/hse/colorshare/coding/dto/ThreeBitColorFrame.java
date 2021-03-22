package ru.hse.colorshare.coding.dto;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.hse.colorshare.util.BitsUtils;

/*
    Implementation of DataFrame. One color per three bits. I guess two or four bit per color better. We will discuss it in future
 */

public final class ThreeBitColorFrame implements DataFrame {

    public static final class ThreeBitUnit extends AbstractUnit {
        public ThreeBitUnit(int color, int encodedValue) {
            super(color, encodedValue);
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
            FROM_COLORS.put(unit.getColor(), unit);
            FROM_BITS.put(unit.getEncodedValue(), unit);
        }
    }

    private static ThreeBitUnit unitOfColor(Integer color) {
        assert FROM_COLORS.containsKey(color);
        return FROM_COLORS.get(color);
    }

    private static ThreeBitUnit unitOfColor(int color) {
        assert FROM_COLORS.containsKey(color);
        return FROM_COLORS.get(color);
    }

    private static ThreeBitUnit unitOfBits(int value) {
        assert 0 <= value && value < 8;
        return FROM_BITS.get(value);
    }


    private final List<DataFrame.Unit> units;
    private final long checksum;

    @Override
    public List<Unit> getUnits() {
        return units;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    public static DataFrame valueOf(byte[] bytes, int offset, int length, long checksum) {
        List<Unit> units = new ArrayList<>();
        for (int inArray = offset; inArray < length; inArray++) {
            int inByte = 0;
            for (; inByte + UNIT_BITS_COUNT < Byte.SIZE; inByte += UNIT_BITS_COUNT) {
                units.add(unitOfBits(BitsUtils.fromByte(bytes[inArray], inByte, UNIT_BITS_COUNT)));
            }
            units.add(unitOfBits(
                    BitsUtils.fromByte(bytes[inArray], inByte, Byte.SIZE - inByte) << (UNIT_BITS_COUNT - Byte.SIZE + inByte)
            ));
        }
        return new ThreeBitColorFrame(units, checksum);
    }

    public static int estimateByteSize(int frameSize) {
        int unitsPerByte = (Byte.SIZE + UNIT_BITS_COUNT - 1) / UNIT_BITS_COUNT;
        return frameSize / unitsPerByte;
    }

    private ThreeBitColorFrame(List<Unit> units, long checksum) {
        this.units = units;
        this.checksum = checksum;
    }

}
