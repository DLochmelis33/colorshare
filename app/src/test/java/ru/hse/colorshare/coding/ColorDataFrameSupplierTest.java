package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import ru.hse.colorshare.frames.BulkColorDataFrames;
import ru.hse.colorshare.frames.ColorDataFrame;
import ru.hse.colorshare.generator.DataFrameGenerator;
import ru.hse.colorshare.generator.FromByteArrayDataFrameGenerator;
import ru.hse.colorshare.generator.GenerationException;

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
    public void fromByteArrayDataFrameSupplierTest() throws GenerationException {
        byte[] bytes = TestUtils.toBytes(0b0011_1100, 0b1010_1010);
        DataFrameGenerator supplier1 = new FromByteArrayDataFrameGenerator(bytes, new CRC32(), 2, 1);
        DataFrameGenerator supplier2 = new FromByteArrayDataFrameGenerator(bytes, new CRC32(), 3, 1);
        int[] colors1 = TestUtils.colorsFromBytes(new int[] {
                0b00, 0b11, 0b11, 0b00,
                0b10, 0b10, 0b10, 0b10
        });

        BulkColorDataFrames frame1 = supplier1.getNextBulk();
        BulkColorDataFrames frame2 = supplier2.getNextBulk();

        Assert.assertNotNull(frame1);
        Assert.assertNotNull(frame2);

        ColorDataFrame[] frames1 = frame1.getDataFrames();
        ColorDataFrame[] frames2 = frame2.getDataFrames();

        Assert.assertEquals(1, frames1.length);
        Assert.assertEquals(1, frames2.length);

        Assert.assertArrayEquals(colors1, frames1[0].getColors());
        Assert.assertArrayEquals(colors1, frames2[0].getColors());

        Assert.assertEquals(frames1[0].getChecksum(), frames2[0].getChecksum());

        Assert.assertThrows(IllegalStateException.class, supplier1::getNextBulk);
        Assert.assertThrows(IllegalStateException.class, supplier2::getNextBulk);

        supplier1.setSuccess(false);
        supplier2.setSuccess(false);

        Assert.assertArrayEquals(colors1, supplier1.getNextBulk().getDataFrames()[0].getColors());
        Assert.assertArrayEquals(colors1, supplier2.getNextBulk().getDataFrames()[0].getColors());

        supplier1.setSuccess(true);
        supplier2.setSuccess(true);

        Assert.assertNull(supplier1.getNextBulk());
        Assert.assertNull(supplier2.getNextBulk());
    }

    @Test
    public void checkSumAsColorsTest() throws GenerationException {
        byte[] bytes = TestUtils.toBytes(0b0000_0000, 0b1111_0000);
        DataFrameGenerator supplier = new FromByteArrayDataFrameGenerator(bytes, new MockChecksum(), 2, 1);
        BulkColorDataFrames frame = supplier.getNextBulk();
        Assert.assertNotNull(frame);
        Assert.assertArrayEquals(
                TestUtils.colorsFromBytes(new int[] {
                        0b00, 0b00, 0b00, 0b00,
                        0b00, 0b00, 0b11, 0b11
                }), frame.getDataFrames()[0].getColors()
        );
        Assert.assertEquals((byte)0b1111_0000, frame.getDataFrames()[0].getChecksum());

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
                }), frame.getDataFrames()[0].getChecksumAsColors()
        );

    }
}
