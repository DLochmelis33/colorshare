package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import ru.hse.colorshare.frames.ColorDataFrame;
import ru.hse.colorshare.generator.DataFrameGenerator;
import ru.hse.colorshare.generator.FromByteArrayDataFrameGenerator;

public class ColorDataFrameSupplierTest {

    private static class MockChecksum implements Checksum {
        long value = 0;

        @Override
        public void update(int b) {

        }

        @Override
        public void update(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) {
                value += b[off + i];
            }
        }

        @Override
        public long getValue() {
            return value;
        }

        @Override
        public void reset() {
            value = 0;
        }
    }

    @Test
    public void fromByteArrayDataFrameSupplierTest() {
        byte[] bytes = TestUtils.toBytes(0b0011_1100, 0b1010_1010);
        DataFrameGenerator supplier1 = new FromByteArrayDataFrameGenerator(bytes, new CRC32(), 2);
        DataFrameGenerator supplier2 = new FromByteArrayDataFrameGenerator(bytes, new CRC32(), 3);
        int[] colors1 = TestUtils.colorsFromBytes(new int[] {
                0b00, 0b11, 0b11, 0b00,
                0b10, 0b10, 0b10, 0b10
        });

        ColorDataFrame frame1 = supplier1.getNextDataFrame();
        ColorDataFrame frame2 = supplier2.getNextDataFrame();

        Assert.assertNotNull(frame1);
        Assert.assertNotNull(frame2);

        Assert.assertArrayEquals(colors1, frame1.getColors());
        Assert.assertArrayEquals(colors1, frame2.getColors());

        Assert.assertEquals(frame1.getChecksum(), frame2.getChecksum());

        Assert.assertThrows(IllegalStateException.class, supplier1::getNextDataFrame);
        Assert.assertThrows(IllegalStateException.class, supplier2::getNextDataFrame);

        supplier1.setSuccess(false);
        supplier2.setSuccess(false);

        Assert.assertArrayEquals(colors1, supplier1.getNextDataFrame().getColors());
        Assert.assertArrayEquals(colors1, supplier2.getNextDataFrame().getColors());

        supplier1.setSuccess(true);
        supplier2.setSuccess(true);

        Assert.assertNull(supplier1.getNextDataFrame());
        Assert.assertNull(supplier2.getNextDataFrame());
    }

    @Test
    public void checkSumAsColorsTest() {
        byte[] bytes = TestUtils.toBytes(0b0000_0000, 0b1111_0000);
        DataFrameGenerator supplier = new FromByteArrayDataFrameGenerator(bytes, new MockChecksum(), 2);
        ColorDataFrame frame = supplier.getNextDataFrame();
        Assert.assertNotNull(frame);
        Assert.assertArrayEquals(
                TestUtils.colorsFromBytes(new int[] {
                        0b00, 0b00, 0b00, 0b00,
                        0b00, 0b00, 0b11, 0b11
                }), frame.getColors()
        );
        Assert.assertEquals((byte)0b1111_0000, frame.getChecksum());

        // Long.toBinaryString(-16) = 11111...11110000

        Assert.assertArrayEquals(
                TestUtils.colorsFromBytes(new int[] {
                        0b00, 0b00, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11,
                        0b11, 0b11, 0b11, 0b11
                }), frame.getChecksumAsColors()
        );

    }
}
