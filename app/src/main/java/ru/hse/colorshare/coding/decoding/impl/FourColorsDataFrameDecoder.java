package ru.hse.colorshare.coding.decoding.impl;

import android.graphics.Color;

import java.util.Objects;
import java.util.zip.Checksum;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;
import ru.hse.colorshare.coding.decoding.ColorDataFrameDecoder;

import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS;
import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.BITS_PER_UNIT;
import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.FROM_COLORS;
import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.UNITS_PER_BYTE;

public class FourColorsDataFrameDecoder implements ColorDataFrameDecoder {

    private final Checksum checksum;

    public FourColorsDataFrameDecoder(Checksum checksum) {
        this.checksum = checksum;
    }

    private int distance(int color1, int color2) {
        return Math.abs(Color.red(color1) - Color.red(color2)) +
                Math.abs(Color.green(color1) - Color.green(color2)) +
                Math.abs(Color.blue(color1) - Color.blue(color2));
    }

    private int chooseClosestColor(int color) {
        int currentOptimalIndex = 0;
        int currentOptimalDistance = Integer.MAX_VALUE;
        for (int i = 0; i < ALL_TWO_BIT_UNITS.size(); i++) {
            int currentDistance = distance(color, ALL_TWO_BIT_UNITS.get(i).color);
            if (currentDistance < currentOptimalDistance) {
                currentOptimalIndex = i;
                currentOptimalDistance = currentDistance;
            }
        }
        return ALL_TWO_BIT_UNITS.get(currentOptimalIndex).color;
    }

    private byte readColorsAsByte(int[] colors, int offset) {
        byte result = 0;
        for (int i = 0; i < UNITS_PER_BYTE; i++) {
            Integer bits = FROM_COLORS.get(chooseClosestColor(colors[offset + i]));
            Objects.requireNonNull(bits);
            result |= bits << i * BITS_PER_UNIT;
        }
        return result;
    }

    @Override
    public ByteDataFrame decode(int[] colors) {
        if (colors.length % UNITS_PER_BYTE != 0)
            throw new IllegalArgumentException("Illegal data frame colors");

        byte[] decodedBytes = new byte[colors.length / UNITS_PER_BYTE];
        int currentDecoded = 0;
        for (int inArray = 0; inArray + UNITS_PER_BYTE < colors.length; inArray += UNITS_PER_BYTE, currentDecoded++) {
            decodedBytes[currentDecoded] = readColorsAsByte(colors, inArray);
        }
        checksum.reset();
        checksum.update(decodedBytes, 0, decodedBytes.length);
        return new SimpleByteDataFrame(decodedBytes, checksum.getValue());
    }

    @Override
    public int estimateBufferSize(int colorsCount) {
        return colorsCount / UNITS_PER_BYTE;
    }
}
