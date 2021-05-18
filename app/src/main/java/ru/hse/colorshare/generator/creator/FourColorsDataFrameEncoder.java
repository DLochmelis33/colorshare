package ru.hse.colorshare.generator.creator;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.Checksum;

import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.BITS_PER_UNIT;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.EMPTY_COLOR;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.FROM_BITS;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.UNITS_PER_BYTE;


public class FourColorsDataFrameEncoder implements DataFrameEncoder {
    private final int unitsPerFrame;

    private final Checksum checksum;

    public FourColorsDataFrameEncoder(int unitsPerFrame, Checksum checksum) {
        this.unitsPerFrame = unitsPerFrame;
        this.checksum = checksum;
    }

    private void writeByteAsColors(byte toEncode, int[] colors, int offset) {
        for (int unit = 0; unit < Byte.SIZE / BITS_PER_UNIT; unit++, toEncode >>= BITS_PER_UNIT) {
            Integer color = FROM_BITS.get(toEncode & ((1 << BITS_PER_UNIT) - 1));
            Objects.requireNonNull(color);
            colors[offset + unit] = color;
        }
    }

    @Override
    public ColorDataFrame encode(ByteBuffer buffer) {
        int[] colors = new int[unitsPerFrame];
        int currentColor = 0;

        for (; buffer.hasRemaining() && currentColor + UNITS_PER_BYTE <= unitsPerFrame; currentColor += UNITS_PER_BYTE) {
            byte toEncode = buffer.get();
            writeByteAsColors(toEncode, colors, currentColor);
            checksum.update(toEncode);
        }

        for (; currentColor < unitsPerFrame; currentColor++) {
            colors[currentColor] = EMPTY_COLOR;
        }

        ColorDataFrame dataFrame = new SimpleColorDataFrame(colors, checksum.getValue());
        checksum.reset();

        return dataFrame;
    }

    @Override
    public int estimateBufferSize() {
        return 1 + unitsPerFrame / UNITS_PER_BYTE;
    }
}
