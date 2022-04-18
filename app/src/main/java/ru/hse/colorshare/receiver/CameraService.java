package ru.hse.colorshare.receiver;


import android.Manifest;
import android.app.Activity;
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
import java.util.Collections;
import java.util.List;

import ru.hse.colorshare.BuildConfig;

public class CameraService {

    private static final String TAG = "cameraServiceLog";

    public final CameraManager manager;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CaptureRequest.Builder captureRequestBuilder;
    public static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;

    private final ReceiverCameraActivity startingActivity;
    private final TextureView previewView;
    private ImageReader imageReader;

    private long lastFrameTime = 0;
    private long frameTimeSum = 0;
    private int frameCount = 0;

    public CameraService(CameraManager manager, ReceiverCameraActivity startingActivity, TextureView previewView) {
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
                throw new IllegalStateException("was camera permission revoked?");
            }
            this.cameraId = cameraId;
            manager.openCamera(cameraId, cameraStateCallback, null); // !
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("couldn't open camera " + cameraId);
        }
    }

    private Size calculateMaxThroughputSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
        Size[] sizes = config.getOutputSizes(IMAGE_FORMAT);
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
        Log.d(TAG, "maxthroughput size: " + result.getWidth() + "x" + result.getHeight() + " with eval=" + resultSizeEvaluation);
        return result;
    }

    /* modified googlearchive way to choose size
     *
     * ---------------- DO NOT TOUCH ----------------
     */
    private Size chooseOptimalSizeMod(Size[] choices, int textureViewWidth,
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
        if (bigEnough.size() > 0) {
            result = Collections.min(bigEnough, (s1, s2) -> s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
        } else if (notBigEnough.size() > 0) {
            result = Collections.max(notBigEnough, (s1, s2) -> s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
        } else {
            throw new IllegalStateException("couldn't find any suitable preview size");
        }
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

                // choose size of output surface
                Size preferredSize = chooseOptimalSizeMod(configurations.getOutputSizes(SurfaceTexture.class),
                        previewView.getWidth(), previewView.getHeight(),
                        calculateMaxThroughputSize(configurations));
//                Size preferredSize = calculateMaxThroughputSize(configurations); // ! DO NOT
//                preferredSize = new Size(1280, 720); // magic numbers

                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.d(TAG, "sensor orientation = " + sensorOrientation);
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    // swap dimensions
                    preferredSize = new Size(preferredSize.getHeight(), preferredSize.getWidth());
                }
                Log.d(TAG, "chosen size = " + preferredSize);
                resizePreviewViewToRatio(preferredSize);

                imageReader = ImageReader.newInstance(preferredSize.getWidth(), preferredSize.getHeight(), IMAGE_FORMAT, 2); // 2 is minimum required for acquireLatestImage

                // create CaptureRequest
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // record vs preview?
//                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 97); // !
//                captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN); // might be better than AF_MODE_CONTINUOUS_PICTURE

                Surface previewSurface = new Surface(previewView.getSurfaceTexture());
                previewView.getSurfaceTexture().setDefaultBufferSize(preferredSize.getWidth(), preferredSize.getHeight());

                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(previewSurface);
                captureRequestBuilder.addTarget(previewSurface);
                surfacesList.add(imageReader.getSurface());
                captureRequestBuilder.addTarget(imageReader.getSurface());

                // submit a capture request by creating a session
                // deprecated (api 30), but no other option (api 23)
                cameraDevice.createCaptureSession(surfacesList, stateCallback, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("camera access exception");
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
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
                        super.onCaptureCompleted(session, request, result);

                        if (imageReader == null) {
                            // can happen while closing
                            return;
                        }

                        Image image = imageReader.acquireLatestImage();
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
                        byte[] nv21 = new byte[ySize + vuSize]; // ! ?
                        yBuffer.get(nv21, 0, ySize);
                        vuBuffer.get(nv21, ySize, vuSize);
                        image.close(); // ASAP

                        ImageProcessor.process(new ImageProcessor.Task(nv21, width, height, startingActivity.getHints(), startingActivity.getReceivingStatusHandler()));

                        if(lastFrameTime == 0) {
                            lastFrameTime = System.currentTimeMillis();
                            return;
                        }
                        long newFrameTime = System.currentTimeMillis();
                        long delta = newFrameTime - lastFrameTime;
                        frameTimeSum += delta;
                        frameCount++;
                        Log.d("FPS", String.valueOf(1.0 * frameTimeSum / frameCount));
                        lastFrameTime = newFrameTime;
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