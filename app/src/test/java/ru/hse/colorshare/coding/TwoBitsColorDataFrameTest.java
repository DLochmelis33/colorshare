package ru.hse.colorshare.coding;


import org.junit.Assert;
import org.junit.Test;

import ru.hse.colorshare.frames.TwoBitsColorDataFrame;

public class TwoBitsColorDataFrameTest {
    @Test
    public void fromBytes() {
        byte[] bytes = TestUtils.toBytes(0b1111_1111, 0b0011_0000);
        int[] colors = new TwoBitsColorDataFrame(bytes, 0).getColors();

        Assert.assertArrayEquals(TestUtils.colorsFromBytes(new int[] {
                0b11, 0b11, 0b11, 0b11,
                0b00, 0b00, 0b11, 0b00
        }), colors);
    }
}
