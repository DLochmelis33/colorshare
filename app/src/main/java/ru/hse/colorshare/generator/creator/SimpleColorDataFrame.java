package ru.hse.colorshare.generator.creator;

public class SimpleColorDataFrame implements ColorDataFrame {
    protected final int[] colors;
    protected final long checksum;

    public SimpleColorDataFrame(int[] colors, long checksum) {
        this.colors = colors;
        this.checksum = checksum;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public int[] getChecksumAsColors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getColors() {
        return colors;
    }
}
