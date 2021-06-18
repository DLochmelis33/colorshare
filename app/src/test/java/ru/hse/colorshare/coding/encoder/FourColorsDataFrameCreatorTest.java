package ru.hse.colorshare.coding.encoder;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import ru.hse.colorshare.TestUtils;
import ru.hse.colorshare.coding.encoding.ColorDataFrame;
import ru.hse.colorshare.coding.encoding.impl.FourColorsDataFrameEncoder;
import ru.hse.colorshare.coding.encoding.DataFrameEncoder;

import static ru.hse.colorshare.TestUtils.colorsFromBytes;

public class FourColorsDataFrameCreatorTest {


    @Test
    public void testSimple_unitsPerFrameDividing4() {
        DataFrameEncoder creator = new FourColorsDataFrameEncoder(4, new TestUtils.MockChecksum());

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int b1 = 0b10101111, b2 = 0b01010111, b3 = 0b11101111;
        buffer.put(TestUtils.toBytes(b1, b2, b3));
        buffer.flip();
        ColorDataFrame result = creator.encode(buffer);

        int[] colors1 = colorsFromBytes(new int[]{
                0b11, 0b11, 0b10, 0b10
        });

        Assert.assertEquals(1, buffer.position());
        Assert.assertArrayEquals(colors1, result.getColors());
        Assert.assertEquals((byte) b1, result.getChecksum());

        result = creator.encode(buffer);

        int[] colors2 = colorsFromBytes(new int[]{
                0b11, 0b01, 0b01, 0b01
        });

        Assert.assertEquals(2, buffer.position());
        Assert.assertArrayEquals(colors2, result.getColors());
        Assert.assertEquals((byte) b2, result.getChecksum());
    }

    @Test
    public void testSimple_unitsPerFrameNotDividing4() {
        DataFrameEncoder creator = new FourColorsDataFrameEncoder(6, new TestUtils.MockChecksum());

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int b1 = 0b00000000, b2 = 0b00000001, b3 = 0b11111111;
        buffer.put(TestUtils.toBytes(b1, b2, b3));
        buffer.flip();

        ColorDataFrame result = creator.encode(buffer);

        int[] colors1 = colorsFromBytes(new int[]{
                0b00, 0b00, 0b00, 0b00
        }, 6);

        Assert.assertEquals(1, buffer.position());
        Assert.assertArrayEquals(colors1, result.getColors());
        Assert.assertEquals((byte) b1, result.getChecksum());

        int[] colors2 = colorsFromBytes(new int[]{
                0b01, 0b00, 0b00, 0b00
        }, 6);

        result = creator.encode(buffer);
        Assert.assertEquals(2, buffer.position());
        Assert.assertArrayEquals(colors2, result.getColors());
        Assert.assertEquals((byte) b2, result.getChecksum());

    }
}
