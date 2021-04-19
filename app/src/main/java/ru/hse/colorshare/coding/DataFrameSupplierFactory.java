package ru.hse.colorshare.coding;

import android.content.Context;
import android.net.Uri;

import java.util.zip.CRC32;

public final class DataFrameSupplierFactory {

    public DataFrameSupplier get(Uri sourceFile, Context context) {
        return null;
    }

    public DataFrameSupplier get(byte[] source, int colorsPerFrame) {
        return new FromByteArrayDataFrameSupplier(source, new CRC32(), colorsPerFrame / 4);
    }
}
