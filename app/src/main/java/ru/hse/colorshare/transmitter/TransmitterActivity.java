package ru.hse.colorshare.transmitter;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ru.hse.colorshare.MainActivity;
import ru.hse.colorshare.coding.encoding.DataFrameBulk;
import ru.hse.colorshare.coding.encoding.EncodingController;
import ru.hse.colorshare.coding.exceptions.EncodingException;
import ru.hse.colorshare.communication.CommunicationProtocol;
import ru.hse.colorshare.communication.Communicator;

@SuppressLint("Assert")
public class TransmitterActivity extends AppCompatActivity {

    private TransmissionState state;
    private TransmissionParams params;
    private EncodingController encodingController;

    private int screenOrientation;

    private static final int FRAMES_PER_BULK = 10; // constant for mock testing
    private static final String LOG_TAG = "ColorShare:transmitter";

    private Communicator communicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri fileToSendUri = getIntent().getParcelableExtra("fileToSendUri");
        Log.d(LOG_TAG, "received intent: " + "uri = " + fileToSendUri);

        try {
            encodingController = EncodingController.create(fileToSendUri, this);
        } catch (FileNotFoundException exc) {
            setResult(MainActivity.TransmissionResultCode.FAILED_TO_READ_FILE.value, new Intent());
            finish();
            return;
        }
        state = TransmissionState.INITIAL;

