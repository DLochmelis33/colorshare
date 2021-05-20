package ru.hse.colorshare.receiver;

import ru.hse.colorshare.test.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import ru.hse.colorshare.receiver.ColorExtractor;
import ru.hse.colorshare.receiver.util.RelativePoint;

@RunWith(AndroidJUnit4.class)
public class ColorExtractorInstrumentedTest {

    // ! check ColorExtractor constants before testing !

    private Bitmap smallImage, normalImage, photoImage, photoCroppedImage, unitSmallerPhotoImage16x9, unitSmallerPhotoImage4x3;

    private static final String TAG = "testing";

    @Before
    public void loadImages() {
        smallImage = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_small),
                45, 60, false);
        normalImage = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_normal),
                900, 1200, false);
        photoImage = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_photo),
                2124, 2832, false);
        photoCroppedImage = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_photo_cropped),
                1422, 1881, false);
        unitSmallerPhotoImage16x9 = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_unit_smaller_16x9),
                1274, 2266, false);
        unitSmallerPhotoImage4x3 = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getContext().getResources(), R.drawable.test_image_unit_smaller_4x3),
                1274, 1699, false);
    }

    @Test
    public void testSmallLocator() {
        // too far from reality

//        RelativePoint[] hints = new RelativePoint[]{
//                new RelativePoint(0 / 45.0, 0 / 60.0),
//                new RelativePoint(44 / 45.0, 0 / 60.0),
//                new RelativePoint(44 / 45.0, 59 / 60.0),
//                new RelativePoint(0 / 45.0, 59 / 60.0)
//        };
//        ColorExtractor.LocatorResult[] results = ColorExtractor.findLocators(smallImage, hints);
//        Log.d(TAG, Arrays.toString(results));
//        Assert.assertNotNull(results);
//        ColorExtractor.LocatorResult ul = results[0];
//        Assert.assertTrue(ul.x == 7 && ul.y == 7 && ul.unit == 3);
//        ColorExtractor.LocatorResult ur = results[1];
//        Assert.assertTrue(ur.x == 37 && ur.y == 7 && ur.unit == 3);
//        ColorExtractor.LocatorResult dr = results[2];
//        Assert.assertTrue(dr.x == 37 && dr.y == 52 && dr.unit == 3);
//        ColorExtractor.LocatorResult dl = results[3];
//        Assert.assertTrue(dl.x == 7 && dl.y == 52 && dl.unit == 3);
    }

    private boolean diffEps(double value, double expected, double eps) {
        return Math.abs(value - expected) <= eps;
    }

    @Test
    public void testNormalLocator() {
        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(0, 0),
                new RelativePoint(899 / 900.0, 0),
                new RelativePoint(899 / 900.0, 1199 / 1200.0),
                new RelativePoint(0, 1199 / 1200.0)
        };
        ColorExtractor.LocatorResult[] results = ColorExtractor.findLocators(normalImage, hints);
//        Log.d(TAG, Arrays.toString(results));
        Assert.assertNotNull(results);
        ColorExtractor.LocatorResult ul = results[0];
        Assert.assertTrue(ul.x == 150 && ul.y == 150 && ul.unit == 60);
        ColorExtractor.LocatorResult ur = results[1];
        Assert.assertTrue(ur.x == 750 && ur.y == 150 && ur.unit == 60);
        ColorExtractor.LocatorResult dr = results[2];
        Assert.assertTrue(dr.x == 750 && dr.y == 1050 && dr.unit == 60);
        ColorExtractor.LocatorResult dl = results[3];
        Assert.assertTrue(dl.x == 150 && dl.y == 1050 && dl.unit == 60);
    }

    @Test
    public void testPhotoCroppedLocator() {
        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(8 / 1422.0, 11 / 1881.0),
                new RelativePoint(1402 / 1422.0, 13 / 1881.0),
                new RelativePoint(1408 / 1422.0, 1869 / 1881.0),
                new RelativePoint(15 / 1422.0, 1870 / 1881.0)
        };
        ColorExtractor.LocatorResult[] results = ColorExtractor.findLocators(photoCroppedImage, hints);
//        Log.d(TAG, Arrays.toString(results));
        Assert.assertNotNull(results);
        ColorExtractor.LocatorResult ul = results[0];
        Assert.assertTrue(diffEps(ul.x, 248, 5) && diffEps(ul.y, 240, 5) && diffEps(ul.unit, 90, 5));
        ColorExtractor.LocatorResult ur = results[1];
        Assert.assertTrue(diffEps(ur.x, 1167, 5) && diffEps(ur.y, 242, 5) && diffEps(ur.unit, 90, 5));
        ColorExtractor.LocatorResult dr = results[2];
        Assert.assertTrue(diffEps(dr.x, 1176, 5) && diffEps(dr.y, 1625, 5) && diffEps(dr.unit, 90, 5));
        ColorExtractor.LocatorResult dl = results[3];
        Assert.assertTrue(diffEps(dl.x, 246, 5) && diffEps(dl.y, 1630, 5) && diffEps(dl.unit, 90, 5));
    }

    @Test
    public void testPhotoLocator() {
        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(396 / 2124.0, 479 / 2832.0),
                new RelativePoint(1773 / 2124.0, 518 / 2832.0),
                new RelativePoint(1745 / 2124.0, 2379 / 2832.0),
                new RelativePoint(354 / 2124.0, 2342 / 2832.0)
        };
        ColorExtractor.LocatorResult[] results = ColorExtractor.findLocators(photoImage, hints);
