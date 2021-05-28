package ru.hse.colorshare.receiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.hse.colorshare.receiver.util.RelativePoint;
//import ru.hse.colorshare.receiver.util.ScriptC_rotator;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";


    public static class Task implements Runnable {
        private final String TAG;

        private static final long TIMEOUT = 1000;
        private static final AtomicInteger cnt = new AtomicInteger(0);

        private final Handler resultHandler;
        private byte[] imageBytes;
        private final int width, height;
        private final RelativePoint[] hints;
        private final long creationTime;
        private final Context context;
        private final int thisCnt;
        private long startTime;

        public Task(byte[] imageBytes, int width, int height, RelativePoint[] hints, Handler resultHandler, Context context) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
            this.hints = hints;
            this.context = context;
            creationTime = System.currentTimeMillis();
            thisCnt = cnt.incrementAndGet();
            TAG = "TASK-" + thisCnt;
//            Log.d(TAG, "created");
        }

        // call after any long operation
        private boolean isOutdated() {
            if(System.currentTimeMillis() - creationTime > TIMEOUT) {
                Log.d(TAG, "outdated, ms=" + (System.currentTimeMillis() - startTime));
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            Log.d(TAG, "running");
            startTime = System.currentTimeMillis();
            if (!ImageProcessor.getInstance().isInit.get()) {
                // synchronized
                ImageProcessor.getInstance().init(width, height, imageBytes.length, context);
            }

            if (ImageProcessor.getInstance().length != imageBytes.length || ImageProcessor.getInstance().width != width || ImageProcessor.getInstance().height != height) {
                throw new IllegalStateException("ImageProcessor parameters cannot change");
            }

            Bitmap converted = ImageProcessor.getInstance().yuv420ToBitmap(imageBytes);
            float scale = 1f / 2;
            Bitmap scaled = Bitmap.createScaledBitmap(converted, (int) (converted.getWidth() * scale), (int) (converted.getHeight() * scale), false);
            Log.d(TAG, "scale complete in " + (System.currentTimeMillis() - startTime));

            Bitmap bitmap = rotateBitmap(scaled, 90); // ! TODO: bind to sensor orientation
            if (isOutdated()) {
                return;
            } else {
                Log.d(TAG, "converted to bitmap in " + (System.currentTimeMillis() - startTime));
            }
            ImageProcessor.getInstance().cameraService.updatePreviewSurface(bitmap);
            if (isOutdated()) {
                return;
            } else {
                Log.d(TAG, "view updated in " + (System.currentTimeMillis() - startTime));
            }
            imageBytes = null; // help GC!
            System.gc();

            ColorExtractor.LocatorResult[] locators = new ColorExtractor.LocatorResult[4]; /*ColorExtractor.findLocators(bitmap, hints)*/
            ;
//            if (isOutdated()) {
//                Log.d(TAG, "outdated after finding locators in " + (System.currentTimeMillis() - startTime));
//                return;
//            } else {
//                Log.d(TAG, "found locators");
//            }
//            Log.d(TAG, Arrays.toString(locators));

//            Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            Canvas c = new Canvas(b);
//            Paint p = new Paint();
//            p.setARGB(150, 20, 100, 255);
//            for (ColorExtractor.LocatorResult res : locators) {
//                if (res != null) {
//                    c.drawCircle(res.x, res.y, 40, p);
//                }
//            }

//            // ! debug
//            Paint hp = new Paint();
//            hp.setARGB(150, 240, 240, 30);
//            for (RelativePoint rp : hints) {
////                c.drawCircle((float) (rp.x * width), (float) (rp.y * height), 30, hp);
//            }

            // TODO
//            Message msg = Message.obtain(resultHandler, 0, b);
//            msg.sendToTarget();
            Log.e(TAG, "complete in ms=" + (System.currentTimeMillis() - startTime));
            System.gc();
            // String.format("#%06X", (0xFFFFFF & bitmap.getPixel(0, 0)))
        }

        private Bitmap rotateBitmap(Bitmap source, float angle) {
            Log.d(TAG, "rotation started");
            long t = System.currentTimeMillis();
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            Bitmap b = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
            Log.d(TAG, "rotation complete in " + (System.currentTimeMillis() - t));
            return b;
        }

    }

    private ImageProcessor() {
    }

    private final int poolSize = Runtime.getRuntime().availableProcessors();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            poolSize, poolSize, Task.TIMEOUT, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.DiscardOldestPolicy()
    );
    private static final ImageProcessor instance = new ImageProcessor(); // ! ! ! ! !

    public static ImageProcessor getInstance() {
        return instance;
    }

    public void process(Task t) {
        // ! sometimes gets stuck, why though?
        Runtime runtime = Runtime.getRuntime();
        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());

        if (availHeapSize <= 6 * 1024 * 1024) { // on my phone one image is around 4MB
            Log.w(TAG, "dumping tasks");
            getInstance().executor.purge();
        }
        getInstance().executor.execute(t);
    }

    public void shutdown() {
        executor.shutdownNow();
        isInit.set(false);
        initFinished.set(false);
//        cameraService = null; // ! can cause NPE in finishing tasks
    }

    public CameraService cameraService;

    public void setCameraService(CameraService cameraService) {
        this.cameraService = cameraService;
    }
