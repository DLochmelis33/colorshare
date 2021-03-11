package ru.hse.colorshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReceiverCameraActivity extends AppCompatActivity {

    private class CameraService {
        public final CameraManager manager;
        private CameraDevice cameraDevice = null;

        private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;

                try {
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // ! record -> preview?
                    builder.addTarget(cameraOutputSurface);
                    // ! set capture request flags
                    CaptureRequest request = builder.build();

                    ArrayList<Surface> patamushta = new ArrayList<>(); // !
                    patamushta.add(cameraOutputSurface);
                    cameraDevice.createCaptureSession(patamushta, new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(request, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
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
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                throw new RuntimeException("camera error, code " + error);
            }
        };

        public CameraService(CameraManager manager) {
            this.manager = manager;
        }

        public void openCamera(String cameraId) {
            closeCamera();
            try {
                if (ActivityCompat.checkSelfPermission(ReceiverCameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    throw new IllegalStateException("was camera permission revoked?");
                }
                manager.openCamera(cameraId, cameraStateCallback, null); // !
            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("couldn't open camera " + cameraId);
            }
        }

        public void closeCamera() {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }

    }

    private CameraService cameraService;
    public Surface cameraOutputSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciever_camera);


        // android-style assert
        if (BuildConfig.DEBUG && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new AssertionError("expected camera permission");
        }

        SurfaceView cameraSurfaceView = findViewById(R.id.cameraSurfaceView);
        cameraOutputSurface = cameraSurfaceView.getHolder().getSurface();
        cameraService = new CameraService((CameraManager) getSystemService(Context.CAMERA_SERVICE));

        String cameraId = chooseCamera();
        if (cameraId == null) {
            throw new IllegalStateException("couldn't get a suitable camera");
        }

        cameraService.openCamera(cameraId);
    }

    // returns a camera that 1) is not monochrome 2) is of largest sensor area
    private String chooseCamera() {
        try {
            String usedCameraId = null;
            CameraManager manager = cameraService.manager;

            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);

                // ! DEBUG
                System.out.println(cc.get(CameraCharacteristics.LENS_FACING));

                if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                        && cc.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) != CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO) {
                    if (usedCameraId == null) {
                        usedCameraId = cameraId;
                    }
                    Comparator<String> cameraIdResolutionComparator = new Comparator<String>() {
                        private long convertIdToResolution(String id) {
                            Size pixelArraySize = cc.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                            return (long) pixelArraySize.getHeight() * pixelArraySize.getWidth();
                        }

                        @Override
                        public int compare(String o1, String o2) {
                            long result = convertIdToResolution(o1) - convertIdToResolution(o2);
                            if (result > 0) {
                                return 1;
                            } else if (result < 0) {
                                return -1;
                            }
                            return 0;
                        }
                    };
                    if (cameraIdResolutionComparator.compare(usedCameraId, cameraId) > 0) {
                        usedCameraId = cameraId;
                    }
                }

            }
            return usedCameraId;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("camera failure");
        }
    }
}