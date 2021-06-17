package ru.hse.colorshare.receiver.util;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SlidingAverage {

    public SlidingAverage(int windowSize) {
        this.windowSize = windowSize;
    }

    private final long windowSize;
    private final ConcurrentLinkedQueue<Long> values = new ConcurrentLinkedQueue<>();
    private double sum = 0;

    public void addValue(long v) {
        values.add(v);
        sum += v;
        if (values.size() >= windowSize) {
            Long old = values.poll();
            if (old == null) {
                throw new IllegalStateException("smth is badly broken");
            }
            sum -= old;
        }
    }

    public double getAverage() {
        if(values.size() < windowSize - 1) {
            return 0;
        }
        return sum / windowSize;
    }

    public double getSum() {
        if(values.size() < windowSize - 1) {
            return 0;
        }
        return sum;
    }

}
