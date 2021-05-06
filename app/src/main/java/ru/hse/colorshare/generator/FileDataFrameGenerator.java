package ru.hse.colorshare.generator;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.CRC32;

import ru.hse.colorshare.generator.creator.CreationResult;
import ru.hse.colorshare.generator.creator.FourColorsDataFrameCreator;
import ru.hse.colorshare.generator.creator.ColorDataFrameCreator;
import ru.hse.colorshare.generator.preprocessor.FileRawDataPreprocessor;
import ru.hse.colorshare.generator.preprocessor.RawDataPreprocessor;
import ru.hse.colorshare.transmitter.TransmissionParams;

/*
    Основная логика кодирования файла.
    Можно налету подменить переводчика в цвета или предобработчика в зависимости от параметров передачи.
 */

public class FileDataFrameGenerator extends AbstractDataFrameGenerator {
    private int unitsPerFrame;
    private int framesPerBulk;

    private InputStream stream;


    private RawDataPreprocessor preprocessor;
    private ColorDataFrameCreator creator;

    private byte[] buffer;

    public FileDataFrameGenerator(InputStream stream, long streamLength) {
        this.stream = stream;
        preprocessor = new FileRawDataPreprocessor(stream, streamLength, 4096);
    }

    public FileDataFrameGenerator(InputStream stream, long fileLength, int unitsPerFrame, int framesPerBulk) {
        this(stream, fileLength);
        setTransmissionParameters(unitsPerFrame, framesPerBulk);
    }

    public FileDataFrameGenerator(Uri uri, Context context) throws FileNotFoundException {
        this(
                context.getContentResolver().openInputStream(uri),
                context.getContentResolver().openFileDescriptor(uri, "r").getStatSize()
        );
    }

    public FileDataFrameGenerator(Uri uri, Context context, int unitsPerFrame, int framesPerBulk) throws FileNotFoundException {
        this(uri, context);
        setTransmissionParameters(unitsPerFrame, framesPerBulk);
    }

    private void changeCreator() {
        creator = new FourColorsDataFrameCreator(unitsPerFrame, framesPerBulk, new CRC32());
        buffer = new byte[creator.estimateBufferSize()];
    }

    @Override
    protected void processFurther() throws GenerationException {
        if (creator == null) {
            throw new GenerationException("Transmission parameters didn't set");
        }

        CreationResult result;
        try {
            int read = preprocessor.getBytes(buffer);
            result = creator.create(buffer, 0, read);
        } catch (IOException e) {
            throw new GenerationException(e);
        }

        previous = result.bulk;

        try {
            preprocessor.returnBytes(result.unread);
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    @Override
    protected boolean hasMore() throws GenerationException {
        try {
            return preprocessor.left() > 0;
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    @Override
    public long estimateSize() throws GenerationException {
        try {
            return preprocessor.left();
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    @Override
    public String getInfo() {
        return "FileDataFrameGenerator{" +
                "unitsPerFrame=" + unitsPerFrame +
                ", framesPerBulk=" + framesPerBulk +
                ", preprocessor=" + preprocessor +
                ", creator=" + creator +
                ", buffer=" + Arrays.toString(buffer) +
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
        setTransmissionParameters(params.getColorFrameSize(), 1);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
