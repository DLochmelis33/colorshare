package ru.hse.colorshare.receiver;

import android.graphics.Color;

import java.util.Objects;

import ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil;

import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.BITS_PER_UNIT;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.UNITS_PER_BYTE;

public class FourColorsDataFrameDecoder implements ColorDataFrameDecoder {

    private int distance(int color1, int color2) {
        return Math.abs(Color.red(color1) - Color.red(color2)) +
                Math.abs(Color.green(color1) - Color.green(color2)) +
                Math.abs(Color.blue(color1) - Color.blue(color2));
    }

    private int chooseClosestColor(int color) {
        int currentOptimalIndex = 0;
        int currentOptimalDistance = Integer.MAX_VALUE;
        for (int i = 0; i < FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.size(); i++) {
            int currentDistance = distance(color, FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(i).color);
            if (currentDistance < currentOptimalDistance) {
                currentOptimalIndex = i;
                currentOptimalDistance = currentDistance;
            }
        }
        return FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(currentOptimalIndex).color;
    }

    private byte readColorsAsByte(int[] colors, int offset) {
        byte result = 0;
        for (int i = 0; i < UNITS_PER_BYTE; i++) {
            Integer bits = FourColorsDataFrameUtil.FROM_COLORS.get(chooseClosestColor(colors[offset + i]));
            Objects.requireNonNull(bits);
            result |= bits << i * BITS_PER_UNIT;
        }
        return result;
    }

    @Override
    public DecodingColorsResult decode(int[] colors, int offset, int length) {
        byte[] decodedBytes = new byte[length / UNITS_PER_BYTE];
        int currentDecoded = 0;
        for (int inArray = 0; inArray + UNITS_PER_BYTE < length; inArray += UNITS_PER_BYTE, currentDecoded++) {
            decodedBytes[currentDecoded] = readColorsAsByte(colors, offset + inArray);
        }
        return new DecodingColorsResult(length - (length % UNITS_PER_BYTE), decodedBytes);
    }
}
