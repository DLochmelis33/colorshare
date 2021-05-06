package ru.hse.colorshare.generator.creator;

/*
    Здесь возможна дополнительная информация
 */

public class BulkColorDataFrames {
    private final ColorDataFrame[] bulk;

    public BulkColorDataFrames(ColorDataFrame[] bulk) {
        this.bulk = bulk;
    }

    public ColorDataFrame[] getDataFrames() {
        return bulk;
    }
}
