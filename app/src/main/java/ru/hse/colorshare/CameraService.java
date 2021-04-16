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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService {

    private static final String TAG = "cameraServiceLog";

    public final CameraManager manager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CaptureRequest.Builder captureRequestBuilder;

    private final Activity startingActivity;
    private final TextureView previewView;
    private final ImageStreamHandler imageStreamHandler;

    public boolean isCameraOpen() {
        return isCameraOpen.get();
    }

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final AtomicBoolean isCameraOpen;

    private Size calculateMaxThroughputSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
        Size[] sizes = config.getOutputSizes(/*previewView.getSurfaceTexture().getClass()*/ ImageFormat.JPEG);
        assert sizes != null;
        Log.d(TAG, Arrays.toString(sizes));
        for (Size size : sizes) {
            double maxTheoreticalFps = 1e9 / config.getOutputMinFrameDuration(previewView.getSurfaceTexture().getClass(), size);
            double sizeEvaluation = size.getHeight() * size.getWidth() * maxTheoreticalFps;
            if (result == null || resultSizeEvaluation < sizeEvaluation) {
                result = size;
                resultSizeEvaluation = sizeEvaluation;
            }
        }
        assert result != null; // ! studio told to
        Log.d(TAG, "chosen size: " + result.getWidth() + "x" + result.getHeight() + " with eval=" + resultSizeEvaluation);
        return result;
    }

    // modified googlearchive way to choose size
    private static Size chooseOptimalSizeMod(Size[] choices, int textureViewWidth,
                                             int textureViewHeight, Size aspectRatio) {
        Size result;

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() * w == option.getWidth() * h) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            result = Collections.min(bigEnough, (s1, s2) -> s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
        } else if (notBigEnough.size() > 0) {
            result = Collections.max(notBigEnough, (s1, s2) -> s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
        } else {
            throw new IllegalStateException("couldn't find any suitable preview size");
        }
        Log.d(TAG, "big enough = " + Arrays.toString(bigEnough.toArray()));
        Log.d(TAG, "not big enough = " + Arrays.toString(notBigEnough.toArray()));
        Log.d(TAG, "chosen size = " + result);
        return result;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;

            try {
                // get char-cs for used camera
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                StreamConfigurationMap configurations = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (!StreamConfigurationMap.isOutputSupportedFor(previewView.getSurfaceTexture().getClass())) {
                    throw new IllegalArgumentException("camera cannot output to SurfaceTexture");
                }

                Size preferredSize = chooseOptimalSizeMod(configurations.getOutputSizes(SurfaceTexture.class),
                        previewView.getWidth(), previewView.getHeight(),
                        calculateMaxThroughputSize(configurations));

                Log.d(TAG, "orientation=" + sensorOrientation);
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    // swap dimensions
                    preferredSize = new Size(preferredSize.getHeight(), preferredSize.getWidth());
                    Log.d(TAG, "swapped dimensions");
                }

                resizePreviewViewToRatio(preferredSize);

                // create CaptureRequest
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // ! record vs preview?
                // set capture request flags
                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 97); // !
//                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                Surface previewSurface = new Surface(previewView.getSurfaceTexture());
                previewView.getSurfaceTexture().setDefaultBufferSize(preferredSize.getWidth(), preferredSize.getHeight());
                captureRequestBuilder.addTarget(previewSurface);

                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(previewSurface);

                // submit a capture request by creating a session
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
                session.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//                        Log.d(logTag, "capture completed");
                        super.onCaptureCompleted(session, request, result);
                    }

                    @Override
                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
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

    public void resizePreviewViewToRatio(Size size) {
        int w = previewView.getWidth();
        int h = previewView.getHeight();
        double scaleByWidth = (double) w / size.getWidth();
        double scaleByHeight = (double) h / size.getHeight();
        if (scaleByWidth * size.getHeight() < h) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByWidth), (int) (size.getHeight() * scaleByWidth)));
        } else if (scaleByHeight * size.getWidth() < w) {
            previewView.setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByHeight), (int) (size.getHeight() * scaleByHeight)));
        } else {
            throw new AssertionError("unscalable size?");
        }
    }

    public CameraService(CameraManager manager, Activity startingActivity, TextureView previewView) {
        this.manager = manager;
        this.cameraDevice = null;
        cameraId = null;
        isCameraOpen = new AtomicBoolean(false);

        this.previewView = previewView;

        this.startingActivity = startingActivity;
        this.imageStreamHandler = ImageStreamHandler.getInstance();
    }

    public void tryOpenCamera(String cameraId) {
        closeCamera();
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
            cameraId = null;
        }
    }

}