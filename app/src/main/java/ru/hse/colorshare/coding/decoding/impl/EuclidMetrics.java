package ru.hse.colorshare.coding.decoding.impl;

import android.graphics.Color;

public class EuclidMetrics implements ColorMetrics {
    @Override
    public int distance(int color1, int color2) {
        int dred = Color.red(color1) - Color.red(color2);
        int dgreen = Color.green(color1) - Color.green(color2);
        int dblue = Color.blue(color1) - Color.blue(color2);
        return dred * dred + dgreen * dgreen + dblue * dblue;
    }
}
