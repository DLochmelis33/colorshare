package ru.hse.colorshare.receiver;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.util.RelativePoint;

public class ColorExtractor {

    public static class LocatorResult {
        public int x, y, unit;

        public LocatorResult(int x, int y, int unit) {
            this.x = x;
            this.y = y;
            this.unit = unit;
        }

        @NonNull
        @Override
        public String toString() {
            return "{x=" + x + " y=" + y + " unit=" + unit + "}";
        }
    }

    public static class BitmapBinaryWrapper {
        private final Bitmap delegate;

        public BitmapBinaryWrapper(Bitmap delegate) {
            if (delegate == null) {
                throw new IllegalArgumentException("delegate bitmap cannot be null");
            }
            this.delegate = delegate;
            Log.d("BBW", "width=" + delegate.getWidth() + " height=" + delegate.getHeight());
        }

        // false for white, true for black
        public boolean get(int x, int y) {
            int real = delegate.getPixel(x, y);
//        Log.d("BBW", "r=" + Color.red(real) + " g=" + Color.green(real) + " b=" + Color.blue(real));
            return Color.red(real) + Color.green(real) + Color.blue(real) <= 128 * 3;
        }

        public int getWidth() {
            return delegate.getWidth();
        }

        public int getHeight() {
            return delegate.getHeight();
        }
    }

    // all variables are subject to change
    private final static int prevCenterCheckingEps = 10;
    private final static int prevCenterUnitEps = 3;

    private final static int checkingEps = 15;
    private final static int minUnit = 3;
    private final static int maxUnit = 100; // ! probably too much

    private final static int iSkip = 1;
    private final static int jSkip = 1;

    @Nullable
    public static LocatorResult findLocator(BitmapBinaryWrapper img, Point hint, Point prevCenter, int prevCenterUnit) {
        if (prevCenter != null) {
            int pcx = prevCenter.x;
            int pcy = prevCenter.y;
            for (int i = Math.max(0, pcx - prevCenterCheckingEps); i < Math.min(img.getWidth(), pcx + prevCenterCheckingEps); i += iSkip) {
                for (int j = Math.max(0, pcy - prevCenterCheckingEps); j < Math.min(img.getHeight(), pcy + prevCenterCheckingEps); j += jSkip) {
                    for (int unit = Math.max(1, prevCenterUnit - prevCenterUnitEps); unit < prevCenterUnit + prevCenterUnitEps; unit++) {
                        if (checkCenter(img, i, j, unit)) {
                            return specifyCenter(img, i, j);
                        }
                    }
                }
            }
        }
        return findLocator(img, hint);
    }

    /**
     * @param hint approximate UL corner of locator
     * @return center of found locator or null if not found
     */
    @Nullable
    public static LocatorResult findLocator(BitmapBinaryWrapper img, @NonNull Point hint) {
        for (int unit = minUnit; unit < maxUnit; unit++) {
            int tcx = hint.x + (int) (2.5 * unit); // theoreticalCenterX
            int tcy = hint.y + (int) (2.5 * unit); // theoreticalCenterY
            for (int i = Math.max(0, tcx - checkingEps); i < Math.min(img.getWidth(), tcx + checkingEps); i += iSkip) {
                for (int j = Math.max(0, tcy - checkingEps); j < Math.min(img.getHeight(), tcy + checkingEps); j += jSkip) {
                    if (checkCenter(img, i, j, unit)) {
                        return specifyCenter(img, i, j);
                    }
                }
            }
        }
        return null;
    }

    private static class Check {
        int dx, dy;
        boolean result;

        public Check(int dx, int dy, boolean result) {
            this.dx = dx;
            this.dy = dy;
            this.result = result;
        }
    }

    private final static List<Check> centerChecks = Arrays.asList(
            new Check(0, 0, true), // center black area
            new Check(1, -1, false), // white area
            new Check(1, 0, false),
            new Check(1, 1, false),
            new Check(-1, -1, false),
            new Check(-1, 0, false),
            new Check(-1, 1, false),
            new Check(0, -1, false),
            new Check(0, 1, false),
            new Check(2, -2, true), // outer black area
            new Check(2, 0, true),
            new Check(2, 2, true),
            new Check(-2, -2, true),
            new Check(-2, 0, true),
            new Check(-2, 2, true),
            new Check(0, -2, true),
            new Check(0, 2, true)
    );

