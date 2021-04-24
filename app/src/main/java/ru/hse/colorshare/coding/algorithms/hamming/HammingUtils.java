package ru.hse.colorshare.coding.algorithms.hamming;


import ru.hse.colorshare.util.BitsUtils;
import ru.hse.colorshare.util.Generator;
import ru.hse.colorshare.util.LimitGenerator;
import ru.hse.colorshare.util.Supplier;

/*
    https://en.wikipedia.org/wiki/Hamming_code
    "Hamming codes" tab
 */

public class HammingUtils {

    public static class Entry {
        public final int value, index;

        public Entry(int value, int index) {
            this.value = value;
            this.index = index;
        }
    }

    public static ShortBitArray calculateControlBits(BitArray input, int countOfControlBits) {
        return calculateParityBits(ofNonControl(input.length), input, countOfControlBits);
    }

    public static ShortBitArray calculateSyndrome(BitArray input, int countOfControlBits) {
        return calculateParityBits(ofAll(input.length), input, countOfControlBits);
    }

    public static ShortBitArray calculateParityBits(Generator<Entry> positions, BitArray input, int countOfControlBits) {
        ShortBitArray controlBits = new ShortBitArray(0, countOfControlBits);
        positions.forEach(
                e -> {
                    for (int i = 0; i < controlBits.length(); i++) {
                        boolean toUpdate = BitsUtils.getIthBit(e.value + 1, i) & input.get(e.index);
                        controlBits.update(i, (b) -> b ^ toUpdate);
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
