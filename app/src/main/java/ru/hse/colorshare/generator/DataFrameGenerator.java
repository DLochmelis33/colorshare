package ru.hse.colorshare.generator;

import ru.hse.colorshare.frames.BulkColorDataFrames;

/*
    Interface describes Data frame generator.
    Usage:
        - get @return next data frame
            - Calling get before setSuccess will raise IllegalStateException. get can be called just after creating
        - if transmitting succeed - setSuccess(true), otherwise setSuccess(false)
            - Calling setSuccess after setSuccess will raise IllegalStateException
        - When there is no more data to transmit get returns NULL.
 */

public interface DataFrameGenerator {

    // get next data frame
    BulkColorDataFrames getNextBulk() throws GenerationException;

    // set success on last bulk sending
    void setSuccess(boolean result) throws GenerationException;

    // @return approximate count of bulks left
    long estimateSize() throws GenerationException;

    // @return index of a current frame
    int getBulkIndex();

    // Some logging information. Maybe we'll include some logging library
    String getInfo();
}
