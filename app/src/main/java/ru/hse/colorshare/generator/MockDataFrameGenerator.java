package ru.hse.colorshare.generator;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MockDataFrameGenerator implements DataFrameGenerator {
    private final int id;
    private final int frameSize; // in units
    private final int framesNumber;
    private final Random random;
    private int currentFrameIndex;

    public MockDataFrameGenerator(int id, int frameSize, int framesNumber) {
        this.id = id;
        this.frameSize = frameSize;
        this.framesNumber = framesNumber;
        random = new Random();
        currentFrameIndex = 0;
    }

    @Override
    public List<Integer> getNextDataFrame() {
        if (currentFrameIndex == framesNumber) {
            return null;
        }
        List<Integer> dataFrame = new ArrayList<>();
        for (int i = 0; i < frameSize; ++i) {
            dataFrame.add(Color.rgb(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)));
        }
        return dataFrame;
    }

    @Override
    public void setSuccess(boolean result) {
        if (result) {
            currentFrameIndex++;
        }
    }

    @Override
    public long estimateSize() {
        return framesNumber - currentFrameIndex;
    }

    public int getFrameIndex() {
        return currentFrameIndex;
    }

    @Override
    public String getInfo() {
        return "MockDataFrameGenerator; " + "id = " + id +
                "; frameSize = " + frameSize + "; framesNumber = " + framesNumber + "; frames left = " + estimateSize();
    }
}
