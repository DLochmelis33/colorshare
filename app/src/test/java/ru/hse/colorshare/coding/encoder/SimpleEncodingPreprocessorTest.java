package ru.hse.colorshare.coding.encoder;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.hse.colorshare.coding.encoding.impl.SimpleEncodingPreprocessor;
import ru.hse.colorshare.coding.encoding.EncodingPreprocessor;

public class SimpleEncodingPreprocessorTest {
    private final Path resourceDirectory = Paths.get("src", "test", "resources");

    private EncodingPreprocessor getProcessor(String filename, long fileLength) throws FileNotFoundException {
        InputStream file = new FileInputStream(resourceDirectory.resolve("testSimple").toFile());
        return new SimpleEncodingPreprocessor(file, fileLength);
    }

    @Test
    public void testSimple() throws Exception {
        EncodingPreprocessor preprocessor = getProcessor("testSimple", 3);
        Assert.assertEquals(3, preprocessor.left());

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        Assert.assertEquals(2, preprocessor.readBytes(buffer, 2));
        buffer.flip();
        byte[] expected1 = new byte[]{'a'};
        byte[] actual1 = new byte[1];
        buffer.get(actual1);
        Assert.assertArrayEquals(expected1, actual1);
        buffer.compact();
        preprocessor.readBytes(buffer, 1);
        buffer.flip();

        byte[] expected2 = new byte[] {'b', 'c'};
        byte[] actual2 = new byte[2];
        buffer.get(actual2);

        Assert.assertArrayEquals(expected2, actual2);
    }
}
