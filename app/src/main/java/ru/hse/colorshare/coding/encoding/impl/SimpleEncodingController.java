package ru.hse.colorshare.coding.encoding.impl;

import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import ru.hse.colorshare.coding.encoding.ColorDataFrame;
import ru.hse.colorshare.coding.encoding.DataFrameBulk;
import ru.hse.colorshare.coding.encoding.DataFrameEncoder;
import ru.hse.colorshare.coding.encoding.EncodingController;
import ru.hse.colorshare.coding.exceptions.EncodingException;
import ru.hse.colorshare.coding.encoding.EncodingPreprocessor;
import ru.hse.colorshare.transmitter.TransmissionParams;

/*
    Можно налету подменить переводчика в цвета или предобработчика в зависимости от параметров передачи.
 */

public class SimpleEncodingController implements EncodingController {
    private int unitsPerFrame;
    private int framesPerBulk;

    private int currentBulk = 0;

    private final InputStream stream;

    private EncodingPreprocessor preprocessor;
    private DataFrameEncoder colorEncoder;

    private final ByteBuffer buffer;

    public SimpleEncodingController(InputStream stream, long streamLength) {
        this.stream = stream;
        buffer = ByteBuffer.allocate(4096);
        preprocessor = new SimpleEncodingPreprocessor(stream, streamLength);
    }

    public SimpleEncodingController(InputStream stream, long fileLength, int unitsPerFrame, int framesPerBulk) {
        this(stream, fileLength);
        setTransmissionParameters(unitsPerFrame, framesPerBulk);
    }

    public SimpleEncodingController(Uri uri, Context context) throws FileNotFoundException {
        this(
                context.getContentResolver().openInputStream(uri),
                context.getContentResolver().openFileDescriptor(uri, "r").getStatSize()
        );
    }

    public SimpleEncodingController(Uri uri, Context context, int unitsPerFrame, int framesPerBulk) throws FileNotFoundException {
        this(uri, context);
        setTransmissionParameters(unitsPerFrame, framesPerBulk);
    }

    private void changeCreator() {
        colorEncoder = new FourColorsDataFrameEncoder(unitsPerFrame, new CRC32());
    }

    @Override
    public DataFrameBulk getNextBulk() throws EncodingException {
        try {
            if (preprocessor.left() <= 0) {
                return null;
            }
        } catch (IOException e) {
            throw new EncodingException(e);
        }

        try {
            preprocessor.readBytes(buffer, colorEncoder.estimateBufferSize() * framesPerBulk);
        } catch (IOException e) {
            throw new EncodingException(e);
        }

        buffer.flip();
        ColorDataFrame[] bulk = new ColorDataFrame[framesPerBulk];
        for (int inBulk = 0; inBulk < framesPerBulk; inBulk++) {
            bulk[inBulk] = colorEncoder.encode(buffer);
        }
        currentBulk++;
        buffer.compact();
        return new DataFrameBulk(bulk);
    }

    @Override
    public int getBulkIndex() {
        return currentBulk;
    }

    @Override
    public long estimateSize() throws EncodingException {
        try {
            return preprocessor.left();
        } catch (IOException e) {
            throw new EncodingException(e);
        }
    }

    @Override
    public String getInfo() {
        return "FileDataFrameGenerator{" +
                "unitsPerFrame=" + unitsPerFrame +
                ", framesPerBulk=" + framesPerBulk +
                ", preprocessor=" + preprocessor +
                ", creator=" + colorEncoder +
                '}';
    }

    @Override
    public void setTransmissionParameters(int unitsPerFrame, int framesPerBulk) {
        this.unitsPerFrame = unitsPerFrame;
        this.framesPerBulk = framesPerBulk;
        changeCreator();
    }

    @Override
    public void setTransmissionParameters(TransmissionParams params) {
        int framesPerBulk = 10;
        setTransmissionParameters(params.getColorFrameSize(), framesPerBulk);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
