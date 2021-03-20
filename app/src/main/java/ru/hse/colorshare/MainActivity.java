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

public class MainActivity extends AppCompatActivity {

    private TextView fileToSendTextView;
    private String fileToSendUri;
    private String fileToSendName;
    private long fileToSendSize;

    private static final int PICK_FILE_REQUEST = 2;
    private static final int TRANSMIT_FILE_REQUEST = 3;
    private static final String LOG_TAG = "ColorShareLogTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_screen);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // currently unused

        fileToSendTextView = findViewById(R.id.fileToSendTextView);
        fileToSendName = getResources().getString(R.string.file_to_send);
    }

    public void onClickSelectFile(View view) {
        Log.d(LOG_TAG, "Attempt to select a file");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, PICK_FILE_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "File Manager not found", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartSending(View view) {
        if (fileToSendUri == null) {
            Toast.makeText(getApplicationContext(), "Select a file", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TransmitterActivity.class);
        intent.putExtra("fileToSendUri", fileToSendUri);
        startActivityForResult(intent, TRANSMIT_FILE_REQUEST);
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
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent resultData) {
        Log.d(LOG_TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);

        switch (requestCode) {
            case PICK_FILE_REQUEST:
                if (resultCode != Activity.RESULT_OK) {
                    return;
                }
                if (resultData == null) {
                    Toast.makeText(getApplicationContext(), "File selection failed, try again", Toast.LENGTH_SHORT).show();
                    break;
                }
                Uri uri = resultData.getData();
                fileToSendUri = uri.toString();
                Log.d(LOG_TAG, "File uri: " + fileToSendUri);

                // get file name and size
                Cursor cursor =
                        getContentResolver().query(uri, null, null, null, null);
                try {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
                    cursor.moveToFirst();
                    fileToSendName = cursor.getString(nameIndex);
                    fileToSendSize = cursor.getLong(sizeIndex);
                } catch (IllegalArgumentException exc) {
                    Toast.makeText(getApplicationContext(), "Failed to get file info, try again", Toast.LENGTH_SHORT).show();
                    fileToSendUri = null;
                    break;
                }
                cursor.close();
                Log.d(LOG_TAG, "File info: " + "name = " + fileToSendName + ", size = " + fileToSendSize);

                fileToSendTextView.setText(fileToSendName);
                break;
            case TRANSMIT_FILE_REQUEST:
                if (resultCode == TransmitterActivity.RESULT_FAILED) {
                    Toast.makeText(getApplicationContext(), "File sending failed, try again", Toast.LENGTH_SHORT).show();
                    break;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("fileToSendUri", fileToSendUri);
        outState.putString("fileToSendName", fileToSendName);
        outState.putLong("fileToSendSize", fileToSendSize);
        Log.d(LOG_TAG, "onSaveInstanceState");
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileToSendUri = savedInstanceState.getString("fileToSendUri");
        fileToSendName = savedInstanceState.getString("fileToSendName");
        fileToSendSize = savedInstanceState.getLong("fileToSendSize");
        fileToSendTextView.setText(fileToSendName);
        Log.d(LOG_TAG, "onRestoreInstanceState");
    }
}