package ru.hse.colorshare.communication;

import androidx.annotation.NonNull;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ru.hse.colorshare.MainActivity;
import ru.hse.colorshare.R;
import ru.hse.colorshare.transmitter.TransmitterActivity;

public class MockReceiverActivity extends AppCompatActivity {

    private Communicator communicator;
    private long uniqueTransmitterKey;
    private long uniqueReceiverKey;

    private TextView textView;

    private static final String LOG_TAG = "ColorShare:receiver";

    @SuppressLint("Assert")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_receiver);
        textView = findViewById(R.id.textView);

        assert checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        communicator = Communicator.getColorShareReceiverSideCommunicator(this);
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

    @SuppressLint("Assert")
    public void onClickReceiveBulkAndSendOK(View view) {
        Log.d(LOG_TAG, "Attempt to receive bulk and send OK");

        byte[] buffer = new byte[1000];
        CommunicationProtocol.TransmitterMessage transmitterMessage;
        int bulkIndex = -1;
        final int maxReceiveAttempts = 5;
        for (int i = 0; i < maxReceiveAttempts; i++) {
            Log.d(LOG_TAG, "Receive attempt #" + i);
            final int blockingReceiveTimeout = 10;
            try {
                transmitterMessage = CommunicationProtocol.TransmitterMessage.parseFromByteArray(uniqueTransmitterKey, buffer, communicator.blockingReceive(buffer, blockingReceiveTimeout));
                assert transmitterMessage != null;
                switch (transmitterMessage.transmissionState) {
                    case IN_PROGRESS:
                        bulkIndex = transmitterMessage.bulkIndex;
                        textView.setText(String.valueOf(bulkIndex));
                        Log.d(LOG_TAG, "Transmitter message attempt #" + i + " was successfully received: " + transmitterMessage);
                        break;
                    case CANCELED:
                        Log.d(LOG_TAG, "Transmission cancelled");
                        Toast.makeText(getApplicationContext(), "Transmission cancelled, try again", Toast.LENGTH_SHORT).show();
                        return;
                    case SUCCESSFULLY_FINISHED:
                        Log.d(LOG_TAG, "Transmission successfully finished");
                        Toast.makeText(getApplicationContext(), "Transmission successfully finished", Toast.LENGTH_SHORT).show();
                        return;
                }
                break;
            } catch (IOException ioException) {
                Log.d(LOG_TAG, "Blocking receive of transmitter message attempt #" + i + " IOException: " + ioException.getMessage());
            }
            if (i == maxReceiveAttempts - 1) {
                Log.d(LOG_TAG, "Max receive attempts reached");
                Toast.makeText(getApplicationContext(), "Max receive attempts reached, try again", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // work
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        // response OK
        CommunicationProtocol.ReceiverMessage receiverMessage = CommunicationProtocol.ReceiverMessage.create(uniqueReceiverKey, bulkIndex);

        final int maxSendAttempts = 5;
        for (int i = 0; i < maxSendAttempts; i++) {
            Log.d(LOG_TAG, "Send OK attempt #" + i);
            try {
                final int blockingSendTimeout = 2;
                communicator.blockingSend(receiverMessage.toByteArray(), blockingSendTimeout);
                Log.d(LOG_TAG, "Receiver message attempt #" + i + " was successfully sent: " + receiverMessage);
                break;
            } catch (IOException ioException) {
                Log.d(LOG_TAG, "Blocking send of receiver message attempt #" + i + " IOException: " + ioException.getMessage());
            }
            if (i == maxSendAttempts - 1) {
                Log.d(LOG_TAG, "Max send attempts reached");
                Toast.makeText(getApplicationContext(), "Max send attempts reached, try again", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @SuppressLint("Assert")
    public void onClickReceiveAndSendHello(View view) {
        Log.d(LOG_TAG, "Attempt to receive and send hello");

        byte[] buffer = new byte[1000];
        int attempt = 0;
        CommunicationProtocol.HelloMessage receivedHelloMessage;
        while (true) {
            Log.d(LOG_TAG, "Receive attempt #" + attempt);
            final int blockingReceiveTimeout = 10;
            try {
                int receivedBytes = communicator.blockingReceive(buffer, blockingReceiveTimeout);
                receivedHelloMessage = CommunicationProtocol.HelloMessage.parseFromByteArray(buffer, receivedBytes);
                assert receivedHelloMessage != null;
                Log.d(LOG_TAG, "Hello message attempt #" + attempt + " was successfully received: " + receivedHelloMessage);
                break;
            } catch (IOException ioException) {
                Log.d(LOG_TAG, "Blocking receive of hello message attempt #" + attempt + " IOException: " + ioException.getMessage());
            }
            attempt++;
            if (attempt == 3) {
                Log.d(LOG_TAG, "Max attempts number reached");
                return;
            }
        }

        uniqueTransmitterKey = receivedHelloMessage.uniqueTransmissionKey;
        textView.setText(String.valueOf(uniqueTransmitterKey));

        uniqueReceiverKey = new Random().nextLong();
        long fileToSendSize = receivedHelloMessage.fileToSendSize;
        CommunicationProtocol.HelloMessage helloMessageToSend = CommunicationProtocol.HelloMessage.create(uniqueReceiverKey, fileToSendSize);

//        final int maxPairingAttempts = 5;
//        for (int i = 0; i < maxPairingAttempts; i++) {
//            Log.d(LOG_TAG, "Send hello attempt #" + i);
//            try {
//                final int blockingSendTimeout = 2;
//                communicator.blockingSend(helloMessageToSend.toByteArray(), blockingSendTimeout);
//            } catch (IOException ioException) {
//                Log.d(LOG_TAG, "Blocking send of hello message attempt #" + i + " IOException: " + ioException.getMessage());
//                continue;
//            }
//            Log.d(LOG_TAG, "Hello message attempt #" + i + " was successfully sent: " + helloMessageToSend);
//        }
    }
}