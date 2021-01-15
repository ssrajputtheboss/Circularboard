package com.example.circularboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;

public class SettingsActivity extends AppCompatActivity {
    private Switch darkModeSwitch, wordSyncSwitch, ringSwitch, vibrateSwitch, messageSwitch;
    private SharedPreferences sharedPreferences;
    private Button deleteButton;
    private EditText delText;
    private DatabaseHelper helper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        helper = new DatabaseHelper(getApplicationContext());
        database = helper.getWritableDatabase();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settngs);
        messageSwitch = (Switch) findViewById(R.id.message_suggestion_switch);
        darkModeSwitch = (Switch) findViewById(R.id.dark_mode_switch);
        wordSyncSwitch = (Switch) findViewById(R.id.word_sync_switch);
        ringSwitch = (Switch) findViewById(R.id.ring_switch);
        vibrateSwitch = (Switch) findViewById(R.id.vibrate_switch);
        delText = (EditText) findViewById(R.id.del_word);
        deleteButton = (Button) findViewById(R.id.delete);
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        darkModeSwitch.setChecked(sharedPreferences.getBoolean("dark_mode", false));
        wordSyncSwitch.setChecked(sharedPreferences.getBoolean("word_sync", false));
        vibrateSwitch.setChecked(sharedPreferences.getBoolean("vibrate", true));
        ringSwitch.setChecked(sharedPreferences.getBoolean("ring", true));
        messageSwitch.setChecked(sharedPreferences.getBoolean("message_suggestion", false));
        saveData();
        darkModeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
        wordSyncSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
        ringSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
        vibrateSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
        messageSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrate();
                database.execSQL("insert into deletedWords(word) values(\"" + delText.getText().toString().trim() + "\");");
                database.execSQL("delete from userWords where word like '" + delText.getText().toString().trim() + "';");
                database.execSQL("delete from wordCollection where word like '" + delText.getText().toString().trim() + "';");
            }
        });

    }

    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("dark_mode", darkModeSwitch.isChecked());
        editor.putBoolean("word_sync", wordSyncSwitch.isChecked());
        editor.putBoolean("ring", ringSwitch.isChecked());
        editor.putBoolean("vibrate", vibrateSwitch.isChecked());
        editor.putBoolean("message_suggestion", messageSwitch.isChecked());
        editor.apply();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else vibrator.vibrate(50);
    }
}
