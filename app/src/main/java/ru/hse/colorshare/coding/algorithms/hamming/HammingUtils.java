package ru.hse.colorshare.coding.algorithms.hamming;


import java.util.BitSet;

import ru.hse.colorshare.util.Generator;
import ru.hse.colorshare.util.LimitGenerator;
import ru.hse.colorshare.util.Supplier;

public class HammingUtils {

    public static int calculateControlBits(int frameSize) {
        return (int) Math.log(frameSize) + 1;
    }

    public static int fromBoolean(boolean[] array) {
        assert array.length == Byte.SIZE;
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i] ? (1 << i) : 0;
        }
        return result;
    }

    public static class Entry {
        public final int actual, count;

        public Entry(int actual, int count) {
            this.actual = actual;
            this.count = count;
        }
    }

    public static boolean[] calculateControlBits(BitSet input, int countOfControlBits) {
        return calculateParityBits(ofNonControl(input.length()), input, countOfControlBits);
    }

    public static boolean[] calculateSyndrome(BitSet input, int countOfControlBits) {
        return calculateParityBits(ofAll(input.length()), input, countOfControlBits);
    }

    public static boolean[] calculateParityBits(Generator<Entry> positions, BitSet input, int countOfControlBits) {
        boolean[] controlBits = new boolean[countOfControlBits];
        positions.forEach(
                e -> {
                    for (int i = 0; i < controlBits.length; i++) {
                        controlBits[i] ^= ((((e.actual + 1) >> i) & 1) != 0) & input.get(e.count);
                    }
                }
        );
        return controlBits;
    }

    public static Generator<Entry> ofAll(long limit) {
        return new LimitGenerator<>(new Supplier<Entry>() {
            int current = 0;

            @Override
            public Entry get() {
                current++;
                return new Entry(current - 1, current - 1);
            }
        }, limit);
    }

    public static Generator<Entry> ofNonControl(long limit) {
        return new LimitGenerator<>(new Supplier<Entry>() {
            int actual = 0, count = 0;
            int up = 0;

            @Override
            public Entry get() {
                while (actual + 1 == (1 << up)) {
                    actual++;
                    up++;
                }
                return new Entry(actual++, count++);
            }
        }, limit);
    }

    public static Generator<Entry> ofControl(long limit) {
        return new LimitGenerator<>(new Supplier<Entry>() {
            int actual = 0, count = 0;

            @Override
            public Entry get() {
                actual = (1 << count) - 1;
                return new Entry(actual, count++);
            }
        }, limit);
    }
}
