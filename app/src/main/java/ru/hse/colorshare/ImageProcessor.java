package ru.hse.colorshare;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor";

    public static class Task implements Runnable {

        private final Handler resultHandler;
        private final byte[] imageBytes;

        public Task(byte[] imageBytes, Handler resultHandler) {
            this.imageBytes = imageBytes;
            this.resultHandler = resultHandler;
        }

        @Override
        public void run() {
            // TODO
            // dummy load
            try {
                TimeUnit.MILLISECONDS.sleep(50);

                Message msg = Message.obtain(resultHandler, 0, imageBytes[0]);
                msg.sendToTarget();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        if(tasksQueue.size() > maximumQueueCapacity * 0.9) {
            Log.w(TAG, "approaching tasks queue capacity: " + tasksQueue.size() + " / " + maximumQueueCapacity);
        }
    }

}
