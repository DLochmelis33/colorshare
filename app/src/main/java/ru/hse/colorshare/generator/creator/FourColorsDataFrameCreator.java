package ru.hse.colorshare.generator.creator;

import java.util.Objects;
import java.util.zip.Checksum;

import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.FROM_BITS;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.UNITS_PER_BYTE;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.EMPTY_COLOR;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.BITS_PER_UNIT;

public class FourColorsDataFrameCreator extends AbstractColorDataFrameCreator {
    public FourColorsDataFrameCreator(int unitsPerFrame, int framesPerBulk, Checksum checksum) {
        super(unitsPerFrame, framesPerBulk, checksum);
    }

    private void writeByteAsColors(int[] colors, int offset, byte toEncode) {
        for (int unit = 0; unit < UNITS_PER_BYTE; unit++, toEncode >>= BITS_PER_UNIT) {
            Integer color = FROM_BITS.get(toEncode & ((1 << BITS_PER_UNIT) - 1));
            Objects.requireNonNull(color);
            colors[offset + unit] = color;
        }
    }

    @Override
    public CreationResult create(byte[] buffer, int offset, int length) {
        ColorDataFrame[] bulk = new ColorDataFrame[framesPerBulk];
        int inBuffer = 0;
        for (int inBulk = 0; inBulk < framesPerBulk; inBulk++) {
            int[] colors = new int[unitsPerFrame];
            int inColors = 0, inBufferBefore = inBuffer;
            for (;
                 inColors + UNITS_PER_BYTE <= unitsPerFrame && inBuffer < length;
                 inColors += UNITS_PER_BYTE, inBuffer++
            ) {
                writeByteAsColors(colors, inColors, buffer[offset + inBuffer]);
            }
            for (; inColors < unitsPerFrame; inColors++) {
                colors[inColors] = EMPTY_COLOR;
            }
            checksum.update(buffer, inBufferBefore, inBuffer - inBufferBefore);
            bulk[inBulk] = new SimpleColorDataFrame(colors, checksum.getValue());
            checksum.reset();
        }
        return new CreationResult(
                new BulkColorDataFrames(bulk),
                length - inBuffer
        );
    }

    @Override
    public int estimateBufferSize() {
        return (int) (framesPerBulk * Math.ceil((double) unitsPerFrame / ((double) Byte.SIZE / BITS_PER_UNIT)));
    }
}
