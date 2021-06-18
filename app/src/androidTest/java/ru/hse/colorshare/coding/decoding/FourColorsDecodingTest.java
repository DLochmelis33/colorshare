package ru.hse.colorshare.coding.decoding;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import ru.hse.colorshare.coding.decoding.impl.FourColorsDataFrameDecoder;
import ru.hse.colorshare.coding.encoding.ColorDataFrame;
import ru.hse.colorshare.coding.encoding.DataFrameEncoder;
import ru.hse.colorshare.coding.encoding.impl.FourColorsDataFrameEncoder;

import ru.hse.colorshare.coding.util.FourColorsDataFrameUtil;

@RunWith(AndroidJUnit4.class)
public class FourColorsDecodingTest {
    private ColorDataFrameDecoder decoder;

    public static byte[] toBytes(int... ints) {
        int[] array = ints.clone();
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte) array[i];
        }
        return bytes;
    }

    @Before
    public void init() {
        decoder = new FourColorsDataFrameDecoder(new CRC32());
    }

    @Test
    public void testSimple() {
        int[] colors = new int[]{
                FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(0).color,
                FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(1).color,
                FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(2).color,
                FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS.get(0).color,
                FourColorsDataFrameUtil.EMPTY_COLOR,
                FourColorsDataFrameUtil.EMPTY_COLOR,
                FourColorsDataFrameUtil.EMPTY_COLOR,
                FourColorsDataFrameUtil.EMPTY_COLOR
        };

        ByteDataFrame frame = decoder.decode(colors);
        Assert.assertEquals(1, frame.getBytes().length);
        Assert.assertEquals(0b00_01_11_00, frame.getBytes()[0]);
    }

    @Test
    public void e2eTest() {
        DataFrameEncoder encoder = new FourColorsDataFrameEncoder(8, new CRC32());

        int b1 = 0b10101111, b2 = 0b01010111;
        byte[] expected = toBytes(b1, b2);
        ByteBuffer buffer = ByteBuffer.wrap(expected);
        ColorDataFrame colors = encoder.encode(buffer);

        ByteDataFrame bytes = decoder.decode(colors.getColors());

        Assert.assertArrayEquals(expected, bytes.getBytes());
        Assert.assertEquals(colors.getChecksum(), bytes.getChecksum());

        expected = toBytes(b1);
        buffer = ByteBuffer.wrap(expected);
        colors = encoder.encode(buffer);

        bytes = decoder.decode(colors.getColors());

        Assert.assertArrayEquals(expected, bytes.getBytes());
        Assert.assertEquals(colors.getChecksum(), bytes.getChecksum());
    }

}