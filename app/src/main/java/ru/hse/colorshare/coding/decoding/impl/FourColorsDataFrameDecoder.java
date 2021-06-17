package ru.hse.colorshare.coding.decoding.impl;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.Checksum;

import ru.hse.colorshare.coding.decoding.ByteDataFrame;
import ru.hse.colorshare.coding.decoding.ColorDataFrameDecoder;
import ru.hse.colorshare.coding.util.FourColorsDataFrameUtil;

import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.BITS_PER_UNIT;
import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.FROM_COLORS;
import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.UNITS_PER_BYTE;

public class FourColorsDataFrameDecoder implements ColorDataFrameDecoder {

    private final Checksum checksum;
    private final OptimalColorChooser chooser;

    public FourColorsDataFrameDecoder(Checksum checksum) {
        this.checksum = checksum;
        this.chooser = new FourColorsMetricsBasedColorChooser(new EuclidMetrics());
    }

    private byte readColorsAsByte(int[] colors, int offset) {
        byte result = 0;
        for (int i = 0; i < UNITS_PER_BYTE; i++) {
            Integer bits = FROM_COLORS.get(chooser.chooseClosest(colors[offset + i]));
            Objects.requireNonNull(bits);
            result |= bits << i * BITS_PER_UNIT;
        }
        return result;
    }

    @Override
    public ByteDataFrame decode(int[] colors) {
        if (colors.length % UNITS_PER_BYTE != 0)
            throw new IllegalArgumentException("Illegal data frame colors");

        ByteBuffer decodedBytes = ByteBuffer.allocate(colors.length / UNITS_PER_BYTE);
        int currentDecoded = 0;
        for (int inArray = 0; inArray + UNITS_PER_BYTE < colors.length; inArray += UNITS_PER_BYTE, currentDecoded++) {
            if (chooser.chooseClosest(colors[inArray]) == FourColorsDataFrameUtil.EMPTY_COLOR) {
                break;
            }
            decodedBytes.put(readColorsAsByte(colors, inArray));
        }
        byte[] bytes = new byte[decodedBytes.limit()];
        decodedBytes.flip();
        decodedBytes.get(bytes);
        checksum.reset();
        checksum.update(bytes, 0, bytes.length);
        return new SimpleByteDataFrame(bytes, checksum.getValue());
    }

    @Override
    public int estimateBufferSize(int colorsCount) {
        return colorsCount / UNITS_PER_BYTE;
    }
}
