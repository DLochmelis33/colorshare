package ru.hse.colorshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TransmitterActivity extends AppCompatActivity {

    private Reader reader;

    private TransmissionParams params;
    private Generator generator;

    private static final String LOG_TAG = "ColorShare:transmitter";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri fileToSendUri = getIntent().getParcelableExtra("fileToSendUri");
        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(fileToSendUri);
            Objects.requireNonNull(inputStream);
            reader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException | RuntimeException exc) {
            setResult(MainActivity.TransmissionResultCode.FAILED.value, new Intent());
            finish();
        }
        Log.d(LOG_TAG, "received intent: " + "uri = " + fileToSendUri);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        enterFullscreenAndLockOrientation();
                    }
                });

        enterFullscreenAndLockOrientation();
        setContentView(new DrawView(this));
    }

    private void enterFullscreenAndLockOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException exc) {
            throw new RuntimeException("Transmission file failed to close", exc);
        }
    }

    private class DrawView extends SurfaceView implements SurfaceHolder.Callback {

        private DrawThread drawThread;

        public DrawView(Context context) {
            super(context);
            getHolder().addCallback(this);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.d(LOG_TAG, "surfaceChanged() was call");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(LOG_TAG, "DrawView created");

            // in future this methods may need to communicate with receiver,
            // draw something on screen
            if (params == null) {
                params = getTransmissionParams();
            }
            if (generator == null) {
                generator = getUnitGenerator();
            }
            Log.d(LOG_TAG, "Transmissions params: " + params.toString());
            Log.d(LOG_TAG, "Generator params: frame size = " + generator.getFrameSize() + ", total number of frames = " + generator.getFramesNumber());

            // start transmission
            drawThread = new DrawThread(getHolder());
            drawThread.setRunning(true);
            drawThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            drawThread.setRunning(false);
            while (retry) {
                try {
                    drawThread.join();
                    retry = false;
                } catch (InterruptedException ignored) {
                }
            }
            Log.d(LOG_TAG, "DrawView destroyed");
            ((Activity) getContext()).setResult(RESULT_OK, new Intent());
        }

        private TransmissionParams getTransmissionParams() {
            // TODO: implement algorithm to determine best transmission parameters
            // currently hardcoded constants are used
            final Rect frameBounds = getHolder().getSurfaceFrame();
            int rows = 20;
            float unitRectSize = frameBounds.height() * 1f / rows;
            RectF unitRect = new RectF(0, 0, unitRectSize, unitRectSize);
            int cols = (int) (frameBounds.width() / unitRectSize);
            return new TransmissionParams(unitRect, rows, cols);
        }

        private Generator getUnitGenerator() {
            // TODO: get the most appropriate unit generator
            // currently mock generator is used
            if (params == null) {
                params = getTransmissionParams();
            }
            int frameSize = params.getCols() * params.getRows(); // in units
            return new Generator(frameSize, 100 * 8 / frameSize + 10);
        }

        private class DrawThread extends Thread {

            private boolean running = false;
            private final WeakReference<SurfaceHolder> surfaceHolderWeakRef;

            private final Paint paint;

            public DrawThread(SurfaceHolder surfaceHolder) {
                this.surfaceHolderWeakRef = new WeakReference<>(surfaceHolder);
                paint = new Paint();
            }

            public void setRunning(boolean running) {
                this.running = running;
            }

            @Override
            public void run() {
                Canvas canvas;
                while (running) {
                    canvas = null;
                    try {
                        List<Generator.Unit> colors = generator.getDataFrame();
                        if (colors == null) {
                            setResult(MainActivity.TransmissionResultCode.SUCCESS.value, new Intent());
                            finish();
                            return;
                        }
                        canvas = surfaceHolderWeakRef.get().lockCanvas(null);
                        if (canvas == null)
                            continue;
                        synchronized (surfaceHolderWeakRef.get()) { // probably, synchronised is unnecessary
                            canvas.save();
                            canvas.drawColor(Color.BLACK);
                            paint.setStyle(Paint.Style.FILL);
                            RectF unitRect = params.getUnitRect();
                            for (int i = 0; i < params.getRows(); ++i) {
                                canvas.save();
                                for (int j = 0; j < params.getCols(); ++j) {
                                    paint.setColor(colors.get(i * params.getCols() + j).getColor());
                                    canvas.drawRect(unitRect, paint);
                                    canvas.translate(unitRect.width(), 0);
                                }
                                canvas.restore();
                                canvas.translate(0, unitRect.height());
                            }
                            canvas.restore();
                        }
                    } finally {
                        if (canvas != null) {
                            surfaceHolderWeakRef.get().unlockCanvasAndPost(canvas);
                        }
                    }
                    boolean response = waitForReceiverResponse();
                    Log.d(LOG_TAG, "data frame #" + generator.getFrameIndex() +
                            " was successfully sent = " + response);
                    generator.setDataFrameSuccessfullySent(response);
                }
            }

            private boolean waitForReceiverResponse() {
                // currently is mock
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException exc) {
                    return false;
                }
                return true;
            }
        }
    }

    private static final class TransmissionParams {
        private final RectF unitRect;
        private final int rows;
        private final int cols;

        public TransmissionParams(RectF unitRect, int rows, int cols) {
            this.unitRect = unitRect;
            this.rows = rows;
            this.cols = cols;
        }

        public RectF getUnitRect() {
            return unitRect;
        }

        public int getCols() {
            return cols;
        }

        public int getRows() {
            return rows;
        }

        @Override
        public @NonNull
        String toString() {
            return "unit rect: height = " + unitRect.height() +
                    ", width = " + unitRect.width() +
                    "; rows = " + rows + "; cols = " + cols;
        }
    }

    // mock generator of colors to transmit
    private static final class Generator {
        private final int frameSize;
        private final int framesNumber;
        private final Random random;
        private int currentFrameIndex;

        public Generator(int frameSize, int framesNumber) {
            this.frameSize = frameSize;
            this.framesNumber = framesNumber;
            random = new Random();
            currentFrameIndex = 0;
        }

        public List<Unit> getDataFrame() {
            if (currentFrameIndex == framesNumber) {
                return null;
            }
            List<Unit> dataFrame = new ArrayList<>();
            for (int i = 0; i < frameSize; ++i) {
                dataFrame.add(new Unit(Color.rgb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256))));
            }
            return dataFrame;
        }

        public void setDataFrameSuccessfullySent(boolean result) {
            if (result) {
                currentFrameIndex++;
            }
        }

        public int getFrameIndex() {
            return currentFrameIndex;
        }

        public int getFramesNumber() {
            return framesNumber;
        }

        public int getFrameSize() {
            return frameSize;
        }

        public static class Unit {
            private final int color;

            public Unit(int color) {
                this.color = color;
            }

            public int getColor() {
                return color;
            }
        }
    }
}