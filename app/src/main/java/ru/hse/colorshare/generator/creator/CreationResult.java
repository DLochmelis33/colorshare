package ru.hse.colorshare.generator.creator;

public class CreationResult {
    public final BulkColorDataFrames bulk;
    public final int unread;

    public CreationResult(BulkColorDataFrames bulk, int unread) {
        this.bulk = bulk;
        this.unread = unread;
    }
}
