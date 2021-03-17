package ru.hse.colorshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;

import java.util.Arrays;
import java.util.Comparator;

public class ReceiverCameraActivity extends AppCompatActivity {

    private CameraService cameraService;
    private TextureView cameraTextureView; // ! will use to draw over camera image

    private static final String logTag = "ReceiverCameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciever_camera);

        // android-style assert
        if (BuildConfig.DEBUG && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new AssertionError("expected camera permission");
        }

        cameraTextureView = findViewById(R.id.cameraTextureView);
        cameraService = new CameraService((CameraManager) getSystemService(Context.CAMERA_SERVICE), this, ImageStreamHandler.getInstance());

        String cameraId = chooseCamera();
        if (cameraId == null) {
            throw new IllegalStateException("couldn't get a suitable camera");
        }

//        cameraTextureView.setSurfaceTexture(new SurfaceTexture(555)); // !
        cameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                cameraService.tryOpenCamera(cameraId, surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d(logTag, "texture size changed");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(logTag, "texture destroyed");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                Log.d(logTag, "texture updated");
            }
        });

    }

    // returns a camera that 1) is not monochrome 2) is of largest sensor area
    private String chooseCamera() {
        try {
            String bestCameraId = null;
            CameraManager manager = cameraService.manager;

            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                        && cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) != CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO) {

                    if (bestCameraId == null) {
                        bestCameraId = cameraId;
                    }
                    Comparator<String> cameraIdByResolutionComparator = new Comparator<String>() {
                        private long convertIdToResolution(String id) {
                            Size pixelArraySize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
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
                    if (cameraIdByResolutionComparator.compare(bestCameraId, cameraId) > 0) {
                        bestCameraId = cameraId;
                    }
                }

            }
            return bestCameraId;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("camera failure");
        }
    }
}