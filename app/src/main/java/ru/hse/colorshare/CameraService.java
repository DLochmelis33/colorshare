package ru.hse.colorshare;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService {
    private static final String logTag = "cameraServiceLog";

    public final CameraManager manager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private SurfaceTexture previewSurfaceTexture;
    private CaptureRequest.Builder cameraRequestBuilder;

    private final Activity startingActivity;
    private final TextureView previewView;
    private final ImageStreamHandler imageStreamHandler;

    public boolean isCameraOpen() {
        return isCameraOpen.get();
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final AtomicBoolean isCameraOpen;

    private Size calculateMaxThroughputSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
        Size[] sizes = config.getOutputSizes(previewSurfaceTexture.getClass());
        assert sizes != null;
        Log.d(logTag, Arrays.toString(sizes));
        for (Size size : sizes) {
            double maxTheoreticalFps = 1e9 / config.getOutputMinFrameDuration(previewSurfaceTexture.getClass(), size);
            double sizeEvaluation = size.getHeight() * size.getWidth() * maxTheoreticalFps;
            if (result == null || resultSizeEvaluation < sizeEvaluation) {
                result = size;
                resultSizeEvaluation = sizeEvaluation;
            }
        }
        assert result != null; // ! studio told to
        Log.d(logTag, "chosen size: " + result.getWidth() + "x" + result.getHeight() + " with eval=" + resultSizeEvaluation);
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

                if (!StreamConfigurationMap.isOutputSupportedFor(previewSurfaceTexture.getClass())) {
                    throw new IllegalArgumentException("camera cannot output to SurfaceTexture");
                }

                Size preferredSize = calculateMaxThroughputSize(configurations);
                resizePreviewViewToRatio(preferredSize);

                // create CaptureRequest
                cameraRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // ! record vs preview?
                Surface previewSurface = new Surface(previewSurfaceTexture);
                cameraRequestBuilder.addTarget(previewSurface);
                // set capture request flags
                cameraRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 97); // !
//                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                cameraRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
//                CaptureRequest request = builder.build();

                // submit a capture request by creating a session
                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(previewSurface);

                // ! deprecated (api 30), but no other option (api 23)
                cameraDevice.createCaptureSession(surfacesList, stateCallback, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("camera access exception");
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
//                throw new RuntimeException("wtf"); // !
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            throw new RuntimeException("camera error, code " + error);
        }
    };

    private final CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                // "at the maximum rate possible"
                session.setRepeatingRequest(cameraRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        Log.d(logTag, "capture completed");
                        super.onCaptureCompleted(session, request, result);
                    }

                    @Override
                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                        Log.d(logTag, "capture sequence completed");
                        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                    }
                }, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            throw new IllegalStateException("camera configuring failed");
        }
    };

    private void resizePreviewViewToRatio(Size size) {
        Rect screenSizeRect = new Rect();
        startingActivity.getWindowManager().getDefaultDisplay().getRectSize(screenSizeRect);
        double scaleByWidth = (double) screenSizeRect.width() / size.getWidth();
        double scaleByHeight = (double) screenSizeRect.height() / size.getHeight();
        if (scaleByWidth * size.getHeight() < screenSizeRect.height()) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByWidth), (int) (size.getHeight() * scaleByWidth)));
        } else if (scaleByHeight * size.getWidth() < screenSizeRect.width()) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByHeight), (int) (size.getHeight() * scaleByHeight)));
        } else {
            throw new AssertionError("unscalable size?");
        }
    }

    // ! copypasted from googlearchive demo camera app
    private void configureTransform(Size preferredSize) {
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();
        int rotation = startingActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, preferredSize.getHeight(), preferredSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / preferredSize.getHeight(),
                    (float) viewWidth / preferredSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        previewView.setTransform(matrix);
    }

    public CameraService(CameraManager manager, Activity startingActivity, TextureView previewView) {
        this.manager = manager;
        this.cameraDevice = null;
        cameraId = null;
        isCameraOpen = new AtomicBoolean(false);

        this.previewView = previewView;
        previewSurfaceTexture = null;

        this.startingActivity = startingActivity;
        this.imageStreamHandler = ImageStreamHandler.getInstance();
    }

    public void tryOpenCamera(String cameraId, SurfaceTexture previewTexture) {
        closeCamera();
        this.previewSurfaceTexture = previewTexture;
        try {
            if (ActivityCompat.checkSelfPermission(startingActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("was camera permission revoked?");
            }
            this.cameraId = cameraId;
            manager.openCamera(cameraId, cameraStateCallback, null); // !
            isCameraOpen.set(true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("couldn't open camera " + cameraId);
        }
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            isCameraOpen.set(false);
            cameraDevice.close();
            cameraDevice = null;
            previewSurfaceTexture = null;
            cameraId = null;
        }
    }

}