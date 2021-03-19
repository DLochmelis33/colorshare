package ru.hse.colorshare.coding;

import java.io.InputStream;

import ru.hse.colorshare.coding.algorithms.hamming.HammingEncoder;
import ru.hse.colorshare.util.Generator;

public class DataFrameGeneratorFactory {
    Generator<DataFrame> getFromStreamGenerator(InputStream stream) {
        return new ColorFrameGenerator(stream, new HammingEncoder(), ThreeBitColorFrame::ofBits);
    }
}
