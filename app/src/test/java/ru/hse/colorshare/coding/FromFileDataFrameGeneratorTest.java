package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.hse.colorshare.generator.creator.ColorDataFrame;
import ru.hse.colorshare.generator.DataFrameGenerator;
import ru.hse.colorshare.generator.FileDataFrameGenerator;
import ru.hse.colorshare.generator.GenerationException;

import static ru.hse.colorshare.coding.TestUtils.colorsFromBytes;

public class FromFileDataFrameGeneratorTest {
    Path resourceDirectory = Paths.get("src", "test", "resources");

    @Test
    public void testSimple() throws IOException, GenerationException {
        InputStream file = new FileInputStream(resourceDirectory.resolve("testSimple").toFile());
        DataFrameGenerator generator = new FileDataFrameGenerator(file, 3, 10, 2);
        ColorDataFrame[] bulk = generator.getNextBulk().getDataFrames();
        Assert.assertEquals(2, bulk.length);
        // 97 = 01100001
        // 98 = 01100010
        // 99 = 01100011
        int[] colors1 = colorsFromBytes(new int[] {
                0b01, 0b00, 0b10, 0b01,
                0b10, 0b00, 0b10, 0b01
        }, 10);
        int[] colors2 = colorsFromBytes(new int[] {
                0b11, 0b00, 0b10, 0b01
        }, 10);

        Assert.assertArrayEquals(colors1, bulk[0].getColors());
        Assert.assertArrayEquals(colors2, bulk[1].getColors());

        Assert.assertThrows(IllegalStateException.class, generator::getNextBulk);
        generator.setSuccess(true);

        Assert.assertNull(generator.getNextBulk());

        generator.close();
    }

}
