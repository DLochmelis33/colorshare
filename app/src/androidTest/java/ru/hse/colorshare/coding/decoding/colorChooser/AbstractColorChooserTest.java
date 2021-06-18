package ru.hse.colorshare.coding.decoding.colorChooser;

import android.graphics.Color;

import org.junit.Assert;
import org.junit.Before;

import ru.hse.colorshare.coding.decoding.impl.OptimalColorChooser;

public abstract class AbstractColorChooserTest {
    protected OptimalColorChooser chooser;

    protected abstract OptimalColorChooser create();

    @Before
    public void init() {
        chooser = create();
    }

    int sumComponents(int origin, int dcomp) {
        return Math.max(0, Math.min(255, origin + dcomp));
    }

    protected int changeColor(int origin, int dred, int dgreen, int dblue) {
        return Color.rgb(sumComponents(Color.red(origin), dred), sumComponents(Color.green(origin), dgreen), sumComponents(Color.blue(origin), dblue));
    }

    protected void test(int origin, int dred, int dgreen, int dblue) {
        Assert.assertEquals(origin, chooser.chooseClosest(changeColor(origin, dred, dgreen, dblue)));
    }
}
