package ru.hse.colorshare.coding.decoding.impl;

import android.graphics.Color;

import static ru.hse.colorshare.coding.util.FourColorsDataFrameUtil.ALL_TWO_BIT_UNITS;

public class FourColorManhattanMetricsColorChooser implements OptimalColorChooser {

    private int distance(int color1, int color2) {
        return Math.abs(Color.red(color1) - Color.red(color2)) +
                Math.abs(Color.green(color1) - Color.green(color2)) +
                Math.abs(Color.blue(color1) - Color.blue(color2));
    }

    @Override
    public int chooseClosest(int color) {
        int currentOptimalIndex = 0;
        int currentOptimalDistance = Integer.MAX_VALUE;
        for (int i = 0; i < ALL_TWO_BIT_UNITS.size(); i++) {
            int currentDistance = distance(color, ALL_TWO_BIT_UNITS.get(i).color);
            if (currentDistance < currentOptimalDistance) {
                currentOptimalIndex = i;
                currentOptimalDistance = currentDistance;
            }
        }
        return ALL_TWO_BIT_UNITS.get(currentOptimalIndex).color;
    }
}
