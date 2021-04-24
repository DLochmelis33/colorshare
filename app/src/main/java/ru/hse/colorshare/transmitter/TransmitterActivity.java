package ru.hse.colorshare.transmitter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import ru.hse.colorshare.MainActivity;
import ru.hse.colorshare.generator.DataFrameGenerator;
import ru.hse.colorshare.generator.MockDataFrameGeneratorFactory;

public class TransmitterActivity extends AppCompatActivity {

    private TransmissionState state;
    private TransmissionParams params;
    private MockDataFrameGeneratorFactory generatorFactory;

    private int screenOrientation;

    private static final String LOG_TAG = "ColorShare:transmitter";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            state = TransmissionState.values()[savedInstanceState.getInt("state")];
            Log.d(LOG_TAG, "restored state: " + state);
        } else {
            state = TransmissionState.INITIAL;
        }
        // cancel because of params loss
        // TODO: support restoring = save TransmitterActivity fields via ViewModel
        if (!state.equals(TransmissionState.INITIAL)) {
            setResult(MainActivity.TransmissionResultCode.CANCELLED.value, new Intent());
            finish();
            return;
        }

        Uri fileToSendUri = getIntent().getParcelableExtra("fileToSendUri");
        Log.d(LOG_TAG, "received intent: " + "uri = " + fileToSendUri);

        try {
            generatorFactory = new MockDataFrameGeneratorFactory(fileToSendUri, this);
        } catch (FileNotFoundException exc) {
            setResult(MainActivity.TransmissionResultCode.FAILED_TO_READ_FILE.value, new Intent());
            finish();
            return;
        }

        screenOrientation = getResources().getConfiguration().orientation;

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
            if (generatorFactory != null) {
                generatorFactory.finish();
            }
        } catch (IOException exc) {
            throw new RuntimeException("Transmission file failed to close", exc);
        }
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state", state.ordinal());
        Log.d(LOG_TAG, "save state = " + state);
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
            Log.d(LOG_TAG, "DrawView created, transmission state = " + state);
            setRequestedOrientation(screenOrientation);

            if (state.equals(TransmissionState.INITIAL)) {
                try {
                    params = getTransmissionParams();
                } catch (IllegalStateException exc) {
                    Log.d(LOG_TAG, "Getting transmission params failed: " + exc.getMessage());
                    state = TransmissionState.FAILED;
                    setResult(MainActivity.TransmissionResultCode.FAILED_TO_GET_TRANSMISSION_PARAMS.value, new Intent());
                    finish();
                    return;
                }
                generatorFactory.setParams(params);
            }
            if (params == null) {
                throw new IllegalStateException("transmission params remain null in state " + state);
            }
            Log.d(LOG_TAG, "Transmissions params: " + params.toString());

            // start transmission
            state = TransmissionState.STARTED;
            drawThread = new DrawThread(getHolder());
            drawThread.setRunning(true);
            drawThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            if (drawThread != null) {
                drawThread.setRunning(false);
                while (retry) {
                    try {
                        drawThread.join();
                        retry = false;
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            Log.d(LOG_TAG, "DrawView destroyed");
        }

        private TransmissionParams getTransmissionParams() throws IllegalStateException {
            final Rect frameBounds = getHolder().getSurfaceFrame();
            int height = frameBounds.height() - 2 * TransmissionParams.BORDER_SIZE;
            int width = frameBounds.width() - 2 * TransmissionParams.BORDER_SIZE;
            // a = height; b = width; d = unitSize;
            // a/d * b/d approx EXPECTED_FRAME_SIZE
            int startUnitSize = (int) Math.sqrt((double) ((height * width) / TransmissionParams.EXPECTED_FRAME_SIZE_IN_UNITS));

            // 1 <= d && MIN_UNITS_IN_LINE <= min(a,b)/d
            int minUnitSize = 0; // non-inclusive
            int maxUnitSize = Math.min(height, width) / TransmissionParams.MIN_UNITS_IN_LINE; // inclusive
            startUnitSize = Math.min(maxUnitSize, startUnitSize);
            Log.d(LOG_TAG, "Start getting transmission params: frame bounds = " + frameBounds.height() + " x " + frameBounds.width() +
                    "; border size = " + TransmissionParams.BORDER_SIZE + "; start unit size = " + startUnitSize +
                    "; max unit size = " + maxUnitSize);

            // prepare to binary search
            if (!testReceiverAbilityToRead(maxUnitSize)) {
                throw new IllegalStateException("receiver is unable to read maxUnitSize greed");
            }
            int left = minUnitSize; // non-inclusive
            int right = maxUnitSize; // inclusive
            if (testReceiverAbilityToRead(startUnitSize)) {
                right = startUnitSize;
            } else {
                left = startUnitSize;
            }
            // binary search
            while (right - left > 1) {
                int mid = (left + right) / 2;
                if (testReceiverAbilityToRead(mid)) {
                    right = mid;
                } else {
                    left = mid;
                }
            }
            Log.d(LOG_TAG, "Getting transmission params: binary search finished with left = " + left + ", right = " + right);
            int unitSize = right;
            int rows = height / unitSize;
            int cols = width / unitSize;
            // center units greed
            PointF leftTopPointOfGreed = new PointF((frameBounds.width() - cols * unitSize) / 2f, (frameBounds.height() - rows * unitSize) / 2f);
            return new TransmissionParams(unitSize, rows, cols, leftTopPointOfGreed);
        }

        private boolean testReceiverAbilityToRead(int unitSize) {
            // TODO: add test messaging with receiver
            return unitSize >= 40; // mock for now
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
                    DataFrameGenerator generator = generatorFactory.getDataFrameGenerator();
                    Log.d(LOG_TAG, "Generator info: " + generator.getInfo());
                    try {
                        int[] colors = generator.getNextDataFrame().getColors();
                        if (colors == null) {
                            state = TransmissionState.FINISHED;
                            setResult(MainActivity.TransmissionResultCode.SUCCEED.value, new Intent());
                            finish();
                            return;
                        }
                        canvas = surfaceHolderWeakRef.get().lockCanvas(null);
                        if (canvas == null)
                            continue;
                        synchronized (surfaceHolderWeakRef.get()) { // probably, synchronised is unnecessary
                            canvas.drawColor(Color.BLACK);
                            canvas.translate(params.leftTopOfGreed.x, params.leftTopOfGreed.y);
                            canvas.save();

                            int locatorMarkSize = LocatorMarkGraphic.SIDE_SIZE_IN_UNITS;
                            int unitSize = params.unitSize;

                            // top stripe
                            LocatorMarkGraphic.draw(canvas, LocatorMarkGraphic.Location.LEFT_TOP, unitSize);
                            canvas.save();
                            canvas.translate(locatorMarkSize * unitSize, 0);
                            int stripeWidth = params.cols - 2 * locatorMarkSize; // in units
                            int index = drawColorUnitsStripe(canvas, colors, 0, locatorMarkSize, stripeWidth);
                            canvas.translate(stripeWidth * unitSize, 0);
                            LocatorMarkGraphic.draw(canvas, LocatorMarkGraphic.Location.RIGHT_TOP, unitSize);
                            canvas.restore();

                            // mid stripe
                            canvas.translate(0, locatorMarkSize * unitSize);
                            int stripeHeight = params.rows - 2 * locatorMarkSize;
                            index = drawColorUnitsStripe(canvas, colors, index, stripeHeight, params.cols);

                            // bottom stripe
                            canvas.translate(0, stripeHeight * unitSize);
                            LocatorMarkGraphic.draw(canvas, LocatorMarkGraphic.Location.LEFT_BOTTOM, unitSize);
                            canvas.translate(locatorMarkSize * unitSize, 0);
                            drawColorUnitsStripe(canvas, colors, index, locatorMarkSize, params.cols - locatorMarkSize);

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
                    generator.setSuccess(response);
                }
            }

            private int drawColorUnitsStripe(Canvas canvas, int[] colors, int startIndex, int stripeHeight, int stripeWidth) {
                // startIndex inclusive; stripeHeight and stripeWidth in units
                paint.setStyle(Paint.Style.FILL);
                int index = startIndex;
                canvas.save();
                for (int i = 0; i < stripeHeight; i++) {
                    canvas.save();
                    for (int j = 0; j < stripeWidth; j++) {
                        paint.setColor(colors[index]);
                        canvas.drawRect(params.unitRect, paint);
                        canvas.translate(params.unitSize, 0);
                        index++;
                    }
                    canvas.restore();
                    canvas.translate(0, params.unitSize);
                }
                canvas.restore();
                return index;
            }

            private boolean waitForReceiverResponse() {
                // currently is mock
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException exc) {
                    return false;
                }
                return true;
            }
        }
    }

    private enum TransmissionState {
        INITIAL, STARTED, FINISHED, FAILED
    }
}