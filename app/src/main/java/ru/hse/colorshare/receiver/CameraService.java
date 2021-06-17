package ru.hse.colorshare.receiver;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.receiver.util.CameraException;
import ru.hse.colorshare.receiver.util.SlidingAverage;

public class CameraService {

    private static final String TAG = "cameraServiceLog";

    public final CameraManager manager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CaptureRequest.Builder captureRequestBuilder;
    public static final int IMAGE_FORMAT = ImageFormat.JPEG;

    private final ReceiverCameraActivity startingActivity;
    private final CameraOverlaidView previewView;
    private ImageReader imageReader;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    private long lastFrameTime = 0;
    private final int fpsWindowSize = 50;
    private final SlidingAverage fpsCounter = new SlidingAverage(fpsWindowSize);

    public CameraService(CameraManager manager, ReceiverCameraActivity startingActivity, CameraOverlaidView previewView, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.manager = manager;
        this.cameraDevice = null;
        cameraId = null;
        this.previewView = previewView;
        this.startingActivity = startingActivity;
        this.exceptionHandler = exceptionHandler;
    }

    public void tryOpenCamera(String cameraId) {
        closeCamera();
        try {
            if (ActivityCompat.checkSelfPermission(startingActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("was camera permission revoked?");
            }
            this.cameraId = cameraId;
            startBackgroundThread();
            manager.openCamera(cameraId, cameraStateCallback, null /* will modify UI in this thread */);
        } catch (CameraAccessException e) {
            throw new CameraException("couldn't open camera " + cameraId, e);
        }
    }

    private Size calculateMaxThroughputSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
        Size[] sizes = config.getOutputSizes(IMAGE_FORMAT);
        assert sizes != null;
        Log.d(TAG, Arrays.toString(sizes));
        for (Size size : sizes) {
            if (size.getWidth() > 2000) {
                continue; // too large sizes decrease fps
            }
            double maxTheoreticalFps = 1e9 / config.getOutputMinFrameDuration(SurfaceTexture.class, size);
            double sizeEvaluation = size.getHeight() * size.getWidth() * maxTheoreticalFps;
            if (result == null || resultSizeEvaluation < sizeEvaluation) {
                result = size;
                resultSizeEvaluation = sizeEvaluation;
            }
        }
        assert result != null; // ! studio told to
        Log.d(TAG, "maxthroughput size: " + result.getWidth() + "x" + result.getHeight() + " with eval=" + resultSizeEvaluation);
        return result;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;

            try {
                // get char-cs for used camera
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap configurations = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (!StreamConfigurationMap.isOutputSupportedFor(SurfaceTexture.class)) {
                    throw new IllegalStateException("camera cannot output to SurfaceTexture");
                }

                Log.d(TAG, "available sizes: " + Arrays.toString(configurations.getOutputSizes(SurfaceTexture.class)));

                // choose size of output surface
                Size preferredSize = calculateMaxThroughputSize(configurations);
//                Size preferredSize = new Size(256, 144); // ! debug

                // ----- THIS IS CURSED -----
//                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//                Log.w(TAG, "sensor orientation = " + sensorOrientation);
//                if (sensorOrientation == 90 || sensorOrientation == 270) {
//                    // swap dimensions
//                    preferredSize = new Size(preferredSize.getHeight(), preferredSize.getWidth());
//                }

                Log.d(TAG, "chosen size = " + preferredSize);

                resizePreviewViewToRatio(new Size(preferredSize.getHeight(), preferredSize.getWidth())); // ! ! !
                previewView.forceLayout();
                imageReader = ImageReader.newInstance(preferredSize.getWidth(), preferredSize.getHeight(), IMAGE_FORMAT, 2); // 2 is minimum required for acquireLatestImage

                SurfaceTexture previewSurfaceTexture = previewView.getSurfaceTexture();
                // ! order ?
                Surface previewSurface = new Surface(previewSurfaceTexture);
                previewSurfaceTexture.setDefaultBufferSize(preferredSize.getWidth(), preferredSize.getHeight());

                // create CaptureRequest
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // record vs preview?
                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(previewSurface);
                captureRequestBuilder.addTarget(previewSurface);
                surfacesList.add(imageReader.getSurface());
                captureRequestBuilder.addTarget(imageReader.getSurface());

//                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 97); // !
//                captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // ! maybe AF_STATE_ACTIVE_SCAN ?

//                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation);

                // submit a capture request by creating a session
                // deprecated (api 30), but no other option (api 23)
                cameraDevice.createCaptureSession(surfacesList, stateCallback, backgroundHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new CameraException("camera access exception", e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            throw new CameraException("camera error, code " + error);
        }
    };

    private final CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                // "at the maximum rate possible"
                session.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);

                        if (imageReader == null) {
                            // can happen while closing
                            return;
                        }

//                        Runtime runtime = Runtime.getRuntime();
//                        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
//                        if (availHeapSize <= 16 * 1024 * 1024) { // on my phone one image is around 4MB
//                            return;
//                        }

                        Image image = imageReader.acquireLatestImage();
                        if (image == null) {
                            Log.v(TAG, "null image");
                            return;
                        }
                        if (BuildConfig.DEBUG && image.getFormat() != IMAGE_FORMAT) {
                            throw new AssertionError("Image format is wrong");
                        }

                        // ! probably incompatible with continuous or active AF
//                        if (result.get(CaptureResult.CONTROL_AF_STATE) != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
//                            image.close();
//                            return;
//                        }

                        // copy image data and close image
                        ByteBuffer imgBuffer = image.getPlanes()[0].getBuffer();
                        int width = image.getWidth();
                        int height = image.getHeight();
                        int bytesSize = imgBuffer.remaining();
                        byte[] imgBytes = new byte[bytesSize];
                        imgBuffer.get(imgBytes, 0, bytesSize);
                        image.close(); // ASAP

                        ImageProcessor.process(new ImageProcessor.Task(imgBytes, width, height, previewView.getOverlayView().getCornersHints(),
                                startingActivity.getReceivingStatusHandler(), startingActivity.getApplicationContext()));

                        if (lastFrameTime == 0) {
                            lastFrameTime = System.currentTimeMillis();
                            return;
                        }
                        long newFrameTime = System.currentTimeMillis();
                        long delta = newFrameTime - lastFrameTime;
                        lastFrameTime = newFrameTime;
                        fpsCounter.addValue(delta);

                        Log.v("FPS", String.valueOf(1000.0 * fpsWindowSize / fpsCounter.getSum()));
                    }

                    @Override
                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                    }
                }, backgroundHandler);

            } catch (CameraAccessException e) {
                throw new CameraException(e);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            throw new CameraException("camera configuring failed");
        }
    };


    public void resizePreviewViewToRatio(Size size) {
        int w = previewView.getWidth();
        int h = previewView.getHeight();
        Log.w(TAG, "resizing: w was " + w + ", h was " + h);
        double scaleByWidth = (double) w / size.getWidth();
        double scaleByHeight = (double) h / size.getHeight();
        if (scaleByWidth * size.getHeight() <= h) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByWidth), (int) (size.getHeight() * scaleByWidth)));
        } else if (scaleByHeight * size.getWidth() <= w) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByHeight), (int) (size.getHeight() * scaleByHeight)));
        } else {
            throw new AssertionError("unscalable size?");
        }
        Log.w(TAG, "resizing: w new " + previewView.getWidth() + ", h new " + previewView.getHeight());
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
            cameraId = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (backgroundHandler != null && backgroundThread != null) {
            stopBackgroundThread();
        }
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.setUncaughtExceptionHandler(exceptionHandler);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            // ignore
            e.printStackTrace();
        }
    }

}