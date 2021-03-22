package ru.hse.colorshare.coding;

import android.net.Uri;

public final class DataFrameCreationParameters {
    private final Uri sourceFile;
    private final int unitsPerFrame;

    public DataFrameCreationParameters(Uri sourceFile, int unitsPerFrame) {
        this.sourceFile = sourceFile;
        this.unitsPerFrame = unitsPerFrame;
    }

    public Uri getSourceFile() {
        return sourceFile;
    }

    public int getUnitsPerFrame() {
        return unitsPerFrame;
    }
}
