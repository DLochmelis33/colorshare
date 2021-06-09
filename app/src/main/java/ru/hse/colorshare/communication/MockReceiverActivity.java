package ru.hse.colorshare.communication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;

import ru.hse.colorshare.R;
import ru.hse.colorshare.transmitter.TransmitterActivity;

public class MockReceiverActivity extends AppCompatActivity {

    private Communicator communicator;

    private static final String LOG_TAG = "ColorShare:receiver";

    @SuppressLint("Assert")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_receiver);

        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                TransmitterActivity.RequestCode.PERMISSION_REQUEST_RECORD_AUDIO.ordinal());
        assert checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        communicator = Communicator.getColorShareTransmitterSideCommunicator(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (communicator != null) {
            try {
                communicator.close();
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to close communicator");
            }
        }
    }

    public void onClickSendOK(View view) {
        Log.d(LOG_TAG, "Attempt to send OK");
    }

    public void onClickSendHello(View view) {
        Log.d(LOG_TAG, "Attempt to send hello");
    }
}