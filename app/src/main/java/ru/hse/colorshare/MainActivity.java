package ru.hse.colorshare;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView fileToSendTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_screen);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // currently unused

        fileToSendTextView = findViewById(R.id.fileToSendTextView);
    }

    public void onClickSelectFile(View view) {
        Toast.makeText(getApplicationContext(), "Select your file", Toast.LENGTH_SHORT).show();
    }

    public void onClickStartSending(View view) {
        Toast.makeText(getApplicationContext(), "Start sending...", Toast.LENGTH_SHORT).show();
    }

    public void onClickTest(View view) {
        Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}