        screenOrientation = getResources().getConfiguration().orientation;

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        enterFullscreenAndLockOrientation();
                    }
                });
        enterFullscreenAndLockOrientation();

        assert checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        communicator = Communicator.getColorShareTransmitterSideCommunicator(this);
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
            if (communicator != null) {
                communicator.close();
            }
            if (encodingController != null) {
                encodingController.close();
            }
        } catch (IOException ioException) {
            throw new RuntimeException("Transmission file failed to close", ioException);
        }
    }

    private class DrawView extends SurfaceView implements SurfaceHolder.Callback {

        private DrawThread drawThread;
        private TransmissionControllerThread controllerThread;

        private DataFrameBulk currentBulk;
        private DrawThreadState drawThreadState;
        private final Lock drawThreadStateLock = new ReentrantLock();
        private final Condition drawThreadStateHasChanged = drawThreadStateLock.newCondition();

        public DrawView(Context context) {
            super(context);
            getHolder().addCallback(this);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.d(LOG_TAG, "surfaceChanged method was call");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(LOG_TAG, "DrawView created, transmission state = " + state);
            setRequestedOrientation(screenOrientation);
            assert !state.equals(TransmissionState.CREATING) && encodingController != null;

            // cancel because of params loss
            // TODO: support restoring = save TransmitterActivity fields via ViewModel
            if (!state.equals(TransmissionState.INITIAL)) {
                setResult(MainActivity.TransmissionResultCode.CANCELED.value, new Intent());
                finish();
                return;
            }

            assert params == null && drawThread == null;
            try {
                params = evaluateTransmissionParams();
            } catch (IllegalStateException exc) {
                Log.d(LOG_TAG, "Evaluating transmission params failed: " + exc.getMessage());
                state = TransmissionState.FAILED;
                assert params == null && drawThread == null;
                setResult(MainActivity.TransmissionResultCode.FAILED_TO_GET_TRANSMISSION_PARAMS.value, new Intent());
                finish();
                return;
            }
            encodingController.setTransmissionParameters(params.getColorFrameSize(), FRAMES_PER_BULK);
            assert params != null;
            Log.d(LOG_TAG, "Transmissions params: " + params.toString());

            // start transmission
            drawThread = new DrawThread(getHolder());
            controllerThread = new TransmissionControllerThread();
            state = TransmissionState.STARTED;
            drawThreadState = DrawThreadState.WAIT_FOR_BULK;
            drawThread.start();
            controllerThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            if (state.equals(TransmissionState.STARTED) || state.equals(TransmissionState.FINISHED)) {
                assert drawThread != null && controllerThread != null;
                drawThreadStateLock.lock();
                try {
                    drawThreadState = DrawThreadState.FINISH;
                    drawThread.interrupt();
                } finally {
                    drawThreadStateLock.unlock();
                }
                while (retry) {
                    try {
                        controllerThread.join();
                        drawThread.join();
                        retry = false;
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            Log.d(LOG_TAG, "DrawView destroyed");
        }

        private class TransmissionControllerThread extends Thread {

            @Override
            public void run() {
                byte[] buffer = new byte[1000];
                final long uniqueTransmitterKey = new Random().nextLong();
                Log.d(LOG_TAG, "Unique transmitter key generated = " + uniqueTransmitterKey);
                long uniqueReceiverKey = runPairing(uniqueTransmitterKey, buffer);
                if (uniqueReceiverKey == -1) {
                    return;
                }

                while (true) {
                    long[] bulkChecksums;
                    int bulkIndex;
                    drawThreadStateLock.lock();
                    try {
                        currentBulk = encodingController.getNextBulk();
                        bulkChecksums = currentBulk.getChecksums().clone();
                        bulkIndex = encodingController.getBulkIndex();
                        if (currentBulk == null) {
                            state = TransmissionState.FINISHED;
                            try {
                                final int blockingSendTimeout = 2;
                                communicator.blockingSend(CommunicationProtocol.TransmitterMessage.createSuccessfullyFinishedMessage(uniqueTransmitterKey).toByteArray(), blockingSendTimeout);
                            } catch (IOException ioException) {
                                Log.d(LOG_TAG, "Blocking send of finish-message IOException: " + ioException.getMessage());
                            }
                            setResult(MainActivity.TransmissionResultCode.SUCCEED.value, new Intent());
                            finish();
                            return;
                        }
                        Log.d(LOG_TAG, "Set current bulk: index #" + bulkIndex);
                        if (!drawThreadState.equals(DrawThreadState.DRAW_BULK)) {
                            drawThreadState = DrawThreadState.DRAW_BULK;
                            Log.d(LOG_TAG, "Changed draw thread state to DRAW_BULK");
                            drawThreadStateHasChanged.signal();
                        }
                    } catch (EncodingException encodingException) {
                        throw new RuntimeException("Encoding controller failed", encodingException);
                    } finally {
                        drawThreadStateLock.unlock();
                    }

                    if(!sendBulkInfoAndWaitForReceiverResponse(uniqueTransmitterKey, uniqueReceiverKey, bulkIndex, bulkChecksums, buffer)) {
                        return;
                    }
                }
            }

            private long runPairing(long uniqueTransmitterKey, byte[] buffer) {
                final long fileToSendSize = 100;
                final int maxPairingAttempts = 3;
                CommunicationProtocol.HelloMessage helloMessageToSend = CommunicationProtocol.HelloMessage.create(uniqueTransmitterKey, fileToSendSize);
                Log.d(LOG_TAG, "Start pairing");
                for (int i = 0; i < maxPairingAttempts; i++) {
                    Log.d(LOG_TAG, "Pairing attempt #" + i);
                    try {
                        final int blockingSendTimeout = 2;
                        communicator.blockingSend(helloMessageToSend.toByteArray(), blockingSendTimeout);
                    } catch (IOException ioException) {
                        Log.d(LOG_TAG, "Blocking send of hello message IOException: " + ioException.getMessage());
                        continue;
                    }
                    Log.d(LOG_TAG, "Hello message was successfully sent");
                    final int blockingReceiveTimeout = 5;
                    CommunicationProtocol.HelloMessage receivedHelloMessage;
                    try {
                        receivedHelloMessage = CommunicationProtocol.HelloMessage.parseFromByteArray(buffer, communicator.blockingReceive(buffer, blockingReceiveTimeout));
                    } catch (IOException ioException) {
                        Log.d(LOG_TAG, "Blocking receive of hello message IOException: " + ioException.getMessage());
                        continue;
                    }
                    Log.d(LOG_TAG, "Hello message was successfully received");
                    assert receivedHelloMessage != null;
                    assert receivedHelloMessage.fileToSendSize == fileToSendSize;
                    Log.d(LOG_TAG, "Pairing succeed! Unique receiver key = " + receivedHelloMessage.uniqueTransmissionKey);
                    return receivedHelloMessage.uniqueTransmissionKey;
                }
                setResult(MainActivity.TransmissionResultCode.PAIRING_FAILED.value, new Intent());
                finish();
                return -1;
            }

            private boolean sendBulkInfoAndWaitForReceiverResponse(long uniqueTransmitterKey, long uniqueReceiverKey, int bulkIndex, long[] bulkChecksums, byte[] buffer) {
                final int maxSendBulkInfoAttempts = 5;
                final int blockingSendTimeout = 2;
                final int blockingReceiveTimeout = 5;
                CommunicationProtocol.TransmitterMessage bulkInfoMessage =
                        CommunicationProtocol.TransmitterMessage.createInProgressMessage(
                                uniqueTransmitterKey,
                                bulkIndex,
                                bulkChecksums,
                                params);
                Log.d(LOG_TAG, "Start sending bulk info message:\n" + bulkInfoMessage);
                for (int i = 0; i < maxSendBulkInfoAttempts; i++) {
                    try {
                        communicator.blockingSend(bulkInfoMessage.toByteArray(), blockingSendTimeout);
                    } catch (IOException ioException) {
                        Log.d(LOG_TAG, "Blocking send of bulk info message IOException: " + ioException.getMessage());
                        continue;
                    }
                    Log.d(LOG_TAG, "Bulk info message was successfully sent");
                    try {
                        CommunicationProtocol.ReceiverMessage receiverMessage = CommunicationProtocol.ReceiverMessage.parseFromByteArray(uniqueReceiverKey, buffer, communicator.blockingReceive(buffer, blockingReceiveTimeout));
                        Log.d(LOG_TAG, "Received success-message from receiver: " + receiverMessage);
                        if (receiverMessage == null) { // TODO:  skip messages with other key
                            continue;
                        }
                        assert receiverMessage.bulkIndex == bulkIndex;
                        return true;
                    } catch (IOException ioException) {
                        Log.d(LOG_TAG, "Blocking send of bulk info message IOException: " + ioException.getMessage());
                    }
                }
                setResult(MainActivity.TransmissionResultCode.FAILED_TO_SEND_BULK.value, new Intent());
                finish();
                return false;
            }

        }

        private class DrawThread extends Thread {

            private final SurfaceHolder surfaceHolder;

            private final Paint paint;

            public DrawThread(SurfaceHolder surfaceHolder) {
                this.surfaceHolder = surfaceHolder;
                paint = new Paint();
            }

            @Override
            public void run() {
                while (true) { // change bulk cycle
                    DataFrameBulk bulk;
                    drawThreadStateLock.lock();
                    try {
                        try {
                            while (drawThreadState.equals(DrawThreadState.WAIT_FOR_BULK)) {
//                                setContentView(R.layout.setting_up_communication);
                                drawThreadStateHasChanged.await();
//                                setContentView(DrawView.this);
                            }
                        } catch (InterruptedException ignored) {
                        }
                        if (drawThreadState.equals(DrawThreadState.FINISH)) {
                            return;
                        }
                        bulk = currentBulk.clone();
                    } finally {
                        drawThreadStateLock.unlock();
                    }
                    long oneFrameDelayInNanos = params.getOneFrameDelayInNanos();
                    Log.d(LOG_TAG, "Bulk # " + bulk.getBulkIndex() + " is ready to be sent");
                    Log.d(LOG_TAG, "Encoding controller info: " + encodingController.getInfo());

                    while (true) { // draw bulk cycle
                        drawAndPostColorFrame(bulk.getNextDataFrame());
                        try {
                            TimeUnit.NANOSECONDS.sleep(oneFrameDelayInNanos);
                        } catch (InterruptedException stopDrawingBulk) {
                            break;
                        }
                    }
                }
            }

            private void drawAndPostColorFrame(int[] colors) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas == null)
                        return;
                    canvas.drawColor(Color.BLACK);
                    canvas.translate(params.leftTopOfGrid.x, params.leftTopOfGrid.y);
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
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
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
        }

        @SuppressWarnings("RedundantCast")
        private TransmissionParams evaluateTransmissionParams() throws IllegalStateException {
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
                throw new IllegalStateException("receiver is unable to read maxUnitSize grid");
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
            // center units grid
            PointF leftTopPointOfGrid = new PointF((frameBounds.width() - cols * unitSize) / 2f, (frameBounds.height() - rows * unitSize) / 2f);
            final int framesPerSecond = 10; // TODO: evaluate this constant too
            return new TransmissionParams(unitSize, rows, cols, leftTopPointOfGrid, framesPerSecond);
        }

        private boolean testReceiverAbilityToRead(int unitSize) {
            // TODO: add test messaging with receiver
            return unitSize >= 40; // mock for now
        }
    }

    private enum TransmissionState {
        CREATING, // generatorFactory == null
        INITIAL, // generatorFactory != null, params == null, drawThread == null, controllerThread == null
        STARTED, // generatorFactory != null, params != null, drawThread != null, controllerThread != null
        FINISHED, // happy path transmission is finished: still generatorFactory != null, params != null, drawThread != null, controllerThread != null
        FAILED // after: now transmission can fail only at evaluating params => generatorFactory != null, params == null, drawThread == null, , controllerThread == null
    }

    private enum DrawThreadState {
        WAIT_FOR_BULK,
        DRAW_BULK,
        FINISH
    }
}