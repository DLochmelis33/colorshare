package ru.hse.colorshare.coding.algorithms;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;
import java.util.List;

import ru.hse.colorshare.coding.Decoder;
import ru.hse.colorshare.coding.Encoder;
import ru.hse.colorshare.coding.algorithms.hamming.CodingProperties;
import ru.hse.colorshare.coding.algorithms.hamming.HammingDecoder;
import ru.hse.colorshare.coding.algorithms.hamming.HammingEncoder;
import ru.hse.colorshare.coding.algorithms.hamming.HammingUtils;
import ru.hse.colorshare.coding.dto.BitArray;

public class HammingCodingTest {

    @Test
    public void testCalculateSourceFrameSize() {
        List<Integer> sourceSizes = Arrays.asList(15, 4, 20, 24, 28);
        for (Integer sourceSize : sourceSizes) {
            CodingProperties properties = CodingProperties.ofSourceSize(sourceSize);
            Assert.assertEquals(properties.sourceSize, CodingProperties.ofEncodedSize(properties.sourceSize + properties.controlBits).sourceSize);
        }
    }

    public void checkBitArrays(BitArray expected, BitArray actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected.get(i), expected.get(i));
        }
    }

    @Test
    public void testEndToEndWithoutError() {
        Encoder encoder = new HammingEncoder();
        Decoder decoder = new HammingDecoder();

        BitArray bitArray = new BitArray(new byte[]{-120, 10, 34});

        checkBitArrays(bitArray, decoder.decode(encoder.encode(bitArray)));
    }

    @Test
    public void testEndToEndWithError() {
        Encoder encoder = new HammingEncoder();
        Decoder decoder = new HammingDecoder();

        BitArray bitArray = new BitArray(new byte[]{1, 2, -128});

        BitArray encoded = encoder.encode(bitArray);
        encoded.flip(12);

        checkBitArrays(bitArray, decoder.decode(encoded));
    }
}
