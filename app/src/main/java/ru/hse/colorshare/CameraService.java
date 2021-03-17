package ru.hse.colorshare;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService {
    public final CameraManager manager;

    private CameraDevice cameraDevice;
    private final Activity startingActivity;
    private String cameraId;
    private SurfaceTexture previewSurfaceTexture;
    private final ImageStreamHandler imageStreamHandler;

    private static final String logTag = "cameraServiceLog";

    public boolean isCameraOpen() {
        return isCameraOpen.get();
    }

    private final AtomicBoolean isCameraOpen;

    private Size calculateMaxThroughputSize(StreamConfigurationMap config) {
        Size result = null;
        double resultSizeEvaluation = 0;
//        Size[] sizes = config.getOutputSizes(outputSurfaceHolder.getSurface().getClass());
        Size[] sizes = config.getOutputSizes(Surface.class);
        assert sizes != null;
        Log.d(logTag, Arrays.toString(sizes));
        for (Size size : sizes) {
            Log.d(logTag, size.getWidth() + "x" + size.getHeight());
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

//                Size preferredSize = calculateMaxThroughputSize(configurations);
//                outputSurfaceHolder.setFixedSize(preferredSize.getWidth(), preferredSize.getHeight());

                // create CaptureRequest
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // ! record vs preview?
                Surface previewSurface = new Surface(previewSurfaceTexture);
                builder.addTarget(previewSurface);
                // set capture request flags
                builder.set(CaptureRequest.JPEG_QUALITY, (byte) 97); // !
                builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF); // disable some post-processing related to edges of objects
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                CaptureRequest request = builder.build();

                // submit a capture request by creating a session
                ArrayList<Surface> surfacesList = new ArrayList<>();
                surfacesList.add(previewSurface);
                // ! deprecated !
                cameraDevice.createCaptureSession(surfacesList, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            // "at the maximum rate possible"
                            session.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
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
                }, null);

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

    public CameraService(CameraManager manager, Activity startingActivity, ImageStreamHandler imageStreamHandler) {
        this.manager = manager;
        this.cameraDevice = null;
        this.startingActivity = startingActivity;
        this.previewSurfaceTexture = null;
        isCameraOpen = new AtomicBoolean(false);
        this.imageStreamHandler = imageStreamHandler;
        cameraId = null;
    }

    public void tryOpenCamera(String cameraId, SurfaceTexture previewSurfaceTexture) {
        closeCamera();
        this.previewSurfaceTexture = previewSurfaceTexture;
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