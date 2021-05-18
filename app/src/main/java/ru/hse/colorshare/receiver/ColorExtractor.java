package ru.hse.colorshare.receiver;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.util.RelativePoint;

public class ColorExtractor {

    private static final String TAG = "ColorExtractor";

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
        }

        // false for white, true for black
        public boolean get(int x, int y) {
            int real = delegate.getPixel(x, y);
            return roundColorBW(real);
        }

        public int getWidth() {
            return delegate.getWidth();
        }

        public int getHeight() {
            return delegate.getHeight();
        }

        public static boolean roundColorBW(int color) {
            return Color.red(color) + Color.green(color) + Color.blue(color) <= 128 * 3;
        }
    }

    // all variables are subject to change
    private final static int PREV_CENTER_CHECKING_EPS = 10;
    private final static int PREV_CENTER_UNIT_EPS = 3;

    private final static int CHECKING_EPS = 30;
    private final static int MIN_UNIT = 50; // ! tests require <= 3
    private final static int MAX_UNIT = 100; // ! tests require >= 90

    private final static int iSkip = 5;
    private final static int jSkip = 5;

    // while UR hint is UR corner of a locator mark, DL hint is a DL corner
    public enum HintPos {
        UL, UR, DR, DL;

        public static HintPos fromIndex(int index) {
            switch (index) {
                case 0:
                    return UL;
                case 1:
                    return UR;
                case 2:
                    return DR;
                case 3:
                    return DL;
                default:
                    throw new IllegalArgumentException("invalid HintPos index " + index);
            }
        }
    }

    @Nullable
    public static LocatorResult findLocator(BitmapBinaryWrapper img, Point hint, HintPos hintPos, Point prevCenter, int prevCenterUnit) {
        if (prevCenter != null) {
            int pcx = prevCenter.x;
            int pcy = prevCenter.y;
            for (int i = Math.max(0, pcx - PREV_CENTER_CHECKING_EPS); i < Math.min(img.getWidth(), pcx + PREV_CENTER_CHECKING_EPS); i += iSkip) {
                for (int j = Math.max(0, pcy - PREV_CENTER_CHECKING_EPS); j < Math.min(img.getHeight(), pcy + PREV_CENTER_CHECKING_EPS); j += jSkip) {
                    for (int unit = Math.max(1, prevCenterUnit - PREV_CENTER_UNIT_EPS); unit < prevCenterUnit + PREV_CENTER_UNIT_EPS; unit++) {
                        if (checkCenter(img, i, j, unit)) {
                            return specifyCenter(img, i, j);
                        }
                    }
                }
            }
        }
        return findLocator(img, hint, hintPos);
    }

    /**
     * @param hint approximate corner of locator
     * @return center of found locator or null if not found
     */
    @Nullable
    public static LocatorResult findLocator(BitmapBinaryWrapper img, @NonNull Point hint, HintPos hintPos) {
        // V1: quite slow, but might be good enough
        for (int unit = MIN_UNIT; unit < MAX_UNIT; unit++) {
            int tcx = hint.x; // theoreticalCenterX
            int tcy = hint.y; // theoreticalCenterY
            switch (hintPos) {
                case UL:
                    tcx = hint.x + (int) (2.5 * unit);
                    tcy = hint.y + (int) (2.5 * unit);
                    break;
                case UR:
                    tcx = hint.x - (int) (2.5 * unit);
                    tcy = hint.y + (int) (2.5 * unit);
                    break;
                case DR:
                    tcx = hint.x - (int) (2.5 * unit);
                    tcy = hint.y - (int) (2.5 * unit);
                    break;
                case DL:
                    tcx = hint.x + (int) (2.5 * unit);
                    tcy = hint.y - (int) (2.5 * unit);
                    break;
            }
            for (int i = Math.max(0, tcx - CHECKING_EPS); i < Math.min(img.getWidth(), tcx + CHECKING_EPS); i += iSkip) {
                for (int j = Math.max(0, tcy - CHECKING_EPS); j < Math.min(img.getHeight(), tcy + CHECKING_EPS); j += jSkip) {
                    if (checkCenter(img, i, j, unit)) {
                        return specifyCenter(img, i, j);
                    }
                }
            }
        }
        Log.i(TAG, "no locator was found");
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

    private static int round(double v) {
        return (int) Math.round(v);
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
        // TODO: better idea: go up - right (left) - up and get a corner, should be better for tilted images
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

    private static LocatorResult[] lastResult;

    @Nullable
    public static LocatorResult[] findLocators(Bitmap img, RelativePoint[] hints) {
        if (BuildConfig.DEBUG && hints.length != 4) {
            throw new AssertionError("hints.length == " + hints.length + " != 4");
        }
        BitmapBinaryWrapper wrapper = new BitmapBinaryWrapper(img);
        LocatorResult[] results = new LocatorResult[4];
        for (int i = 0; i < 4; i++) {
            Point hint = new Point((int) (wrapper.getWidth() * hints[i].x), (int) (wrapper.getHeight() * hints[i].y));
            if (lastResult != null) {
                results[i] = findLocator(wrapper, hint, HintPos.fromIndex(i), new Point(lastResult[i].x, lastResult[i].y), lastResult[i].unit);
            } else {
                results[i] = findLocator(wrapper, hint, HintPos.fromIndex(i));
            }
            if (results[i] == null) {
                lastResult = null;
                return null;
            }
        }
        lastResult = results;
        return results;
    }

    private static class GridMapper {
        private final PointF ul, ur, dr, dl;
        private final int gridWidth, gridHeight;

        public GridMapper(Point ul, Point ur, Point dr, Point dl, int gridWidth, int gridHeight) {
            this.ul = new PointF(ul);
            this.ur = new PointF(ur);
            this.dr = new PointF(dr);
            this.dl = new PointF(dl);
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
        }

        private PointF calcInBetween(PointF first, PointF second, float coef) {
            return new PointF(first.x + (second.x - first.x) * coef, first.y + (second.y - first.y) * coef);
        }

        // returns a point on real image that corresponds to grid entry
        public Point map(int gx, int gy) { // gridX, gridY
            // bilinear interpolation

            float xCoef = 1.0f * gx / (gridWidth - 1);
            float yCoef = 1.0f * gy / (gridHeight - 1);
            PointF u = calcInBetween(ul, ur, xCoef);
            PointF d = calcInBetween(dl, dr, xCoef);
            PointF res = calcInBetween(u, d, yCoef);

            return new Point(round(res.x), round(res.y));
        }
    }

    // return average if values are not "too different", else return -1
    private static int deduceAverage(int... values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        int avgRounded = round(sum / 4.0);
        if (Math.abs(sum - avgRounded * values.length) > 1) {
            return -1;
        }
        return avgRounded;
    }

    // check if a point is not in a skipped region (i.e. overlaps with a locator)
    // this is determined by the protocol, locator style to be specific
    private static boolean isDataIndex(int x, int y, int width, int height) {
        if ((x <= 5 && y <= 5) || (width - x <= 6 && y <= 5) || (width - x <= 6 && height - y <= 6) || (x <= 5 && height - y <= 6)) {
            // simplified version is unreadable
            return false;
        }
        return true;
    }

    private static boolean inBounds(int x, int y, Bitmap img) {
        return x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight();
    }

    private static int sampleColor(Bitmap img, Point point, int unitSize) {
        // V1: average of neighbors
        int eps = unitSize / 4;
        int total = 5;
        int[] xOffsets = new int[]{0, eps, -eps, 0, 0};
        int[] yOffsets = new int[]{0, 0, 0, eps, -eps};
        int sumR = 0, sumB = 0, sumG = 0;
        for (int i = 0; i < total; i++) {
            int x = point.x + xOffsets[i];
            int y = point.y + yOffsets[i];
            if (!inBounds(x, y, img)) {
//                Log.d(TAG, "oob x=" + x + " y=" + y);
                continue;
            }
            int c = img.getPixel(x, y);
            sumR += Color.red(c);
            sumG += Color.green(c);
            sumB += Color.blue(c);
        }
        return Color.rgb(sumR / total, sumG / total, sumB / total);
    }


    @Nullable
    public static ArrayList<Integer> extractColors(Bitmap img, RelativePoint[] hints) {
        LocatorResult[] locators = findLocators(img, hints);
        if (locators == null) {
            return null;
        }
        // centers of units!
        Point gridCornerUL = new Point(locators[0].x - 2 * locators[0].unit, locators[0].y - 2 * locators[0].unit);
        Point gridCornerUR = new Point(locators[1].x + 2 * locators[1].unit, locators[1].y - 2 * locators[1].unit);
        Point gridCornerDR = new Point(locators[2].x + 2 * locators[2].unit, locators[2].y + 2 * locators[2].unit);
        Point gridCornerDL = new Point(locators[3].x - 2 * locators[3].unit, locators[3].y + 2 * locators[3].unit);

        double deltaX = average(gridCornerUR.x - gridCornerUL.x, gridCornerDR.x - gridCornerDL.x);
        double deltaY = average(gridCornerDR.y - gridCornerUR.y, gridCornerDL.y - gridCornerUL.y);
        double avgUnit = 0;
        for (int i = 0; i < 4; i++) {
            avgUnit += locators[i].unit;
        }
        avgUnit /= 4;

        // check if unit sizes are similar enough: grid width and height should be unequivocal
        int gridHeightL1 = (int) Math.round(1.0 * (gridCornerDL.y - gridCornerUL.y) / locators[0].unit);
        int gridHeightL2 = (int) Math.round(1.0 * (gridCornerDL.y - gridCornerUL.y) / locators[3].unit);
        int gridHeightR1 = (int) Math.round(1.0 * (gridCornerDR.y - gridCornerUR.y) / locators[1].unit);
        int gridHeightR2 = (int) Math.round(1.0 * (gridCornerDR.y - gridCornerUR.y) / locators[2].unit);
        int gridHeight = deduceAverage(gridHeightL1, gridHeightL2, gridHeightR1, gridHeightR2) + 1;
        if (gridHeight == 0) {
            // discard the image as too distorted
            Log.w(TAG, "image too distorted, cannot deduce grid height");
            return null;
        }

        int gridWidthU1 = (int) Math.round(1.0 * (gridCornerUR.x - gridCornerUL.x) / locators[0].unit);
        int gridWidthU2 = (int) Math.round(1.0 * (gridCornerUR.x - gridCornerUL.x) / locators[1].unit);
        int gridWidthD1 = (int) Math.round(1.0 * (gridCornerDR.x - gridCornerDL.x) / locators[2].unit);
        int gridWidthD2 = (int) Math.round(1.0 * (gridCornerDR.x - gridCornerDL.x) / locators[3].unit);
        int gridWidth = deduceAverage(gridWidthU1, gridWidthU2, gridWidthD1, gridWidthD2) + 1;
        if (gridWidth == 0) {
            Log.w(TAG, "image too distorted, cannot deduce grid width");
            return null;
        }

        GridMapper gridMapper = new GridMapper(
                gridCornerUL, gridCornerUR, gridCornerDR, gridCornerDL,
                gridWidth, gridHeight
        );

//        Log.d(TAG, "gw=" + gridWidth + " gh=" + gridHeight);
        ArrayList<Integer> extracted = new ArrayList<>();
        extracted.ensureCapacity(gridHeight * gridWidth - 36 * 4);
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (!isDataIndex(x, y, gridWidth, gridHeight)) {
                    continue;
                }
                Point inUnit = gridMapper.map(x, y);
                extracted.add(sampleColor(img, inUnit, round(avgUnit)));
            }
        }

        return extracted;
    }

}
