package ru.hse.colorshare.coding.decoding.colorChooser;

import android.graphics.Color;

import org.junit.Test;

import java.util.Random;

import ru.hse.colorshare.coding.decoding.impl.ColorMetrics;
import ru.hse.colorshare.coding.decoding.impl.FourColorsMetricsBasedColorChooser;
import ru.hse.colorshare.coding.decoding.impl.OptimalColorChooser;
import ru.hse.colorshare.coding.util.FourColorsDataFrameUtil;

public abstract class AbstractFourColorChooserTest extends AbstractColorChooserTest {

    protected abstract ColorMetrics metrics();

    @Override
    protected OptimalColorChooser create() {
        return new FourColorsMetricsBasedColorChooser(metrics());
    }

    protected void testForAllColors(int dred, int dgreen, int dblue) {
        for (FourColorsDataFrameUtil.FourColorUnit unit : FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS) {
            test(unit.color, dred, dgreen, dblue);
        }
    }

    @Test
    public void testSimple() {
        testForAllColors(0, 0, 0);
    }

    @Test
    public void testChanged() {
        testForAllColors(10, 0, 0);
        testForAllColors(0, 10, 0);
        testForAllColors(0, 0, 10);
    }

    protected abstract int maxAbs();

    @Test
    public void testStress() {
        Random r = new Random();
        final int iterations = 10000;
        final int abs = maxAbs();
        for (int i = 0; i < iterations; i++) {
            testForAllColors(abs - r.nextInt(2 * abs),
                    abs - r.nextInt(2 * abs),
                    abs - r.nextInt(2 * abs));
        }
    }

}
