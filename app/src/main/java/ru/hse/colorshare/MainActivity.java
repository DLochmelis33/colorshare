package ru.hse.colorshare;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import ru.hse.colorshare.transmitter.TransmitterActivity;

public class MainActivity extends AppCompatActivity {

    private TextView fileToSendTextView;
    private final FileToSendInfo fileToSendInfo = new FileToSendInfo();

    private static final String LOG_TAG = "ColorShare:main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_screen);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // currently unused

        fileToSendTextView = findViewById(R.id.fileToSendTextView);
        fileToSendInfo.name = getResources().getString(R.string.default_file_to_send_message);

        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RequestCode.RECORD_AUDIO_PERMISSION.ordinal());
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (RequestCode.values()[requestCode]) {
            case RECORD_AUDIO_PERMISSION: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setResult(MainActivity.TransmissionResultCode.FAILED_TO_GET_RECORD_AUDIO_PERMISSION.value, new Intent());
                    finish();
                }
                break;
            }
            default:
                throw new IllegalStateException("illegal permission request code");
        }
    }

    public void onClickSelectFile(View view) {
        Log.d(LOG_TAG, "Attempt to select a file");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, RequestCode.PICK_FILE.ordinal());
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "File Manager not found", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartSending(View view) {
        if (fileToSendInfo.uri == null) {
            Toast.makeText(getApplicationContext(), "Select a file", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(LOG_TAG, "Start TransmitterActivity");
        Intent intent = new Intent(this, TransmitterActivity.class);
        intent.putExtra("fileToSendUri", fileToSendInfo.uri);
        startActivityForResult(intent, RequestCode.TRANSMIT_FILE.ordinal());
    }

    public void onClickReceive(View view) {
        Log.d(LOG_TAG, "Start ReceiverCameraActivity");
        Intent intent = new Intent(this, ru.hse.colorshare.communication.MockReceiverActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCodeValue, int resultCodeValue,
                                    Intent resultData) {
        Log.d(LOG_TAG, "requestCodeValue = " + requestCodeValue + "; resultCodeValue = " + resultCodeValue);
        RequestCode requestCode = RequestCode.values()[requestCodeValue];
        Log.d(LOG_TAG, "requestCode = " + requestCode);
        switch (requestCode) {
            case PICK_FILE:
                if (resultCodeValue != Activity.RESULT_OK) {
                    return;
                }
                if (resultData == null) {
                    Toast.makeText(getApplicationContext(), "File selection failed, try again", Toast.LENGTH_SHORT).show();
                    break;
                }
                fileToSendInfo.uri = resultData.getData();

                // get file name and size
                try (Cursor cursor = getContentResolver().query(fileToSendInfo.uri, null, null, null, null)) {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    fileToSendInfo.name = cursor.getString(nameIndex);
                } catch (IllegalArgumentException exc) {
                    Toast.makeText(getApplicationContext(), "Failed to get file info, try again", Toast.LENGTH_SHORT).show();
                    fileToSendInfo.uri = null;
                    break;
                }
                Log.d(LOG_TAG, "File info: " + "name = " + fileToSendInfo.name + "; uri = " + fileToSendInfo.uri);

                fileToSendTextView.setText(fileToSendInfo.name);
                break;
            case TRANSMIT_FILE:
                TransmissionResultCode resultCode = TransmissionResultCode.getCode(resultCodeValue);
                Log.d(LOG_TAG, "resultCode = " + resultCode);
                switch (resultCode) {
                    case SUCCEED:
                        Toast.makeText(getApplicationContext(), "File was successfully sent!", Toast.LENGTH_LONG).show();
                        break;
                    case CANCELED:
                        break;
                    case FAILED_TO_GET_RECORD_AUDIO_PERMISSION:
                        Toast.makeText(getApplicationContext(), "File sending failed: record audio permission was not granted, try again", Toast.LENGTH_LONG).show();
                        break;
                    case FAILED_TO_READ_FILE:
                        Toast.makeText(getApplicationContext(), "File sending failed: failed to read file, try again", Toast.LENGTH_LONG).show();
                        break;
                    case FAILED_TO_GET_TRANSMISSION_PARAMS:
                        Toast.makeText(getApplicationContext(), "File sending failed: bad device params", Toast.LENGTH_LONG).show();
                        break;
                    case PAIRING_FAILED:
                        Toast.makeText(getApplicationContext(), "File sending failed: transmitter and receiver pairing failed, try again", Toast.LENGTH_LONG).show();
                        break;
                    case RECEIVER_LOST:
                        Toast.makeText(getApplicationContext(), "File sending failed: receiver has been lost while transmission, try again", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            case RECEIVE_FILE:
                if (resultCodeValue != RESULT_OK) {
                    return;
                }
                Toast.makeText(getApplicationContext(), "File was successfully received!", Toast.LENGTH_LONG).show();
                break;
        }
        super.onActivityResult(requestCodeValue, resultCodeValue, resultData);
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        fileToSendInfo.saveTo(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileToSendInfo.restoreFrom(savedInstanceState);
        fileToSendTextView.setText(fileToSendInfo.name);
        Log.d(LOG_TAG, "onRestoreInstanceState");
    }

    private final static class FileToSendInfo {
        String name = "";
        Uri uri = null;

        public void saveTo(Bundle outState) {
            outState.putParcelable("fileToSendUri", uri);
            outState.putString("fileToSendName", name);
        }

        public void restoreFrom(Bundle savedInstanceState) {
            uri = savedInstanceState.getParcelable("fileToSendUri");
            name = savedInstanceState.getString("fileToSendName");
        }
    }

    private enum RequestCode {
        RECORD_AUDIO_PERMISSION,
        PICK_FILE,
        TRANSMIT_FILE,
        RECEIVE_FILE
    }

    public enum TransmissionResultCode {
        SUCCEED(Activity.RESULT_OK),
        CANCELED(Activity.RESULT_CANCELED),
        FAILED_TO_GET_RECORD_AUDIO_PERMISSION(4),
        FAILED_TO_READ_FILE(5),
        FAILED_TO_GET_TRANSMISSION_PARAMS(6),
        PAIRING_FAILED(7),
        RECEIVER_LOST(8);

        public final int value;
        private static final Map<Integer, TransmissionResultCode> map = new HashMap<>();

        static {
            for (TransmissionResultCode resultCode : TransmissionResultCode.values()) {
                map.put(resultCode.value, resultCode);
            }
        }

        TransmissionResultCode(int value) {
            this.value = value;
        }

        public static TransmissionResultCode getCode(int resultCodeValue) {
            return map.get(resultCodeValue);
        }
    }
}