package ru.hse.colorshare.receiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.hse.colorshare.util.RelativePoint;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private final Handler resultHandler;
        private final byte[] imageBytes;
        private final int width, height;
        private final RelativePoint[] hints;

        public Task(byte[] imageBytes, int width, int height, RelativePoint[] hints, Handler resultHandler) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
            this.hints = hints;
        }

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            if (!ImageProcessor.getInstance().isInit) {
                ImageProcessor.getInstance().init(width, height, imageBytes.length);
            }
            if (ImageProcessor.getInstance().length != imageBytes.length || ImageProcessor.getInstance().width != width || ImageProcessor.getInstance().height != height) {
                throw new IllegalStateException("ImageProcessor parameters cannot change");
            }
            Bitmap bitmap = ImageProcessor.getInstance().yuv420ToBitmap(imageBytes);

            ColorExtractor.LocatorResult[] locators = ColorExtractor.findLocators(bitmap, hints);
//            ArrayList<Integer> colors = ColorExtractor.extractColors(bitmap, hints);


            // TODO
            Message msg = Message.obtain(resultHandler, 0, Arrays.toString(locators));
            // String.format("#%06X", (0xFFFFFF & bitmap.getPixel(0, 0)))
            msg.sendToTarget();
            Log.w(TAG, "ms=" + (System.currentTimeMillis() - time));
        }
    }

    private ImageProcessor() {
    }

    private static final ImageProcessor instance = new ImageProcessor(); // ! ! ! ! !

    public static ImageProcessor getInstance() {
        return instance;
    }

    public static void process(Task t) {
        getInstance().executor.execute(t);
    }

    private final int poolSize = 5; // ! needs testing
    public final Executor executor = Executors.newFixedThreadPool(poolSize);

    private Context context = null;

    public synchronized void setContext(Context context) {
        this.context = context;
    }

// ----------- image processing methods below -----------

    public static final double RATIO = 16.0 / 9.0;

    private ScriptIntrinsicYuvToRGB script;
    private Allocation in, out;

    private boolean isInit = false;
    private int width, height, length;

    public synchronized void init(int width, int height, int length) {
        if (isInit) {
            return;
        }
        this.width = width;
        this.height = height;
        this.length = length;

        RenderScript rs = RenderScript.create(context);
        script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(length);
        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        assert in != null;

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        assert out != null;

        isInit = true;
    }

    // ! copypasted from https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
    private Bitmap yuv420ToBitmap(byte[] yuvByteArray) {
        in.copyFrom(yuvByteArray);
        script.setInput(in);
        script.forEach(out);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bitmap);
        assert bitmap != null;
        return bitmap;
    }

}



