package ru.hse.colorshare.coding.decoding.impl;

import android.graphics.Color;

public class ManhattanMetrics implements ColorMetrics {

    @Override
    public int distance(int color1, int color2) {
        return Math.abs(Color.red(color1) - Color.red(color2)) +
                Math.abs(Color.green(color1) - Color.green(color2)) +
                Math.abs(Color.blue(color1) - Color.blue(color2));
    }
}