//        Log.d(TAG, Arrays.toString(results));
        Assert.assertNotNull(results);
        ColorExtractor.LocatorResult ul = results[0];
        Assert.assertTrue(diffEps(ul.x, 627, 5) && diffEps(ul.y, 727, 5) && diffEps(ul.unit, 90, 5));
        ColorExtractor.LocatorResult ur = results[1];
        Assert.assertTrue(diffEps(ur.x, 1545, 5) && diffEps(ur.y, 753, 5) && diffEps(ur.unit, 90, 5));
        ColorExtractor.LocatorResult dr = results[2];
        Assert.assertTrue(diffEps(dr.x, 1517, 5) && diffEps(dr.y, 2135, 5) && diffEps(dr.unit, 90, 5));
        ColorExtractor.LocatorResult dl = results[3];
        Assert.assertTrue(diffEps(dl.x, 590, 5) && diffEps(dl.y, 2116, 5) && diffEps(dl.unit, 90, 5));
    }

    @Test
    public void testNormalExtract() {
        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(0, 0),
                new RelativePoint(899 / 900.0, 0),
                new RelativePoint(899 / 900.0, 1199 / 1200.0),
                new RelativePoint(0, 1199 / 1200.0)
        };
        ArrayList<Integer> colors = ColorExtractor.extractColors(normalImage, hints);
        Assert.assertNotNull(colors);
        Assert.assertEquals(15 * 20 - 36 * 4, colors.size());
        for (int i = 0; i < colors.size(); i++) {
//            Log.d(TAG, "r=" + Color.red(colors.get(i)) + " g=" + Color.green(colors.get(i)) + " b=" + Color.blue(colors.get(i)));
            Assert.assertEquals((i % 2 == 0 ? Color.BLACK : Color.WHITE), (int) colors.get(i));
        }
    }

    @Test
    public void testPhotoExtract() {
        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(396 / 2124.0, 479 / 2832.0),
                new RelativePoint(1773 / 2124.0, 518 / 2832.0),
                new RelativePoint(1745 / 2124.0, 2379 / 2832.0),
                new RelativePoint(354 / 2124.0, 2342 / 2832.0)
        };
        ArrayList<Integer> colors = ColorExtractor.extractColors(photoImage, hints);
        Assert.assertNotNull(colors);
        Assert.assertEquals(15 * 20 - 36 * 4, colors.size());
        for (int i = 0; i < colors.size(); i++) {
//            Log.d(TAG, "r=" + Color.red(colors.get(i)) + " g=" + Color.green(colors.get(i)) + " b=" + Color.blue(colors.get(i)));
            Assert.assertEquals((i % 2 == 0), ColorExtractor.BitmapBinaryWrapper.roundColorBW(colors.get(i)));
        }
    }

    @Test
    public void testUnitSmallerPhotoExtract16x9() {
        // impossible without metainfo = gridWidth and gridHeight

        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(59 / 1274.0, 217 / 2266.0),
                new RelativePoint(1190 / 1274.0, 218 / 2266.0),
                new RelativePoint(1200 / 1274.0, 2089 / 2266.0),
                new RelativePoint(86 / 1274.0, 2095 / 2266.0)
        };
        ArrayList<Integer> colors = ColorExtractor.extractColors(unitSmallerPhotoImage16x9, hints, 30, 50);
        Assert.assertNotNull(colors);
        Assert.assertEquals(30 * 50 - 36 * 4, colors.size());
        for (int i = 0; i < colors.size(); i++) {
//            Log.d(TAG, "r=" + Color.red(colors.get(i)) + " g=" + Color.green(colors.get(i)) + " b=" + Color.blue(colors.get(i)));
            Assert.assertEquals((i % 2 == 0), ColorExtractor.BitmapBinaryWrapper.roundColorBW(colors.get(i)));
        }
    }

    @Test
    public void testUnitSmallerPhotoExtract4x3() {
        // impossible without metainfo = gridWidth and gridHeight

        RelativePoint[] hints = new RelativePoint[]{
                new RelativePoint(197 / 1274.0, 82 / 1699.0),
                new RelativePoint(1115 / 1274.0, 88 / 1699.0),
                new RelativePoint(1130 / 1274.0, 1613 / 1699.0),
                new RelativePoint(206 / 1274.0, 1633 / 1699.0)
        };
        ArrayList<Integer> colors = ColorExtractor.extractColors(unitSmallerPhotoImage4x3, hints, 30, 50);
        Assert.assertNotNull(colors);
        Assert.assertEquals(30 * 50 - 36 * 4, colors.size());
        for (int i = 0; i < colors.size(); i++) {
//            Log.d(TAG, "r=" + Color.red(colors.get(i)) + " g=" + Color.green(colors.get(i)) + " b=" + Color.blue(colors.get(i)));
            Assert.assertEquals((i % 2 == 0), ColorExtractor.BitmapBinaryWrapper.roundColorBW(colors.get(i)));
        }
    }

    @Test
    public void testPhotoLocatorPrev() {
        // fake hint; should work without it or break when trying to use it
        ColorExtractor.LocatorResult ul = ColorExtractor.findLocator(new ColorExtractor.BitmapBinaryWrapper(photoImage),
                new Point(0, 0), ColorExtractor.HintPos.UL, new Point(626, 728), 90);
        Assert.assertNotNull(ul);
        Log.d(TAG, ul.toString());
        Assert.assertTrue(diffEps(ul.x, 627, 5) && diffEps(ul.y, 727, 5) && diffEps(ul.unit, 90, 5));
    }

}