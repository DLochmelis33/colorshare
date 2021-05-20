package ru.hse.colorshare.receiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.hse.colorshare.receiver.util.RelativePoint;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private static final long TIMEOUT = 1000;

        private final Handler resultHandler;
        private byte[] imageBytes;
        private final int width, height;
        private final RelativePoint[] hints;
        private long startTime;

        public Task(byte[] imageBytes, int width, int height, RelativePoint[] hints, Handler resultHandler) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
            this.hints = hints;
        }

        // call after any long operation
        private boolean isOutdated() {
            return System.currentTimeMillis() - startTime > TIMEOUT;
        }

        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            if (!ImageProcessor.getInstance().isInit) {
                ImageProcessor.getInstance().init(width, height, imageBytes.length);
            }
            if (ImageProcessor.getInstance().length != imageBytes.length || ImageProcessor.getInstance().width != width || ImageProcessor.getInstance().height != height) {
                throw new IllegalStateException("ImageProcessor parameters cannot change");
            }

            Bitmap bitmap = ImageProcessor.getInstance().yuv420ToBitmap(imageBytes);
            if(isOutdated()) {
                return;
            }

            imageBytes = null; // help GC!
            System.gc(); // ! this might slow everything down

            ColorExtractor.LocatorResult[] locators = ColorExtractor.findLocators(bitmap, hints);
            if(isOutdated()) {
                return;
            }
//            ArrayList<Integer> colors = ColorExtractor.extractColors(bitmap, hints);


            // TODO
            Message msg = Message.obtain(resultHandler, 0, Arrays.toString(locators));
            // String.format("#%06X", (0xFFFFFF & bitmap.getPixel(0, 0)))
            msg.sendToTarget();
//            Log.d(TAG, "ms=" + (System.currentTimeMillis() - startTime));
        }
    }

    private static class DumpableLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

        /**
         * Removes oldest elements, blocking put() and take()
         *
         * @param howMany how many elements to remove
         */
        public void dump(int howMany) {
            for (int i = 0; i < howMany; i++) {
                try {
                    super.take();
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }

    private ImageProcessor() {
    }

    private final int poolSize = Runtime.getRuntime().availableProcessors();
    private final DumpableLinkedBlockingQueue<Runnable> tasksQueue = new DumpableLinkedBlockingQueue<>();
    public final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, tasksQueue, new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ImageProcessor instance = new ImageProcessor(); // ! ! ! ! !

    public static ImageProcessor getInstance() {
        return instance;
    }

    public static void process(Task t) {
        // ! sometimes gets stuck, why though?
        Runtime runtime = Runtime.getRuntime();
        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        if (availHeapSize <= 16 * 1024 * 1024) { // on my phone one image is around 4MB
            Log.w(TAG, "dumping tasks");
            getInstance().tasksQueue.dump(10);
        }

        getInstance().executor.execute(t);
    }

    private Context context = null;

    public synchronized void setContext(Context context) {
        this.context = context;
    }

// ----------- image processing methods below -----------

    public static final double DEFAULT_RATIO = 16.0 / 9.0;

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



