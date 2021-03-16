package ru.hse.colorshare.coding.algorithms.hamming;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.BitSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Util {
    public static class Entry {
        final int actual, count;

        public Entry(int actual, int count) {
            this.actual = actual;
            this.count = count;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean[] calculateControlBits(BitSet input, int countOfControlBits) {
        boolean[] controlBits = new boolean[countOfControlBits];
        int length = input.length();
        ofNonControl(length).forEach(
                e -> {
                    for (int i = 0; i < controlBits.length; i++) {
                        controlBits[i] ^= ((((e.actual + 1) >> i) & 1) != 0) & input.get(e.count);
                    }
                }
        );
        return controlBits;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Stream<Entry> ofNonControl(long limit) {
        return Stream.generate(new Supplier<Entry>() {
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
        }).limit(limit);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Stream<Entry> ofControl(long limit) {
        return Stream.generate(new Supplier<Entry>() {
            int actual = 0, count = 0;

            @Override
            public Entry get() {
                actual = (1 << count) - 1;
                return new Entry(actual, count++);
            }
        }).limit(limit);
    }
}
