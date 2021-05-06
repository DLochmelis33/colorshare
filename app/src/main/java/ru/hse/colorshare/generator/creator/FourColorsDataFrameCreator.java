package ru.hse.colorshare.generator.creator;

import java.util.zip.Checksum;

import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.UNITS_PER_BYTE;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.EMPTY_COLOR;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.BITS_PER_UNIT;
import static ru.hse.colorshare.generator.creator.FourColorsDataFrameUtil.writeByteAsColors;

public class FourColorsDataFrameCreator extends AbstractColorDataFrameCreator {
    public FourColorsDataFrameCreator(int unitsPerFrame, int framesPerBulk, Checksum checksum) {
        super(unitsPerFrame, framesPerBulk, checksum);
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
