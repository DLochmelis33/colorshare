package ru.hse.colorshare.coding;

import ru.hse.colorshare.util.Supplier;

/*
    Interface describes Data frame generator.
    Usage:
        - get @return next data frame
            - Calling get before setSuccess will raise IllegalStateException. get can be called just after creating
        - if transmitting succeed - setSuccess(true), otherwise setSuccess(false)
            - Calling setSuccess after setSuccess will raise IllegalStateException
        - When there is no more data to transmit get returns NULL.
 */

public interface DataFrameSupplier extends Supplier<ColorDataFrame> {
    void setSuccess(boolean success);

    long estimateSize();

    int getFrameIndex();

    String getInfo();
}
