package ru.hse.colorshare.coding;


import org.junit.Assert;
import org.junit.Test;

import static ru.hse.colorshare.coding.TestUtils.map;

public class TwoBitsColorDataFrameTest {
    @Test
    public void fromBytes() {
        byte[] bytes = TestUtils.toBytes(0b1111_1111, 0b0011_0000);
        int[] colors = new TwoBitsColorDataFrame(bytes, 0).getColors();

        Assert.assertArrayEquals(
                new int[]{
                        map.get(0b11), map.get(0b11), map.get(0b11), map.get(0b11),
                        map.get(0b00), map.get(0b00), map.get(0b11), map.get(0b00)},
                colors);
    }
}
