package ru.hse.colorshare.generator;

import android.content.Context;
import android.net.Uri;

import java.util.zip.CRC32;

// TODO
public final class DataFrameGeneratorFactory {

    public DataFrameGenerator get(Uri sourceFile, Context context) {
        return null;
    }

    public DataFrameGenerator get(byte[] source, int colorsPerFrame) {
        return new FromByteArrayDataFrameGenerator(source, new CRC32(), colorsPerFrame / 4);
    }
}
