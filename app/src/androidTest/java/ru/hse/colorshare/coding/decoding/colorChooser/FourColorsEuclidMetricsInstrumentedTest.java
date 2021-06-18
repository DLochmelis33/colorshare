package ru.hse.colorshare.coding.decoding.colorChooser;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import ru.hse.colorshare.coding.decoding.impl.ColorMetrics;
import ru.hse.colorshare.coding.decoding.impl.EuclidMetrics;


@RunWith(AndroidJUnit4.class)
public class FourColorsEuclidMetricsInstrumentedTest extends AbstractFourColorChooserTest {
    @Override
    protected ColorMetrics metrics() {
        return new EuclidMetrics();
    }

    @Override
    protected int maxAbs() {
        return 120;
    }
}
