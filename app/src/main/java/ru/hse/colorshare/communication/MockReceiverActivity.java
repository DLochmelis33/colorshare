package ru.hse.colorshare.communication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;

import ru.hse.colorshare.MainActivity;
import ru.hse.colorshare.R;
import ru.hse.colorshare.communication.messages.BulkMessage;
import ru.hse.colorshare.communication.messages.BulkReceivedMessage;
import ru.hse.colorshare.communication.messages.PairingMessage;
import ru.hse.colorshare.communication.messages.TransmissionFinishedMessage;

public class MockReceiverActivity extends AppCompatActivity {

    private ByTurnsCommunicator communicator;

    private TextView textView;

    private static final String LOG_TAG = "ColorShare:receiver";

    @SuppressLint("Assert")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_receiver);
        textView = findViewById(R.id.textView);

        communicator = ByTurnsCommunicator.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (communicator != null) {
            communicator.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!communicator.checkPermissionsAreGranted(requestCode, grantResults)) {
            setResult(MainActivity.TransmissionResultCode.FAILED_TO_GET_RECORD_AUDIO_PERMISSION.ordinal(), new Intent());
            finish();
        }
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    public void onClickRun(View view) {
        Log.d(LOG_TAG, "Pairing started");
        try {
            Message message = communicator.firstTurnReceive();
            assert message.getMessageType().equals(Message.MessageType.PAIRING);
            communicator.bindPartner(message.getSenderId());
            Log.d(LOG_TAG, "Successfully bound to " + message.getSenderId());
            textView.setText("partner id: " + message.getSenderId());
        } catch (ConnectionLostException connectionLostException) {
            Log.d(LOG_TAG, "Pairing failed while waiting for request: " + connectionLostException.getMessage());
        }
        communicator.send(new PairingMessage());

        try {
            while (true) {
                Message message = communicator.receive();
                switch (message.getMessageType()) {
                    case BULK:
                        BulkMessage bulkMessage = (BulkMessage) message;
                        Log.d(LOG_TAG, "Bulk received: " + bulkMessage.getGridRows() + " x " + bulkMessage.getGridCols());
                        Log.d(LOG_TAG, "checksums: " + Arrays.toString(bulkMessage.getBulkChecksums()));
                        communicator.send(new BulkReceivedMessage());
                        break;
                    case TRANSMISSION_FINISHED:
                        Log.d(LOG_TAG, "Transmission finished!");
                        communicator.lastTurnSend(new TransmissionFinishedMessage());
                        return;
                    case TRANSMISSION_CANCELED:
                        Log.d(LOG_TAG, "Transmission canceled :(");
                        return;
                }
            }
        } catch (ConnectionLostException connectionLostException) {
            Log.d(LOG_TAG, "Connection lost: " + connectionLostException.getMessage());
        }

    }
}