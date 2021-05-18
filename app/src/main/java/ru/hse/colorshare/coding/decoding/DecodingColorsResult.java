package ru.hse.colorshare.coding.decoding;

public class DecodingColorsResult {
    public final byte[] encodedBytes;
    public final int unreadColors;

    public DecodingColorsResult(int unreadColors, byte[] encodedBytes) {
        this.unreadColors = unreadColors;
        this.encodedBytes = encodedBytes;
    }
}
