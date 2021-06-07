package ru.hse.colorshare.receiver;

import android.Manifest;
import android.content.Context;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Comparator;

import ru.hse.colorshare.BuildConfig;
import ru.hse.colorshare.R;
import ru.hse.colorshare.receiver.util.RelativePoint;

public class ReceiverCameraActivity extends AppCompatActivity {

    private static final String TAG = "ReceiverCameraActivity";

    private CameraService cameraService;
    private TextView dummyTextView;
    private FrameOverlayView frameOverlayView;

    public Handler getReceivingStatusHandler() {
        return receivingStatusHandler;
    }

    private Handler receivingStatusHandler;

    private static final int cameraPermissionRequestCode = 55555; // !

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, cameraPermissionRequestCode);
        } else {
            init(savedInstanceState);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init(null);
            } else {
                Toast.makeText(this, "Camera permission is required, please grant it.", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    private void init(Bundle savedInstanceState) {
        setContentView(R.layout.activity_reciever_camera);

        // android-style assert
        if (BuildConfig.DEBUG && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw new AssertionError("expected camera permission");
        }

        CameraOverlaidView cameraTextureView = findViewById(R.id.cameraTextureView);
        frameOverlayView = findViewById(R.id.frameOverlayView);
        cameraTextureView.setOverlayView(frameOverlayView);
        frameOverlayView.setUnderlyingView(cameraTextureView);

        dummyTextView = findViewById(R.id.dummyReadingStatusTextView);
        cameraService = new CameraService((CameraManager) getSystemService(Context.CAMERA_SERVICE), this, cameraTextureView);

        receivingStatusHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
//                ColorExtractor.LocatorResult[] res = (ColorExtractor.LocatorResult[]) msg.obj;
//                String s = Arrays.toString(res);
//                dummyTextView.setText(s);

                frameOverlayView.setExtras((Bitmap) msg.obj);
                // drawing in this thread is too slow
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

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraService.closeCamera();
//        ImageProcessor.getInstance().EXECUTOR.shutdownNow();
    }

    public RelativePoint[] getHints() {
        return frameOverlayView.getCornersHints();
    }

    // returns a camera that 1) is not monochrome 2) is of largest sensor area
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