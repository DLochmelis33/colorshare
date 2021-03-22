package ru.hse.colorshare.coding;

import ru.hse.colorshare.coding.dto.DataFrame;
import ru.hse.colorshare.util.Supplier;

public interface DataFrameSupplier extends Supplier<DataFrame> {
    void setSuccess(boolean success);

    long estimateSize();
}
