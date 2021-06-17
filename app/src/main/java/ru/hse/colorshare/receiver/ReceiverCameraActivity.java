package ru.hse.colorshare.receiver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.R;
import ru.hse.colorshare.receiver.util.CameraException;

public class ReceiverCameraActivity extends AppCompatActivity {

    private static final String TAG = "ReceiverCameraActivity";

    private CameraService cameraService;
    private FrameOverlayView frameOverlayView;
    private ActivityResultLauncher<Intent> fileCreateResultLauncher;
    private ReceiverController receiverController;

    public Handler getReceivingStatusHandler() {
        return receivingStatusHandler;
    }

    private Handler receivingStatusHandler;
    private final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            // TODO: status
            Log.e(TAG, Log.getStackTraceString(e));
            String toastMessage = "Sorry, something went wrong.";
            if (e instanceof CameraException) {
                toastMessage = "Sorry, something went wrong with the camera.";
            }
            // ! for some reason doesn't show; probably bc of Context being destroyed with the activity?
            Toast.makeText(ReceiverCameraActivity.this.getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
            ReceiverCameraActivity.this.finish();
        }
    };

    private static final int cameraPermissionRequestCode = 55555; // !

    public void callFileCreate() {
        Intent intent = new ActivityResultContracts.CreateDocument().createIntent(getApplicationContext(), null);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        fileCreateResultLauncher.launch(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileCreateResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                ReceiverController.onFileCreateCallback
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, cameraPermissionRequestCode);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Toast.makeText(this, "Camera permission is required, please grant it.", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    private void init() {
        setContentView(R.layout.activity_reciever_camera);
        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);

        // android-style assert
        if (BuildConfig.DEBUG && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new AssertionError("expected camera permission");
        }

        CameraOverlaidView cameraTextureView = findViewById(R.id.cameraTextureView);
        frameOverlayView = findViewById(R.id.frameOverlayView);
        cameraTextureView.setOverlayView(frameOverlayView);
        frameOverlayView.setUnderlyingView(cameraTextureView);

//        TextView dummyTextView = findViewById(R.id.dummyReadingStatusTextView);
        cameraService = new CameraService((CameraManager) getSystemService(Context.CAMERA_SERVICE), this, cameraTextureView, exceptionHandler);

        receivingStatusHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // TODO: more info
                frameOverlayView.setExtras((Bitmap) msg.obj);
            }
        };

        String cameraId = chooseCamera();
        if (cameraId == null) {
            throw new IllegalStateException("couldn't get a suitable camera");
        }

        cameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                cameraService.tryOpenCamera(cameraId);
                receiverController.start();
                // ! debug
//                ImageProcessor.getInstance().setGridSize(30, 50);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.d(TAG, "texture destroyed");
                cameraService.closeCamera();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });

        try {
            receiverController = new ReceiverController(this, exceptionHandler);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraService.closeCamera();
        ImageProcessor.getInstance().shutdown();
    }

    // returns a camera that 1) faces backwards and is not monochrome 2) is of largest sensor area
    private String chooseCamera() {
        try {
            String bestCameraId = null;
            CameraManager manager = cameraService.manager;

            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);

                boolean isFacingBack = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK;
                boolean isMono = false;
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) != null
                        && cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) == CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO) {
                    isMono = true;
                }
                if (isFacingBack && !isMono) {
                    if (bestCameraId == null) {
                        bestCameraId = cameraId;
                    }
                    Comparator<String> cameraIdByResolutionComparator = new Comparator<String>() {
                        private long convertIdToResolution(String id) {
                            CameraCharacteristics cc;
                            try {
                                cc = manager.getCameraCharacteristics(id);
                            } catch (CameraAccessException e) {
                                throw new IllegalArgumentException("invalid camera id", e);
                            }
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