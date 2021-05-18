package ru.hse.colorshare.receiver;

public interface ColorDataFrameDecoder {
    default DecodingColorsResult decode(int[] colors) {
        return decode(colors, 0, colors.length);
    }

    DecodingColorsResult decode(int[] colors, int offset, int length);
}