    private static boolean checkCenter(BitmapBinaryWrapper img, int cx, int cy, int unit) {
        for (Check check : centerChecks) {
            int x = cx + check.dx * unit;
            int y = cy + check.dy * unit;
            if (!(0 <= x && x < img.getWidth() && 0 <= y && y < img.getHeight())) {
                return false;
            }
            if (img.get(x, y) != check.result) {
                return false;
            }
        }
        return true;
    }

    private static int round(double x) {
        return (int) Math.round(x);
    }

    private static double average(double... values) {
        double sum = 0;
        for (double x : values) {
            sum += x;
        }
        return sum / values.length;
    }

    // acx(y) = approximate center x(y)
    private static LocatorResult specifyCenter(BitmapBinaryWrapper img, int acx, int acy) {
        int top = acy;
        while (top >= 0 && img.get(acx, top)) {
            top--;
        }
        int left = acx;
        while (left >= 0 && img.get(left, acy)) {
            left--;
        }
        int bottom = acy;
        while (bottom < img.getHeight() && img.get(acx, bottom)) {
            bottom++;
        }
        int right = acx;
        while (right < img.getWidth() && img.get(right, acy)) {
            right++;
        }
        return new LocatorResult(round(average(right, left)), round(average(top, bottom)), round(((right - left) + (bottom - top) - 2.5) / 2.0));
    }

    private static final int maxUnitDifference = 4;

    @Nullable
    public static LocatorResult[] findLocators(Bitmap img, RelativePoint[] hints) {
        if (BuildConfig.DEBUG && hints.length != 4) {
            throw new AssertionError("hints.length == " + hints.length + " != 4");
        }
        BitmapBinaryWrapper wrapper = new BitmapBinaryWrapper(img);
        LocatorResult[] results = new LocatorResult[4];
        for (int i = 0; i < 4; i++) {
            Point hint = new Point((int) (wrapper.getWidth() * hints[i].x), (int) (wrapper.getHeight() * hints[i].y));
            results[i] = findLocator(wrapper, hint);
            if (results[i] == null) {
                return null;
            }
        }
        return results;
    }

    private static class GridMapper {
        private final int realWidth, realHeight;
        private final Point ul, ur, dr, dl;
        private final int gridWidth, gridHeight;

        public GridMapper(int realWidth, int realHeight, Point ul, Point ur, Point dr, Point dl, int gridWidth, int gridHeight) {
            this.realWidth = realWidth;
            this.realHeight = realHeight;
            this.ul = ul;
            this.ur = ur;
            this.dr = dr;
            this.dl = dl;
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
        }

        public Point map(Point onGrid) {
            // TODO
            return null;
        }
    }

    private Color[] extractData(Bitmap img, RelativePoint[] hints) {
        LocatorResult[] locators = findLocators(img, hints);
        if (locators == null) {
            return null;
        }
        Point gridCornerUL = new Point(locators[0].x - 2 * locators[0].unit, locators[0].y - 2 * locators[0].unit);
        Point gridCornerUR = new Point(locators[1].x + 2 * locators[1].unit, locators[1].y - 2 * locators[1].unit);
        Point gridCornerDR = new Point(locators[2].x + 2 * locators[2].unit, locators[2].y + 2 * locators[2].unit);
        Point gridCornerDL = new Point(locators[3].x - 2 * locators[3].unit, locators[3].y + 2 * locators[3].unit);

        double deltaX = average(gridCornerUR.x - gridCornerUL.x, gridCornerDR.x - gridCornerDL.x);
        double deltaY = average(gridCornerUR.y - gridCornerUL.y, gridCornerDR.y - gridCornerDL.y);
        double avgUnit = 0;
        for (int i = 0; i < 4; i++) {
            avgUnit += locators[i].unit;
        }
        avgUnit /= 4;

        // TODO
        return null;
    }

}
