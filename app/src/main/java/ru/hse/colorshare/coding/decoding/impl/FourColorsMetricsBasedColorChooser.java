package ru.hse.colorshare.coding.decoding.impl;


import ru.hse.colorshare.coding.util.FourColorsDataFrameUtil;

import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS;

public class FourColorsMetricsBasedColorChooser implements OptimalColorChooser {

    private final ColorMetrics metrics;

    public FourColorsMetricsBasedColorChooser(ColorMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public int chooseClosest(int color) {
        int currentOptimalIndex = 0;
        int currentOptimalDistance = Integer.MAX_VALUE;
        for (int i = 0; i < ALL_TWO_BIT_UNITS.size(); i++) {
            int currentDistance = metrics.distance(color, ALL_TWO_BIT_UNITS.get(i).color);
            if (currentDistance <= currentOptimalDistance) {
                currentOptimalIndex = i;
                currentOptimalDistance = currentDistance;
            }
        }
        if (metrics.distance(color, FourColorsDataFrameUtil.EMPTY_COLOR) < currentOptimalDistance) {
            return FourColorsDataFrameUtil.EMPTY_COLOR;
        }
        return ALL_TWO_BIT_UNITS.get(currentOptimalIndex).color;
    }
}
