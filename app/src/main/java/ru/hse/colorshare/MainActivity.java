package ru.hse.colorshare;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
    }

    public void onClickSelectFile(View view) {
        Log.d(LOG_TAG, "Attempt to select a file");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, RequestCode.PICK_FILE.value);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "File Manager not found", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartSending(View view) {
        if (fileToSendInfo.uri == null) {
            Toast.makeText(getApplicationContext(), "Select a file", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TransmitterActivity.class);
        intent.putExtra("fileToSendUri", fileToSendInfo.uri);
        startActivityForResult(intent, RequestCode.TRANSMIT_FILE.value);
    }

    public void onClickTest(View view) {
        Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCodeValue, int resultCodeValue,
                                    Intent resultData) {
        Log.d(LOG_TAG, "requestCodeValue = " + requestCodeValue + ", resultCodeValue = " + resultCodeValue);
        RequestCode requestCode = RequestCode.valueOf(requestCodeValue);
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
                Cursor cursor =
                        getContentResolver().query(fileToSendInfo.uri, null, null, null, null);
                try {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    fileToSendInfo.name = cursor.getString(nameIndex);
                } catch (IllegalArgumentException exc) {
                    Toast.makeText(getApplicationContext(), "Failed to get file info, try again", Toast.LENGTH_SHORT).show();
                    fileToSendInfo.uri = null;
                    break;
                }
                cursor.close();
                Log.d(LOG_TAG, "File info: " + "name = " + fileToSendInfo.name + "; uri = " + fileToSendInfo.uri);

                fileToSendTextView.setText(fileToSendInfo.name);
                break;
            case TRANSMIT_FILE:
                TransmissionResultCode resultCode = TransmissionResultCode.valueOf(resultCodeValue);
                switch (resultCode) {
                    case FAILED:
                        Toast.makeText(getApplicationContext(), "File sending failed, try again", Toast.LENGTH_LONG).show();
                        break;
                    case SUCCESS:
                        Toast.makeText(getApplicationContext(), "File was successfully sent!", Toast.LENGTH_LONG).show();
                        break;
                }
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
        PICK_FILE(2),
        TRANSMIT_FILE(3);

        private final int value;
        private static final Map<Integer, RequestCode> map = new HashMap<>();

        static {
            for (RequestCode requestCode : RequestCode.values()) {
                map.put(requestCode.value, requestCode);
            }
        }

        RequestCode(int value) {
            this.value = value;
        }

        public static RequestCode valueOf(int requestCodeValue) {
            return map.get(requestCodeValue);
        }
    }

    public enum TransmissionResultCode {
        FAILED(3),
        SUCCESS(4);

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

        public static TransmissionResultCode valueOf(int resultCodeValue) {
            return map.get(resultCodeValue);
        }
    }
}