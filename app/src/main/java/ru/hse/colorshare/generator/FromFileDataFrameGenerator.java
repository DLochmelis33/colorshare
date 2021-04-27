package ru.hse.colorshare.generator;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.Checksum;

import ru.hse.colorshare.frames.BulkColorDataFrames;
import ru.hse.colorshare.frames.ColorDataFrame;
import ru.hse.colorshare.frames.TwoBitsColorDataFrame;

public class FromFileDataFrameGenerator extends AbstractDataFrameGenerator {

    private final RandomAccessFile file;
    private final Checksum checksum;

    private final byte[] frame;
    private final int bulkSize;

    public FromFileDataFrameGenerator(RandomAccessFile file, Checksum checksum, int bytesPerFrame, int bulkSize) {
        this.file = file;
        this.checksum = checksum;
        this.frame = new byte[bytesPerFrame];
        this.bulkSize = bulkSize;
    }

    private long leftToRead() throws GenerationException {
        try {
            return file.length() - file.getFilePointer();
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    @Override
    protected void processFurther() throws GenerationException {
        ColorDataFrame[] bulk = new ColorDataFrame[bulkSize];
        for (int i = 0; i < bulkSize; i++) {
            int read = (int) Math.min(leftToRead(), frame.length - 1);
            if (read == 0)
                break;
            frame[0] = (byte) i;
            try {
                file.readFully(frame, 1, read);
            } catch (IOException e) {
                throw new GenerationException(e);
            }
            checksum.update(frame, 0, read + 1);
            bulk[i] = new TwoBitsColorDataFrame(frame, 0, read + 1, checksum.getValue());
            checksum.reset();
        }
        previous = new BulkColorDataFrames(bulk);
    }

    @Override
    protected boolean hasMore() throws GenerationException {
        return leftToRead() > 0;
    }

    @Override
    public long estimateSize() throws GenerationException {
        return leftToRead();
    }

    @Override
    public String getInfo() {
        return null;
    }
}
