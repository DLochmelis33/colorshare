package ru.hse.colorshare.receiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ru.hse.colorshare.receiver.util.RelativePoint;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private final String TAG;
        private static AtomicInteger cnt = new AtomicInteger(0);

        private static final long TIMEOUT = 1000;

        private final Handler resultHandler;
        private byte[] imageBytes;
        private final int width, height;
        private final RelativePoint[] hints;
        private final long creationTime;
        private final Context context;

        public Task(byte[] imageBytes, int width, int height, RelativePoint[] hints, Handler resultHandler, Context context) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
            this.hints = hints;
            this.context = context;
            this.TAG = "TASK" + (cnt.incrementAndGet());
            creationTime = System.currentTimeMillis();
        }

        // call after any long operation
        private boolean isOutdated() {
            return System.currentTimeMillis() - creationTime > TIMEOUT;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                Log.w(TAG, "could not decode bitmap");
                return;
            }
            imageBytes = null; // help GC
            System.gc();

            Log.d(TAG, "width=" + width + " height=" + height + " bitmap.width=" + bitmap.getWidth() + " bitmap.height=" + bitmap.getHeight());

            ColorExtractor.LocatorResult[] locators = ColorExtractor.findLocators(bitmap, hints);
            if (isOutdated()) {
                Log.d(TAG, "outdated after finding locators");
                return;
            }
            Log.d(TAG, Arrays.toString(locators));

            Bitmap extrasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas extrasCanvas = new Canvas(extrasBitmap);
            Paint extrasPaint = new Paint();
            extrasPaint.setARGB(150, 20, 100, 255);
            for (ColorExtractor.LocatorResult res : locators) {
                if (res != null) {
                    extrasCanvas.drawCircle(res.x, res.y, 30, extrasPaint);
                }
            }
            if (isOutdated()) {
                Log.d(TAG, "outdated after creating extrasBitmap");
                return;
            }

            // ! debug
            Paint hp = new Paint();
            hp.setARGB(150, 240, 240, 30);
            for (RelativePoint rp : hints) {
                extrasCanvas.drawCircle((float) (rp.x * width), (float) (rp.y * height), 30, hp);
            }
            // ! debug end

//            // TODO
            Message msg = Message.obtain(resultHandler, 0, extrasBitmap);
            // String.format("#%06X", (0xFFFFFF & bitmap.getPixel(0, 0)))
            msg.sendToTarget();
            Log.d(TAG, "working ms=" + (System.currentTimeMillis() - startTime));
        }

    }

    private ImageProcessor() {
    }

    private final int poolSize = Runtime.getRuntime().availableProcessors();
    private final BlockingQueue<Runnable> tasksQueue = new LinkedBlockingQueue<>();
    public final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, tasksQueue, new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ImageProcessor instance = new ImageProcessor();

    public static ImageProcessor getInstance() {
        return instance;
    }

    public static void process(Task t) {
        Runtime runtime = Runtime.getRuntime();
        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        if (availHeapSize <= 6 * 1024 * 1024) { // on my phone one image is around 4MB
            Log.w(TAG, "dumping tasks");

            getInstance().executor.purge();

            // ! sometimes gets stuck, why though?
//            for(int i = 0; i < 10; i++) {
//                try {
//                    getInstance().tasksQueue.take();
//                } catch (InterruptedException e) {
//                    // ignored
//                }
//            }
        }

        getInstance().executor.execute(t);
    }

// ----------- image processing methods below -----------

    public static final double DEFAULT_RATIO = 16.0 / 9.0;

    public synchronized void init(int width, int height, int length, Context context) {

//        RenderScript rs = RenderScript.create(context);
//        script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
//
//        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(length);
//        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
//        assert in != null;
//
//        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
//        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
//        assert out != null;

    }

//    // ! copypasted from https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
//    private Bitmap yuv420ToBitmap(byte[] yuvByteArray) {
//        in.copyFrom(yuvByteArray);
//        script.setInput(in);
//        script.forEach(out);
//
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        out.copyTo(bitmap);
//        assert bitmap != null;
//        return bitmap;
//    }

}



