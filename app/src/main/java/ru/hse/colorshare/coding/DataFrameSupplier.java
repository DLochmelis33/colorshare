package ru.hse.colorshare.coding;

/*
    Interface describes Data frame generator.
    Usage:
        - get @return next data frame
            - Calling get before setSuccess will raise IllegalStateException. get can be called just after creating
        - if transmitting succeed - setSuccess(true), otherwise setSuccess(false)
            - Calling setSuccess after setSuccess will raise IllegalStateException
        - When there is no more data to transmit get returns NULL.
 */

public interface DataFrameSupplier {

    // get next data frame
    ColorDataFrame get();

    // set success on last frame sending
    void setSuccess(boolean success);

    // @return approximate count of frames left
    long estimateSize();

    // @return index of a current frame
    int getFrameIndex();

    // Some logging information. Maybe we'll include some logging library
    String getInfo();
}
