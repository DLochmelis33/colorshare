package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import ru.hse.colorshare.generator.creator.ColorDataFrame;
import ru.hse.colorshare.generator.creator.CreationResult;
import ru.hse.colorshare.generator.creator.FourColorsDataFrameCreator;
import ru.hse.colorshare.generator.creator.ColorDataFrameCreator;

import static ru.hse.colorshare.coding.TestUtils.colorsFromBytes;

public class FourColorsDataFrameCreatorTest {

    @Test
    public void testSimple_unitsPerFrameDividing4() {
        ColorDataFrameCreator creator = new FourColorsDataFrameCreator(4, 2, new TestUtils.MockChecksum());

        int b1 = 0b10101111, b2 = 0b01010111, b3 = 0b11101111;
        CreationResult result = creator.create(TestUtils.toBytes(b1, b2, b3));

        ColorDataFrame[] bulk = result.bulk.getDataFrames();

        Assert.assertEquals(2, bulk.length);

        int[] colors1 = colorsFromBytes(new int[]{
                0b11, 0b11, 0b10, 0b10
        });
        Assert.assertArrayEquals(colors1, bulk[0].getColors());

        int[] colors2 = colorsFromBytes(new int[]{
                0b11, 0b01, 0b01, 0b01
        });
        Assert.assertArrayEquals(colors2, bulk[1].getColors());

        Assert.assertEquals((byte) b1, bulk[0].getChecksum());
        Assert.assertEquals((byte) b2, bulk[1].getChecksum());

        Assert.assertEquals(1, result.unread);
    }

    @Test
    public void testSimple_unitsPerFrameNotDividing4() {
        ColorDataFrameCreator creator = new FourColorsDataFrameCreator(6, 2, new TestUtils.MockChecksum());

        int b1 = 0b00000000, b2 = 0b00000001, b3 = 0b11111111;
        CreationResult result = creator.create(TestUtils.toBytes(b1, b2, b3));

        ColorDataFrame[] bulk = result.bulk.getDataFrames();

        Assert.assertEquals(2, bulk.length);

        int[] colors1 = colorsFromBytes(new int[]{
                0b00, 0b00, 0b00, 0b00
        }, 6);
        Assert.assertArrayEquals(colors1, bulk[0].getColors());

        int[] colors2 = colorsFromBytes(new int[]{
                0b01, 0b00, 0b00, 0b00
        }, 6);
        Assert.assertArrayEquals(colors2, bulk[1].getColors());

        Assert.assertEquals(1, result.unread);

    }

    @Test
    public void testSimple_bytesLessThanInBulk() {
        ColorDataFrameCreator creator = new FourColorsDataFrameCreator(8, 3, new TestUtils.MockChecksum());

        int b1 = 0b10101101;
        CreationResult result = creator.create(TestUtils.toBytes(b1));

        ColorDataFrame[] bulk = result.bulk.getDataFrames();

        Assert.assertEquals(3, bulk.length);

        int[] colors1 = colorsFromBytes(new int[]{
                0b01, 0b11, 0b10, 0b10
        }, 8);
        Assert.assertArrayEquals(colors1, bulk[0].getColors());

        int[] colors23 = colorsFromBytes(new int[]{}, 8);
        Assert.assertArrayEquals(colors23, bulk[1].getColors());
        Assert.assertArrayEquals(colors23, bulk[2].getColors());
    }
}
