package ru.hse.colorshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button receiveButton = findViewById(R.id.receive_button);
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryStartReceiveActivity();
            }
        });
    }

    private void tryStartReceiveActivity() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 55555); // !
        } else {
            startReceiveActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 55555) { // !
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startReceiveActivity();
            } else {
                throw new IllegalStateException("couldn't get camera permission");
            }
        }
    }

    private void startReceiveActivity() {
        Intent openReceiverActivityIntent = new Intent(MainActivity.this, ReceiverCameraActivity.class);
        startActivity(openReceiverActivityIntent);
    }

}