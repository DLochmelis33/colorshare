package ru.hse.colorshare.communication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.hse.colorshare.MainActivity;
import ru.hse.colorshare.R;

public class MockReceiverActivity extends AppCompatActivity {

    private ColorShareReceiverCommunicator communicator;

    private TextView textView;

    private static final String LOG_TAG = "ColorShare:receiver";

    @SuppressLint("Assert")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_receiver);
        textView = findViewById(R.id.textView);

        assert checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        communicator = ColorShareReceiverCommunicator.getInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (communicator != null) {
            communicator.shutdown();
        }
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    public void onClickReceiveBulkAndSendOK(View view) {
        Log.d(LOG_TAG, "Attempt to receive bulk and send OK");

        try {
            TransmitterMessage message = communicator.waitForMessage(60);
            Log.d(LOG_TAG, "Transmitter message received!");
            switch (message.getMessageType()) {
                case BULK_INFO:
                    Log.d(LOG_TAG, "Received bulk message");
                    textView.setText("Received bulk index: " + message.getBulkIndex());
                    break;
                case TRANSMISSION_SUCCEED:
                    Log.d(LOG_TAG, "Transmission succeed!");
                    textView.setText("Transmission succeed!");
                    return;
                case TRANSMISSION_CANCELED:
                    Log.d(LOG_TAG, "Transmission canceled");
                    textView.setText("Transmission canceled");
                    return;
            }
        } catch (TimeoutException timeoutException) {
            Log.d(LOG_TAG, "Receive bulk message timeout");
            return;
        }

        communicator.sendBulkReceivedMessage();
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    public void onClickRunPairing(View view) {
        Log.d(LOG_TAG, "Pairing started");
        final int maxPairingAttempts = 10;
        for (int i = 0; i < maxPairingAttempts; i++) {
            Log.d(LOG_TAG, "Pairing attempt #" + i);
            try {
                long transmitterId = communicator.waitForPairingRequest(6);
                communicator.bindPartnerId(transmitterId);
                Log.d(LOG_TAG, "Pairing request received from transmitter: " + transmitterId);
            } catch (TimeoutException ignored) {
                Log.d(LOG_TAG, "Pairing receive request attempt #" + i + " failed");
            }
        }
        for (int i = 0; i < maxPairingAttempts; i++) {
            communicator.sendPairingRequest();
            try {
                communicator.waitForPairingSucceedMessage(6);
                Log.d(LOG_TAG, "Pairing succeed!");
                return;
            } catch (TimeoutException ignored) {
                Log.d(LOG_TAG, "Pairing send request attempt #" + i + " failed");
            }
        }
        Log.d(LOG_TAG, "Pairing failed");
    }
}