// ----------- image processing methods below -----------

    public static final double DEFAULT_RATIO = 16.0 / 9.0;

    private ScriptIntrinsicYuvToRGB scriptConvert;
    //    private ScriptIntrinsicResize scriptResize;
    private Allocation inConvert, outConvert;
    private Allocation inResize, outResize;
    private RenderScript rsResize;

    private final AtomicBoolean isInit = new AtomicBoolean(false);
    private final AtomicBoolean initFinished = new AtomicBoolean(false);
    private int width, height, length;

    public synchronized void init(int width, int height, int length, Context context) {
        if (isInit.get()) {
            return;
        }
        this.width = width;
        this.height = height;
        this.length = length;

        RenderScript rsConvert = RenderScript.create(context);
        scriptConvert = ScriptIntrinsicYuvToRGB.create(rsConvert, Element.U8_4(rsConvert));

        Type.Builder yuvType = new Type.Builder(rsConvert, Element.U8(rsConvert)).setX(length);
        inConvert = Allocation.createTyped(rsConvert, yuvType.create(), Allocation.USAGE_SCRIPT);
        assert inConvert != null;

        Type.Builder rgbaType = new Type.Builder(rsConvert, Element.RGBA_8888(rsConvert)).setX(width).setY(height);
        outConvert = Allocation.createTyped(rsConvert, rgbaType.create(), Allocation.USAGE_SCRIPT);
        assert outConvert != null;

        rsResize = RenderScript.create(context);
//        scriptResize = ScriptIntrinsicResize.create(rsResize);

        initFinished.set(true);
    }

    public synchronized Bitmap yuv420ToBitmap(byte[] yuvByteArray) {
        long t = System.currentTimeMillis();
        inConvert.copyFrom(yuvByteArray);
        scriptConvert.setInput(inConvert);
        scriptConvert.forEach(outConvert);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outConvert.copyTo(bitmap);
        assert bitmap != null;

        Log.d(TAG, "conversion complete in " + (System.currentTimeMillis() - t));
        return bitmap;
    }

//    public synchronized Bitmap scale(Bitmap source, float scale) {
//        Bitmap output = Bitmap.createBitmap((int) (source.getWidth() * scale), (int) (source.getHeight() * scale), Bitmap.Config.ARGB_8888);
//        Allocation inAlloc = Allocation.createFromBitmap(rsResize, source);
//        Allocation outAlloc = Allocation.createFromBitmap(rsResize, output);
//        scriptResize.setInput(inAlloc);
//        scriptResize.forEach_bicubic(outAlloc);
//        outAlloc.copyTo(output);
//        return output;
//    }

//    public Bitmap rotate(Bitmap bitmap) {
//        RenderScript rs = RenderScript.create(mContext);
//        ScriptC_rotator script = new ScriptC_rotator(rs);
//        script.set_inWidth(bitmap.getWidth());
//        script.set_inHeight(bitmap.getHeight());
//        Allocation sourceAllocation = Allocation.createFromBitmap(rs, bitmap,
//                Allocation.MipmapControl.MIPMAP_NONE,
//                Allocation.USAGE_SCRIPT);
//        bitmap.recycle();
//        script.set_inImage(sourceAllocation);
//
//        int targetHeight = bitmap.getWidth();
//        int targetWidth = bitmap.getHeight();
//        Bitmap.Config config = bitmap.getConfig();
//        Bitmap target = Bitmap.createBitmap(targetWidth, targetHeight, config);
//        final Allocation targetAllocation = Allocation.createFromBitmap(rs, target,
//                Allocation.MipmapControl.MIPMAP_NONE,
//                Allocation.USAGE_SCRIPT);
//        script.forEach_rotate_90_clockwise(targetAllocation, targetAllocation);
//        targetAllocation.copyTo(target);
//        rs.destroy();
//        return target;
//    }

}



