package ru.hse.colorshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int cameraPermissionRequestCode = 55555; // !

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button receiveButton = findViewById(R.id.receive_button);
        receiveButton.setOnClickListener(v -> tryStartReceiveActivity());
    }

    private void tryStartReceiveActivity() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, cameraPermissionRequestCode);
        } else {
            startReceiveActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == cameraPermissionRequestCode) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startReceiveActivity();
            } else {
                Toast.makeText(this, "Camera permission is required, please grant it.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startReceiveActivity() {
        Intent openReceiverActivityIntent = new Intent(MainActivity.this, ReceiverCameraActivity.class);
        startActivity(openReceiverActivityIntent);
    }

}