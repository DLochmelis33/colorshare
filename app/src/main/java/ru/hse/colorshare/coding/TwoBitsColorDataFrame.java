package ru.hse.colorshare.coding;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static ru.hse.colorshare.coding.TwoBitsFrameUtil.ALL_TWO_BIT_UNITS;
import static ru.hse.colorshare.coding.TwoBitsFrameUtil.UNIT_BITS_COUNT;
import static ru.hse.colorshare.coding.TwoBitsFrameUtil.writeByteAsColors;

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
    public int[] getColors() {
        return colors;
    }

}
