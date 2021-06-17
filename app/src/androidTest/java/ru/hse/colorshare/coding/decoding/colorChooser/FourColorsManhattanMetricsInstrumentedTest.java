package ru.hse.colorshare.coding.decoding.colorChooser;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import ru.hse.colorshare.coding.decoding.impl.ColorMetrics;
import ru.hse.colorshare.coding.decoding.impl.ManhattanMetrics;


@RunWith(AndroidJUnit4.class)
public class FourColorsManhattanMetricsInstrumentedTest extends AbstractFourColorChooserTest {
    @Override
    protected ColorMetrics metrics() {
        return new ManhattanMetrics();
    }

    @Override
    protected int maxAbs() {
        return 120;
    }
}
