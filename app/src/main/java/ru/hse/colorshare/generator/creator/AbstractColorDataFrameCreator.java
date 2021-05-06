package ru.hse.colorshare.generator.creator;

import java.util.zip.Checksum;

import ru.hse.colorshare.generator.creator.ColorDataFrameCreator;

public abstract class AbstractColorDataFrameCreator implements ColorDataFrameCreator {

    protected final int unitsPerFrame;
    protected final int framesPerBulk;

    protected final Checksum checksum;

    public AbstractColorDataFrameCreator(int unitsPerFrame, int framesPerBulk, Checksum checksum) {
        this.unitsPerFrame = unitsPerFrame;
        this.framesPerBulk = framesPerBulk;
        this.checksum = checksum;
    }

}
