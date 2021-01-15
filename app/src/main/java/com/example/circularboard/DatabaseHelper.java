package com.example.circularboard;

import android.content.Context;
import android.graphics.Color;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class DatabaseHelper extends SQLiteAssetHelper {
    public DatabaseHelper(Context context) {
        super(context, "engwords.db", null, 1);
    }
}
