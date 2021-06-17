package ru.hse.colorshare.receiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.hse.colorshare.receiver.util.RelativePoint;
import ru.hse.colorshare.receiver.util.SlidingAverage;
import ru.hse.colorshare.rs.ScriptC_rotator;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private final String TAG;
        private static final AtomicInteger cnt = new AtomicInteger(0);

        private static final long TIMEOUT = 2000;

        private final Handler resultHandler;
        private byte[] imageBytes;
        private final int width, height;
        private final RelativePoint[] hints;
        private final long creationTime;
        private final Context context;
        private final int bulkOnCreation;

        public Task(byte[] imageBytes, int width, int height, RelativePoint[] hints, Handler resultHandler, Context context) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
            this.hints = hints;
            this.context = context;
            this.TAG = "TASK" + (cnt.incrementAndGet());
            creationTime = System.currentTimeMillis();
            bulkOnCreation = ReceiverController.currentBulk;
        }

        // call after any long operation
        private boolean isOutdated(String msg) {
            if (System.currentTimeMillis() - creationTime > TIMEOUT) {
                Log.w(TAG, "outdated after having " + msg);
                return true;
            }
            if (bulkOnCreation != ReceiverController.currentBulk) {
                Log.w(TAG, "outdated bulk");
            }
            return false;
        }

        @Override
        public void run() {
            if (bulkOnCreation == -1) {
                Log.v(TAG, "no bulk");
                return;
            }
            if (isOutdated("started")) {
                return;
            }

            long startTime = System.currentTimeMillis();
            Log.v(TAG, "ms elapsed before start: " + (startTime - creationTime));
            if (startTime - creationTime > TIMEOUT) {
                Log.w(TAG, "outdated before start!");
                return;
            }

            while (!getInstance().isInit.get()) {
                getInstance().init(context);
            }

            Bitmap bitmap = getInstance().rotate(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            if (bitmap == null) {
                if (getInstance().isInit.get()) {
                    Log.w(TAG, "could not decode bitmap");
                } // else is part of shutting down
                return;
            }
            imageBytes = null; // help GC
            System.gc();

//            Log.d(TAG, "width=" + width + " height=" + height + " bitmap.width=" + bitmap.getWidth() + " bitmap.height=" + bitmap.getHeight());

            ColorExtractor.LocatorResult[] locators = ColorExtractor.findLocators(bitmap, hints);
            if (isOutdated("locators found")) {
                return;
            }
//            Log.d(TAG, Arrays.toString(locators));

            // ! ! !
            Bitmap extrasBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
            Canvas extrasCanvas = new Canvas(extrasBitmap);
            Paint extrasPaint = new Paint();
            extrasPaint.setARGB(150, 20, 100, 255);
            for (ColorExtractor.LocatorResult res : locators) {
                if (res != null) {
                    extrasCanvas.drawCircle(res.x, res.y, 30, extrasPaint);
                }
            }
            if (isOutdated("extrasBitmap created")) {
                return;
            }

            // ! debug broken :)
//            Paint hp = new Paint();
//            hp.setARGB(150, 240, 240, 30);
//            for (RelativePoint rp : hints) {
//                extrasCanvas.drawCircle((float) (rp.x * width), (float) (rp.y * height), 30, hp);
//            }

            ArrayList<Integer> colors = ColorExtractor.extractColorsFromResult(bitmap, locators, getInstance().gridWidth, getInstance().gridHeight);
            if (colors != null) {
                int[] colorArray = new int[colors.size()];
                ReceiverController.decodingController.testFrame(colorArray);
//                if (isOutdated("colors checked")) {
//                    return;
//                }
            } else {
                Log.w(TAG, "no colors");
            }

//            // TODO
            Message msg = Message.obtain(resultHandler, 0, extrasBitmap);
            msg.sendToTarget();

            long workTime = System.currentTimeMillis() - startTime;
            getInstance().taskTimeCounter.addValue(workTime);
            Log.v(TAG, "avg working ms = " + getInstance().taskTimeCounter.getAverage());
        }

    }

    private ImageProcessor() {
    }

    private final int poolSize = Runtime.getRuntime().availableProcessors();
    private final BlockingQueue<Runnable> tasksQueue = new LinkedBlockingQueue<>();
    public ThreadPoolExecutor executor = new ThreadPoolExecutor(
            poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, tasksQueue, new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ImageProcessor instance = new ImageProcessor();

    public static ImageProcessor getInstance() {
        return instance;
    }

    public static void process(Task t) {
        Runtime runtime = Runtime.getRuntime();
        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        if (availHeapSize <= 6 * 1024 * 1024 /* on my phone one image is around 4MB */) {
            Log.w(TAG, "dumping tasks: approaching OOM");
            getInstance().executor.purge();
        }
        if (getInstance().tasksQueue.size() > 10) {
            Log.w(TAG, "dumping tasks: too many tasks queued");
            getInstance().executor.purge();
        }

        getInstance().executor.execute(t);
    }

    private final SlidingAverage taskTimeCounter = new SlidingAverage(10);

    private volatile int gridWidth = -1;
    private volatile int gridHeight = -1;

    public void setGridSize(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

// ----------- image processing methods below -----------

    private RenderScript rs;
    private ScriptC_rotator script;
    private final AtomicBoolean isInit = new AtomicBoolean(false);

    public synchronized void init(Context context) {
        if (isInit.get()) {
            return;
        }
        Log.d(TAG, "initializing");
        rs = RenderScript.create(context);
        script = new ScriptC_rotator(rs);
        isInit.set(true);
    }

    public synchronized void shutdown() {
        if (!isInit.get()) {
            return;
        }
        executor.shutdownNow();
        isInit.set(false);

        // ! TODO: FIX THIS CODESTYLE (new executor alive after shutdown)
        executor = new ThreadPoolExecutor(
                poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, tasksQueue, new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public Bitmap rotate(Bitmap bitmap) {
        if (!isInit.get()) {
//            throw new IllegalStateException();
            return null;
        }
        script.set_inWidth(bitmap.getWidth());
        script.set_inHeight(bitmap.getHeight());
        Allocation sourceAllocation = Allocation.createFromBitmap(rs, bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        script.set_inImage(sourceAllocation);

        int targetHeight = bitmap.getWidth();
        int targetWidth = bitmap.getHeight();
        Bitmap.Config config = bitmap.getConfig();
        bitmap.recycle();
        Bitmap target = Bitmap.createBitmap(targetWidth, targetHeight, config);
        final Allocation targetAllocation = Allocation.createFromBitmap(rs, target,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        script.forEach_rotate_270_clockwise(targetAllocation, targetAllocation);
        targetAllocation.copyTo(target);

        sourceAllocation.destroy();
        targetAllocation.destroy();
        return target;
    }

//    public synchronized void init(int width, int height, int length, Context context) {
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
//    }

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



