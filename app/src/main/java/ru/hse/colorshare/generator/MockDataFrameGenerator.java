package ru.hse.colorshare.generator;

import android.graphics.Color;

import java.util.Random;

import ru.hse.colorshare.frames.ColorDataFrame;

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

    public class MockDataFrame implements ColorDataFrame {
        private final int[] colors;

        public MockDataFrame(int[] colors) {
            this.colors = colors;
        }

        @Override
        public long getChecksum() {
            return 0;
        }

        @Override
        public int[] getChecksumAsColors() {
            return new int[0];
        }

        @Override
        public int[] getColors() {
            return colors;
        }
    }

    @Override
    public ColorDataFrame getNextDataFrame() {
        if (currentFrameIndex == framesNumber) {
            return null;
        }
        int[] dataFrame = new int[frameSize];
        for (int i = 0; i < frameSize; ++i) {
            dataFrame[i] = (Color.rgb(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)));
        }
        return new MockDataFrame(dataFrame);
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
