package ru.hse.colorshare.coding;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.hse.colorshare.generator.preprocessor.FileRawDataPreprocessor;
import ru.hse.colorshare.generator.preprocessor.RawDataPreprocessor;

public class FileRawDataPreprocessorTest {
    private final Path resourceDirectory = Paths.get("src", "test", "resources");

    private RawDataPreprocessor getProcessor(String filename, long fileLength) throws FileNotFoundException {
        InputStream file = new FileInputStream(resourceDirectory.resolve("testSimple").toFile());
        return new FileRawDataPreprocessor(file, fileLength, 10);
    }

    @Test
    public void testSimple() throws Exception {
        RawDataPreprocessor preprocessor = getProcessor("testSimple", 3);
        Assert.assertEquals(3, preprocessor.left());

        byte[] actual;

        actual = new byte[]{'a', 'b'};
        byte[] buffer1 = new byte[2];
        preprocessor.getBytes(buffer1);
        Assert.assertArrayEquals(actual, buffer1);

        preprocessor.returnBytes(1);

        actual = new byte[]{'b', 'c'};
        byte[] buffer2 = new byte[2];
        preprocessor.getBytes(buffer2);
        Assert.assertArrayEquals(actual, buffer2);
    }
}
