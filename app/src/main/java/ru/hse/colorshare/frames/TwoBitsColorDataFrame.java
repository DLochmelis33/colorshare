package ru.hse.colorshare.frames;

import static ru.hse.colorshare.frames.TwoBitsFrameUtil.UNIT_BITS_COUNT;
import static ru.hse.colorshare.frames.TwoBitsFrameUtil.writeByteAsColors;

/*
    Implementation of DataFrame. One color per two bits.
 */

public final class TwoBitsColorDataFrame implements ColorDataFrame {
    private final int[] colors;
    private final long checksum;

    public TwoBitsColorDataFrame(byte[] frame, long checksum) {
        this(frame, 0, frame.length, checksum);
    }

    public TwoBitsColorDataFrame(byte[] frame, int offset, int length, long checksum) {
        int unitsPerByte = Byte.SIZE / UNIT_BITS_COUNT;
        this.checksum = checksum;
        this.colors = new int[length * unitsPerByte];
        for (int inFrame = 0, currentColor = 0; inFrame < length; inFrame++, currentColor += unitsPerByte) {
            writeByteAsColors(colors, currentColor, frame[inFrame + offset]);
        }
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public int[] getChecksumAsColors() {
        int unitsPerByte = Byte.SIZE / UNIT_BITS_COUNT;
        int[] encoded = new int[unitsPerByte * Long.SIZE / Byte.SIZE];
        for (int i = 0; i < Long.SIZE / Byte.SIZE; i++) {
            writeByteAsColors(encoded, i * unitsPerByte, (byte) ((checksum >> (i * 8)) & 255));
        }
        return encoded;
    }

    @Override
    public int[] getColors() {
        return colors;
    }

}
