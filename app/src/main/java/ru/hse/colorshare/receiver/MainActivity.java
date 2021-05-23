package ru.hse.colorshare.receiver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.Toast;

import ru.hse.colorshare.R;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button receiveButton = findViewById(R.id.receive_button);
        receiveButton.setOnClickListener(v -> {
            Intent openReceiverActivityIntent = new Intent(MainActivity.this, ReceiverCameraActivity.class);
            startActivity(openReceiverActivityIntent);
        });
    }

}