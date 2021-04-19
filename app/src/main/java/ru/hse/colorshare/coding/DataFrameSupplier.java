package ru.hse.colorshare.coding;

import ru.hse.colorshare.util.Supplier;

public interface DataFrameSupplier extends Supplier<ColorDataFrame> {
    void setSuccess(boolean success);

    long estimateSize();

    int getFrameIndex();

    String getInfo();
}
