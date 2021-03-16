package ru.hse.colorshare.coding.algorithms;


import org.junit.Test;

import java.util.BitSet;

import ru.hse.colorshare.coding.algorithms.hamming.HammingEncoder;

public class HammingEncodingTest {

    @Test
    public void testEncodeFrame() {
        HammingEncoder encoder = new HammingEncoder();

        BitSet set = new BitSet(15);
        set.set(0);
        set.set(3);
        set.set(6);
        set.set(8);
        set.set(9);
        set.set(10);
        set.set(14);

        BitSet bitSet = encoder.encodeFrame(set);
        for (int i = 0; i < 25; i++) {
            System.out.println((i + 1) + " "+ bitSet.get(i));
        }
    }
}
