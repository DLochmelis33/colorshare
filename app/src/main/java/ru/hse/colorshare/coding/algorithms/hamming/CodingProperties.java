package ru.hse.colorshare.coding.algorithms.hamming;

public class CodingProperties {
    public int sourceSize, controlBits;

    public CodingProperties(int sourceSize, int controlBits) {
        this.sourceSize = sourceSize;
        this.controlBits = controlBits;
    }

    private static int calculateControlBits(int sourceSize) {
        return (int) Math.log(sourceSize) + 1;
    }

    public static CodingProperties ofSourceSize(int sourceSize) {
        return new CodingProperties(sourceSize, calculateControlBits(sourceSize));
    }

    public static CodingProperties ofEncodedSize(int encodedSize) {
        int left = 0, right = encodedSize, current;
        while (left + 1 < right) {
            current = (left + right) / 2;
            if (current + calculateControlBits(current) < encodedSize) {
                left = current;
            } else {
                right = current;
            }
        }
        return new CodingProperties(right, encodedSize - right);
    }
}
