package ru.hse.colorshare.generator;

import java.util.List;

public interface DataFrameGenerator {
    List<Integer> getNextDataFrame();

    void setSuccess(boolean result);

    long estimateSize();

    int getFrameIndex();

    String getInfo();
}
