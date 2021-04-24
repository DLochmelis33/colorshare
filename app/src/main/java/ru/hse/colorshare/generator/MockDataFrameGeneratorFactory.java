package ru.hse.colorshare.generator;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

import ru.hse.colorshare.transmitter.TransmissionParams;

public class MockDataFrameGeneratorFactory {
    private final Reader reader;
    private TransmissionParams params = null;
    private DataFrameGenerator generator = null;
    private int generatorId = 0;

    public MockDataFrameGeneratorFactory(Uri fileToSendUri, Context context) throws FileNotFoundException {
        InputStream inputStream =
                context.getContentResolver().openInputStream(fileToSendUri);
        Objects.requireNonNull(inputStream);
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public void finish() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    public void setParams(TransmissionParams params) {
        this.params = params;
    }

    public DataFrameGenerator getDataFrameGenerator() throws IllegalArgumentException {
        if (params == null) {
            throw new IllegalStateException("params are not set");
        }
        if (generator == null) {
            generatorId++;
            generator = new MockDataFrameGenerator(generatorId, params.getColorFrameSize(), 10);
        }
        return generator;
    }
}
