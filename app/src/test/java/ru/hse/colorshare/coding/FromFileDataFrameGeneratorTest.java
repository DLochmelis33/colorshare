package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

import ru.hse.colorshare.frames.ColorDataFrame;
import ru.hse.colorshare.generator.DataFrameGenerator;
import ru.hse.colorshare.generator.FromFileDataFrameGenerator;
import ru.hse.colorshare.generator.GenerationException;

public class FromFileDataFrameGeneratorTest {
    Path resourceDirectory = Paths.get("src", "test", "resources");

    @Test
    public void testSimple() throws IOException, GenerationException {
        RandomAccessFile file = new RandomAccessFile(resourceDirectory.resolve("testSimple").toFile(), "r");
        DataFrameGenerator generator = new FromFileDataFrameGenerator(file, new CRC32(), 10, 2);
        ColorDataFrame[] bulk = generator.getNextBulk().getDataFrames();
        Assert.assertEquals(2, bulk.length);
        // 97 = 01100001
        // 98 = 01100010
        // 99 = 01100011
        int[] colors = TestUtils.colorsFromBytes(new int[]{
                        0b00, 0b00, 0b00, 0b00,
                        0b01, 0b00, 0b10, 0b01,
                        0b10, 0b00, 0b10, 0b01,
                        0b11, 0b00, 0b10, 0b01
                });
        Assert.assertArrayEquals(colors, bulk[0].getColors());
    }
}
