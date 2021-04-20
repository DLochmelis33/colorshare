package ru.hse.colorshare;

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private final Handler resultHandler;
        private final byte[] imageBytes;
        private final int width, height;

        public Task(byte[] imageBytes, int width, int height, Handler resultHandler) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            if (ImageProcessor.length != imageBytes.length || ImageProcessor.width != width || ImageProcessor.height != height) {
                Log.w(TAG, "changing ImageProcessor parameters");
                ImageProcessor.width = width;
                ImageProcessor.height = height;
                ImageProcessor.length = imageBytes.length;
                ImageProcessor.init();
            }
            Bitmap bitmap = yuv420ToBitmap(imageBytes);
            // TODO
            Message msg = Message.obtain(resultHandler, 0, String.format("#%06X", (0xFFFFFF & bitmap.getPixel(0, 0))));
            msg.sendToTarget();
        }
    }

    // everything is static in order to avoid being synchronized (speed optimization)
    private ImageProcessor() {
    }

    private static final int corePoolSize = 1;
    private static final int maximumPoolSize = 5; // ! subject to change, needs irl testing
    private static final long keepAliveTime = 1000;
    private static final TimeUnit unit = TimeUnit.MILLISECONDS;

    private static final int maximumQueueCapacity = 40; // ! subject to change, value of 40 goes well with 50ms latency
    private static final BlockingQueue<Runnable> tasksQueue = new ArrayBlockingQueue<>(maximumQueueCapacity);
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, tasksQueue);

    public static void process(Task t) {
        executor.execute(t);
        if (tasksQueue.size() > maximumQueueCapacity * 0.9) {
            Log.w(TAG, "approaching tasks queue capacity: " + tasksQueue.size() + " / " + maximumQueueCapacity);
        }
    }

    public static void shutdown() {
        executor.shutdown();
    }

    private static Context context = null; // ! ! ! ! !

    public synchronized static void setContext(Context context) {
        if (ImageProcessor.context != null) {
            throw new IllegalStateException("can set context only once");
        }
        ImageProcessor.context = context;
    }

// ----------- image processing methods below -----------

    private static ScriptIntrinsicYuvToRGB script;
    private static Allocation in, out;

    public static int width, height, length;

    public static void init() {
        RenderScript rs = RenderScript.create(context);
        script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(length);
        in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        // The allocations above "should" be cached if you are going to perform
        // repeated conversion of YUV_420_888 to Bitmap.
    }

    // ! copypasted from https://blog.minhazav.dev/how-to-convert-yuv-420-sp-android.media.Image-to-Bitmap-or-jpeg/
    private static Bitmap yuv420ToBitmap(byte[] yuvByteArray) {
        in.copyFrom(yuvByteArray);
        script.setInput(in);
        script.forEach(out);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bitmap);
        assert bitmap != null;
        return bitmap;
    }

}
