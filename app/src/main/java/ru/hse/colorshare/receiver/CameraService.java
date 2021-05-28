package ru.hse.colorshare.receiver;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import ru.hse.colorshare.BuildConfig;

public class CameraService {

    private static final String TAG = "cameraServiceLog";

    public static class CameraException extends IllegalStateException {
        public CameraException() {
        }

        public CameraException(String s) {
            super(s);
        }

        public CameraException(String message, Throwable cause) {
            super(message, cause);
        }

        public CameraException(Throwable cause) {
            super(cause);
        }
    }

    public final CameraManager manager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CaptureRequest.Builder captureRequestBuilder;
    public static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;

    private final ReceiverCameraActivity startingActivity;
    private final CameraOverlaidView previewView;
    private ImageReader imageReader;
    private Surface previewSurface;
    private final Paint previewPaint = new Paint();

    private long lastFrameTime = 0;
    private long frameTimeSum = 0;
    private int frameCount = 0;
    private final Queue<Long> timesQueue = new LinkedList<>();

    public CameraService(CameraManager manager, ReceiverCameraActivity startingActivity, CameraOverlaidView previewView) {
        this.manager = manager;
        this.cameraDevice = null;
        cameraId = null;
        this.previewView = previewView;
        this.startingActivity = startingActivity;
    }

    public void tryOpenCamera(String cameraId) {
        closeCamera();
        try {
            if (ActivityCompat.checkSelfPermission(startingActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new CameraException("no camera permission");
            }
            this.cameraId = cameraId;
            manager.openCamera(cameraId, cameraStateCallback, null); // !
        } catch (CameraAccessException e) {
            throw new CameraException("couldn't open camera " + cameraId, e);
        }
    }

    private Size calculatePreferredSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
        Size[] sizes = config.getOutputSizes(IMAGE_FORMAT);
        assert sizes != null;
        Log.d(TAG, Arrays.toString(sizes));
        for (Size size : sizes) {
            if(size.getWidth() > 2000) {
                continue; // ! filter too large ones
            }
            double maxTheoreticalFps = 1e9 / config.getOutputMinFrameDuration(previewView.getSurfaceTexture().getClass(), size);
            double sizeEvaluation = size.getHeight() * size.getWidth() * maxTheoreticalFps;
            if (result == null || resultSizeEvaluation < sizeEvaluation) {
                result = size;
                resultSizeEvaluation = sizeEvaluation;
            }
        }
        assert result != null; // ! IDE told to
        Log.d(TAG, "maxthroughput size: " + result + " with eval=" + resultSizeEvaluation);
        return result;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            ImageProcessor.getInstance().setCameraService(CameraService.this);

            try {
                // get char-cs for used camera
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap configurations = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (!StreamConfigurationMap.isOutputSupportedFor(SurfaceTexture.class)) {
                    throw new CameraException("camera cannot output to SurfaceTexture");
                }

                Size preferredSize = calculatePreferredSize(configurations);
//                preferredSize = new Size(1280, 720); // magic numbers

                // create ImageReader before rotating preferredSize - the image will be manually rotated later
                // ! maxImages = ?
                imageReader = ImageReader.newInstance(preferredSize.getWidth(), preferredSize.getHeight(), IMAGE_FORMAT, 25); // 2 is minimum required for acquireLatestImage

                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.w(TAG, "sensor orientation = " + sensorOrientation);
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    // swap dimensions
                    preferredSize = new Size(preferredSize.getHeight(), preferredSize.getWidth());
                }
                Log.w(TAG, "chosen size = " + preferredSize);
                previewView.resizeToRatio(preferredSize);

                // create CaptureRequest
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // record vs preview?
                captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // ! maybe AF_STATE_ACTIVE_SCAN ?

//                previewView.getSurfaceTexture().setDefaultBufferSize(preferredSize.getWidth(), preferredSize.getHeight());
                previewSurface = new Surface(previewView.getSurfaceTexture());

                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(imageReader.getSurface());
                captureRequestBuilder.addTarget(imageReader.getSurface());

                // submit a capture request by creating a session
                // deprecated (api 30), but no other option (api 23)
                cameraDevice.createCaptureSession(surfacesList, stateCallback, null);


            } catch (CameraAccessException e) {
                throw new CameraException(e);
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

                        System.gc();

                        Runtime runtime = Runtime.getRuntime();
                        long availHeapSize = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
                        if (availHeapSize <= 16 * 1024 * 1024) {
//                            Log.w(TAG, "image discarded due to memory lack");
                            return;
                        }

                        try (Image image = imageReader.acquireLatestImage()) {
                            if (image == null) {
//                            Log.v(TAG, "null image");
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
                            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                            ByteBuffer vuBuffer = image.getPlanes()[2].getBuffer();
                            int width = image.getWidth();
                            int height = image.getHeight();
                            int ySize = yBuffer.remaining();
                            int vuSize = vuBuffer.remaining();

                            byte[] nv21 = new byte[ySize + vuSize];
                            yBuffer.get(nv21, 0, ySize);
                            vuBuffer.get(nv21, ySize, vuSize);
//                            image.close(); // ASAP

//                        Log.d(TAG, "image width = " + width + ", height = " + height);
                            ImageProcessor.getInstance().process(new ImageProcessor.Task(nv21, width, height, startingActivity.getHints(),
                                    startingActivity.getReceivingStatusHandler(), startingActivity.getApplicationContext()));

                            // calculate frame rate
//                        if (lastFrameTime == 0) {
//                            lastFrameTime = System.currentTimeMillis();
//                            return;
//                        }
//                        long newFrameTime = System.currentTimeMillis();
//                        long delta = newFrameTime - lastFrameTime;
//                        lastFrameTime = newFrameTime;
//                        frameTimeSum += delta;
//                        frameCount++;
//                        timesQueue.add(delta);
//                        if (frameCount < 100) {
//                            return;
//                        }
//
//                        Long lastTime = timesQueue.poll();
//                        if(lastTime == null) {
//                            timesQueue.clear();
//                            frameCount = 0;
//                            frameTimeSum = 0;
//                            return;
//                        }
//                        frameTimeSum -= lastTime;
//                        frameCount--;
//                        Log.v("FPS", String.valueOf(1.0 * frameTimeSum / frameCount));

                        }
                        System.gc();
                    }

                    @Override
                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                    }
                }, null);

            } catch (CameraAccessException e) {
                throw new CameraException(e);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            throw new CameraException("camera configuring failed");
        }
    };

    // ! sync ?
    public void updatePreviewSurface(Bitmap bitmap) {

        startingActivity.runOnUiThread(() -> {
            Canvas canvas = previewSurface.lockHardwareCanvas();
            Matrix transform = new Matrix();
            transform.setScale((float) previewView.getWidth() / bitmap.getWidth(), (float) previewView.getHeight() / bitmap.getHeight());
            canvas.drawBitmap(bitmap, transform, previewPaint);
            previewSurface.unlockCanvasAndPost(canvas);
            previewView.invalidate();
            Log.d(TAG, "preview surface updated");
        });

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
    }

}