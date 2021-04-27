package ru.hse.colorshare.generator;

import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.util.zip.CRC32;

import ru.hse.colorshare.transmitter.TransmissionParams;

// TODO
public final class DataFrameGeneratorFactory {

    public DataFrameGeneratorFactory(Uri sourceFile, Context context) throws FileNotFoundException {
    }

    public DataFrameGenerator getDataFrameGenerator() { return null; }

    public void setParams(TransmissionParams params) {}

    public DataFrameGenerator get(Uri sourceFile, Context context) {
        return null;
    }

    public DataFrameGenerator get(byte[] source, int colorsPerFrame) {
        return null ;// new FromByteArrayDataFrameGenerator(source, new CRC32(), colorsPerFrame / 4);
    }
}
