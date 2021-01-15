package com.example.circularboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.media.AudioManager;
import android.opengl.Visibility;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.lukedeighton.wheelview.WheelView;
import com.lukedeighton.wheelview.adapter.WheelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CircularInputBoard extends InputMethodService {
    private ConstraintLayout constraintLayout;
    private TextView[] suggestionText;
    private WheelView[] wheelViews, dualWheelViews;
    private String currWord, suggestions[], msg;
    private boolean isDark, isCaps, isSpchr, isWheelRunning, isDualWheelRunning;
    private float prevAngle, prevAngle2;
    private Button spaceButton, spaceButton2, doneButton;
    private SharedPreferences settings;
    private int keyboardType, drawableChoice;
    private int keyboardWidth, keyboardHeight;
    private int ITEM_RADIUS = 50, BUTTON_RADIUS = 125;
    private LinearLayout layout, messageView;
    private DisplayMetrics displayMetrics;
    private List<CircleKeys> keyItems[];
    private float centerX, centerY;
    private int code, code2;
    private SQLiteOpenHelper helper;
    private SQLiteDatabase readDB, writeDB;
    private EditText message;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateInputView() {
        initialize();
        setViews();
        setWheelDrawables();
        setListners();
        setInputView(constraintLayout);
        return constraintLayout;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onWindowShown() {
        isDark = settings.getBoolean("dark_mode", true);
        isCaps = false;
        if (settings.getBoolean("message_suggestion", false))
            messageView.setVisibility(View.VISIBLE);
        else messageView.setVisibility(View.INVISIBLE);
        if (isDark) drawableChoice = 3;
        else drawableChoice = 0;
        setWheelDrawables();
    }

    private void initialize() {
        settings = getSharedPreferences("settings", MODE_PRIVATE);
        constraintLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.keyboard_main, null);
        layout = (LinearLayout) constraintLayout.findViewById(R.id.linear_layout);
        messageView = (LinearLayout) constraintLayout.findViewById(R.id.message_view);
        message = (EditText) constraintLayout.findViewById(R.id.message);
        doneButton = (Button) constraintLayout.findViewById(R.id.done_button);
        wheelViews = new WheelView[4];
        dualWheelViews = new WheelView[4];
        suggestionText = new TextView[3];
        keyItems = new List[4];
        wheelViews[0] = (WheelView) constraintLayout.findViewById(R.id.wheel1);
        wheelViews[1] = (WheelView) constraintLayout.findViewById(R.id.wheel2);
        wheelViews[2] = (WheelView) constraintLayout.findViewById(R.id.wheel3);
        wheelViews[3] = (WheelView) constraintLayout.findViewById(R.id.wheel4);
        dualWheelViews[0] = (WheelView) constraintLayout.findViewById(R.id.wheel5);
        dualWheelViews[1] = (WheelView) constraintLayout.findViewById(R.id.wheel6);
        dualWheelViews[2] = (WheelView) constraintLayout.findViewById(R.id.wheel7);
        dualWheelViews[3] = (WheelView) constraintLayout.findViewById(R.id.wheel8);
        suggestionText[0] = (TextView) constraintLayout.findViewById(R.id.text_one);
        suggestionText[1] = (TextView) constraintLayout.findViewById(R.id.text_two);
        suggestionText[2] = (TextView) constraintLayout.findViewById(R.id.text_three);
        spaceButton = (Button) constraintLayout.findViewById(R.id.space_button);
        spaceButton2 = (Button) constraintLayout.findViewById(R.id.space_button2);
        isDark = settings.getBoolean("dark_mode", true);
        isSpchr = false;
        prevAngle2 = prevAngle = wheelViews[3].getAngle();
        currWord = new String("");
        msg = new String("");
        suggestions = new String[3];
        for (int i = 0; i < 3; i++) {
            suggestions[i] = "";
        }
        keyboardType = 0;
        drawableChoice = 3;
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        keyboardWidth = displayMetrics.widthPixels;
        ITEM_RADIUS = (keyboardWidth * 3 / 64);
        BUTTON_RADIUS = (keyboardWidth / 8);
        keyboardHeight = keyboardWidth;

        //sqlite
        helper = new DatabaseHelper(getApplicationContext());
        readDB = helper.getReadableDatabase();
        writeDB = helper.getWritableDatabase();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setViews() {

        spaceButton.setWidth(BUTTON_RADIUS * 2);
        spaceButton.setHeight(BUTTON_RADIUS * 2);
        spaceButton2.setWidth(BUTTON_RADIUS * 2);
        spaceButton2.setHeight(BUTTON_RADIUS * 2);
        //setting Wheelviews
        for (int i = 0; i < 4; i++) {
            wheelViews[i].setWheelItemRadius(ITEM_RADIUS);
            wheelViews[i].setWheelRadius(BUTTON_RADIUS + (i + 1) * 2 * ITEM_RADIUS);
            wheelViews[i].setWheelItemCount((i + 2) * 4);
            wheelViews[i].setX(keyboardWidth - BUTTON_RADIUS - 2 * (i + 1) * ITEM_RADIUS);
            wheelViews[i].setY(keyboardHeight - wheelViews[i].getWheelRadius());
            dualWheelViews[i].setWheelItemRadius(ITEM_RADIUS);
            dualWheelViews[i].setWheelRadius(BUTTON_RADIUS + (i + 1) * 2 * ITEM_RADIUS);
            dualWheelViews[i].setWheelItemCount((i + 2) * 4);
            dualWheelViews[i].setX(0f - BUTTON_RADIUS - 2 * (i + 1) * ITEM_RADIUS);
            dualWheelViews[i].setY(keyboardHeight - dualWheelViews[i].getWheelRadius());
        }
        centerX = ((float) wheelViews[3].getWheelRadius());
        centerY = ((float) wheelViews[3].getWheelRadius());
        spaceButton.setX(keyboardWidth - BUTTON_RADIUS);
        spaceButton.setY(keyboardHeight - BUTTON_RADIUS);
        spaceButton2.setX(0 - BUTTON_RADIUS);
        spaceButton2.setY(keyboardHeight - BUTTON_RADIUS);
        layout.setY(keyboardHeight - wheelViews[3].getWheelRadius() - 2 * ITEM_RADIUS);
        message.setHeight(2 * ITEM_RADIUS);
        doneButton.setHeight(2 * ITEM_RADIUS);
        message.setWidth(keyboardWidth * 8 / 10);
        messageView.setY(keyboardHeight - wheelViews[3].getWheelRadius() - ITEM_RADIUS * 4 - 10);
        if (keyboardType == 0) {
            suggestionText[0].setWidth(keyboardWidth / 3);
            suggestionText[1].setWidth(keyboardWidth / 3);
            suggestionText[2].setWidth(keyboardWidth / 3);
        }
        suggestionText[0].setBackgroundColor(Color.DKGRAY);
        suggestionText[0].setTextColor(Color.WHITE);
        suggestionText[1].setBackgroundColor(Color.DKGRAY);
        suggestionText[1].setTextColor(Color.WHITE);
        suggestionText[2].setBackgroundColor(Color.DKGRAY);
        suggestionText[2].setTextColor(Color.WHITE);
        setItemXY();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setWheelDrawables() {
        final Drawable[] drawables = getDrawables();
        wheelViews[0].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                switch (position) {
                    case 0:
                        keyItems[0].get(6).setCode(Keyboard.KEYCODE_DONE);
                        return drawables[48];
                    case 7:
                        keyItems[0].get(7).setCode(Keyboard.KEYCODE_DELETE);
                        return drawables[49];
                    case 6:
                        keyItems[0].get(0).setCode(Keyboard.KEYCODE_DELETE);
                        return drawables[49];
                    case 5:
                        keyItems[0].get(1).setCode(Keyboard.KEYCODE_DONE);
                        return drawables[48];
                    default:
                        return drawables[49];
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        });

        wheelViews[1].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                return drawables[position];
            }

            @Override
            public int getCount() {
                return 12;
            }
        });

        wheelViews[2].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                return drawables[12 + position];
            }

            @Override
            public int getCount() {
                return 16;
            }
        });

        wheelViews[3].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {

                return drawables[28 + position];
            }

            @Override
            public int getCount() {
                return 20;
            }
        });


        dualWheelViews[0].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                switch (position) {
                    case 0:
                        keyItems[0].get(6).setCode(Keyboard.KEYCODE_DONE);
                        return drawables[48];
                    case 7:
                        keyItems[0].get(7).setCode(Keyboard.KEYCODE_DELETE);
                        return drawables[49];
                    case 6:
                        keyItems[0].get(0).setCode(Keyboard.KEYCODE_DELETE);
                        return drawables[49];
                    case 5:
                        keyItems[0].get(1).setCode(Keyboard.KEYCODE_DONE);
                        return drawables[48];
                    default:
                        return drawables[49];
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        });

        dualWheelViews[1].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                return drawables[position];
            }

            @Override
            public int getCount() {
                return 12;
            }
        });

        dualWheelViews[2].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {
                return drawables[12 + position];
            }

            @Override
            public int getCount() {
                return 16;
            }
        });

        dualWheelViews[3].setAdapter(new WheelAdapter() {
            @Override
            public Drawable getDrawable(int position) {

                return drawables[28 + position];
            }

            @Override
            public int getCount() {
                return 20;
            }
        });
    }

    private void setListners() {

        wheelViews[3].setOnWheelAngleChangeListener(new WheelView.OnWheelAngleChangeListener() {
            @Override
            public void onWheelAngleChange(float angle) {
                setItemXY();
                wheelViews[1].setAngle(wheelViews[3].getAngle());
                wheelViews[2].setAngle(wheelViews[3].getAngle());
            }
        });

        dualWheelViews[3].setOnWheelAngleChangeListener(new WheelView.OnWheelAngleChangeListener() {
            @Override
            public void onWheelAngleChange(float angle) {
                setItemXY();
                dualWheelViews[1].setAngle(dualWheelViews[3].getAngle());
                dualWheelViews[2].setAngle(dualWheelViews[3].getAngle());
            }
        });


        wheelViews[3].setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        setItemXY();
                        code = getCodeItemXY(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        if (code != 0) {
                            print(code);
                            code = 0;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (prevAngle != wheelViews[3].getAngle())
                            code = 0;
                        break;
                }
                prevAngle = wheelViews[3].getAngle();

                if (keyboardType == 2) dualWheelViews[3].onTouchEvent(event);
                return wheelViews[3].onTouchEvent(event);
            }
        });

        dualWheelViews[3].setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (keyboardType == 2) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            setItemXY();
                            code2 = getCodeItemXY(event.getX(), event.getY());
                            break;
                        case MotionEvent.ACTION_UP:
                            if (code2 != 0) {
                                print(code2);
                                code2 = 0;
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (prevAngle2 != dualWheelViews[3].getAngle())
                                code2 = 0;
                            break;
                    }
                    prevAngle2 = dualWheelViews[3].getAngle();
                }

                if (keyboardType == 2) wheelViews[3].onTouchEvent(event);
                return dualWheelViews[3].onTouchEvent(event);
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (settings.getBoolean("message_suggestion", false)) {
                    getCurrentInputConnection().deleteSurroundingText(msg.length(), 0);
                    msg = "";
                    String msg = message.getText().toString();
                    getCurrentInputConnection().commitText(msg, msg.length());
                    message.setText("");
                    currWord = "";
                    updateSuggestions();
                }
            }
        });

        spaceButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                print(32);
            }
        });
        for (int i = 0; i < 3; i++) {
            final int finalI = i;
            suggestionText[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    outputText(finalI);
                }
            });
        }
        spaceButton2.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                print(32);
            }
        });
        for (int i = 0; i < 3; i++) {
            final int finalI = i;
            suggestionText[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    outputText(finalI);
                }
            });
        }
    }

    private void playClick(int primaryCode) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (primaryCode) {
            case 32:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 5);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 5);
                break;
            case Keyboard.KEYCODE_DELETE:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, 5);
                break;
            default:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 5);
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else vibrator.vibrate(50);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeKeyboard() {
        switch (keyboardType) {
            case 1:
                for (int i = 0; i < dualWheelViews.length; i++) {
                    dualWheelViews[i].setVisibility(View.INVISIBLE);
                }
                spaceButton2.setVisibility(View.INVISIBLE);
                keyboardWidth = 0;
                setViews();
                wheelViews[0].setAngle(0f);
                wheelViews[3].setAngle(0f);
                break;
            case 0:
                for (int i = 0; i < dualWheelViews.length; i++) {
                    dualWheelViews[i].setVisibility(View.INVISIBLE);
                }
                spaceButton2.setVisibility(View.INVISIBLE);
                keyboardWidth = displayMetrics.widthPixels;
                setViews();
                wheelViews[3].setAngle(0f);
                wheelViews[0].setAngle(0f);
                break;
            case 2:
                keyboardWidth = displayMetrics.widthPixels;
                dualWheelViews[3].setAngle(0f);
                setViews();
                wheelViews[3].setAngle(0f);
                wheelViews[0].setAngle(0f);
                spaceButton2.setVisibility(View.VISIBLE);
                for (int i = 0; i < dualWheelViews.length; i++) {
                    dualWheelViews[i].setVisibility(View.VISIBLE);
                }
                isDualWheelRunning = false;
                isWheelRunning = false;
                break;

        }
    }

    private float getMainAngle() {
        float angle = wheelViews[3].getAngle();
        if (angle < 0)
            return (-1) * (angle % 360f);
        else return 360f - angle % 360f;
    }

    private float addExtraAngle(int i) {
        if (i == 0) {
            return wheelViews[0].getWheelItemAngle() - (float) Math.toDegrees(Math.asin(ITEM_RADIUS / wheelViews[0].getWheelRadius()));
        } else {
            return (float) Math.toDegrees(Math.asin((i - 1) * ITEM_RADIUS / wheelViews[i].getWheelRadius()));
        }
    }

    private double sinValue(int i, int j) {
        return Math.sin(Math.toRadians((getMainAngle() + addExtraAngle(i) + j * wheelViews[i].getWheelItemAngle()) % 360f));
    }

    private double cosValue(int i, int j) {
        return Math.cos(Math.toRadians((getMainAngle() + addExtraAngle(i) + j * wheelViews[i].getWheelItemAngle()) % 360f));
    }

    private void setItemXY() {
        for (int i = 0; i < wheelViews.length; i++) {
            if (keyItems[i] == null) {
                keyItems[i] = new ArrayList<>();
                for (int j = 0; j < wheelViews[i].getWheelItemCount(); j++) {
                    float cx = (float) (centerX - (float) ((2 * i + 1) * ITEM_RADIUS + BUTTON_RADIUS) * sinValue(i, j));
                    float cy = (float) (centerY - (float) ((2 * i + 1) * ITEM_RADIUS + BUTTON_RADIUS) * cosValue(i, j));
                    keyItems[i].add(new CircleKeys(cx, cy, ITEM_RADIUS));
                }
            } else {
                if (i == 0) continue;
                for (int j = 0; j < wheelViews[i].getWheelItemCount(); j++) {
                    float cx = (float) (centerX - (float) ((2 * i + 1) * ITEM_RADIUS + BUTTON_RADIUS) * sinValue(i, j));
                    float cy = (float) (centerY - (float) ((2 * i + 1) * ITEM_RADIUS + BUTTON_RADIUS) * cosValue(i, j));
                    keyItems[i].get(j).setCenterXY(cx, cy);
                }
            }
        }

    }

    private int getCodeItemXY(float x, float y) {
        for (int i = 0; i < wheelViews.length; i++) {
            for (int j = 0; j < keyItems[i].size(); j++) {

                if (keyItems[i].get(j).contains(x, y)) {
                    return keyItems[i].get(j).getCode();
                }
            }


        }
        return 0;
    }


    private void updateSuggestions() {
        if (!currWord.equals("")) {
            Cursor c = readDB.rawQuery("select word from userWords where word like '" + currWord + "%';", new String[]{});
            int i = 0;
            while (c.moveToNext() && i < 3) {
                String string = c.getString(0).trim() + " ";
                if (suggestions[0].equals(string) || suggestions[1].equals(string) || suggestions[2].equals(string))
                    continue;
                suggestions[i] = string + " ";
                ++i;
            }
            if (i < 3) {
                c = null;
                c = readDB.rawQuery("select word from wordCollection where word like '" + currWord + "%';", new String[]{});
                while (c.moveToNext() && i < 3) {
                    String string = c.getString(0).trim() + " ";
                    if (suggestions[0].equals(string) || suggestions[1].equals(string) || suggestions[2].equals(string))
                        continue;
                    suggestions[i] = string + " ";
                    ++i;
                }
                if (i < 3) {
                    while (i < 3) {
                        suggestions[i] = currWord + " ";
                        ++i;
                    }
                    if (suggestions[0].equals(suggestions[1]) && suggestions[1].equals(suggestions[2])) {
                        suggestions[0] = suggestions[2] = "";
                    } else if (suggestions[1].equals(suggestions[2])) suggestions[2] = "";
                }
            }
        } else {
            suggestions[0] = "";
            suggestions[1] = "";
            suggestions[2] = "";
        }
        for (int i = 0; i < 3; i++) {
            if (suggestions[i].length() <= 10)
                suggestionText[i].setText(suggestions[i]);
            else
                suggestionText[i].setText(suggestions[i].substring(0, 7) + ".." + suggestions[i].charAt(suggestions[i].length() - 1));
        }
    }

    private void outputText(int i) {
        vibrate();
        if (settings.getBoolean("word_sync", false)) {
            Cursor c = readDB.rawQuery("select word from deletedWords where word like ('" + currWord + "')", new String[]{});
            if (!c.moveToNext())
                writeDB.execSQL("insert into userWords(word) values(\"" + currWord + "\");");
        }
        getCurrentInputConnection().deleteSurroundingText(currWord.length(), 0);
        getCurrentInputConnection().commitText(suggestions[i], suggestions[i].length());
        currWord = "";
        updateSuggestions();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void print(int code) {
        if (settings.getBoolean("ring", true))
            playClick(code);
        if (settings.getBoolean("vibrate", true))
            vibrate();
        InputConnection ic = getCurrentInputConnection();

        switch (code) {
            case Keyboard.KEYCODE_DELETE:
                if (settings.getBoolean("message_suggestion", false)) {
                    if (!msg.equals(""))
                        msg = msg.substring(0, msg.length() - 1);
                    updateMessageView();
                }
                if (!currWord.equals(""))
                    currWord = currWord.substring(0, currWord.length() - 1);
                ic.deleteSurroundingText(1, 0);
                updateSuggestions();
                break;
            case Keyboard.KEYCODE_DONE:
                msg = "";
                if (!currWord.equals("") && settings.getBoolean("word_sync", false)) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("word", currWord);
                    Cursor c = readDB.rawQuery("select word from deletedWords where word like ('" + currWord + "')", new String[]{});
                    if (!c.moveToNext())
                        writeDB.insert("userWords", null, contentValues);
                }
                currWord = "";
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                updateSuggestions();
                updateMessageView();
                break;
            case 300:
                if (keyboardType == 2) keyboardType = 0;
                else keyboardType++;
                changeKeyboard();
                break;
            case 301:
                if (isCaps) {
                    if (drawableChoice != 0 || drawableChoice != 3)
                        drawableChoice--;
                } else drawableChoice++;
                setWheelDrawables();
                isCaps = !isCaps;
                break;
            case 305:
                if (isDark) {
                    if (!isSpchr)
                        drawableChoice = 5;
                    else
                        drawableChoice = 3;
                } else {
                    if (!isSpchr) {
                        drawableChoice = 2;
                    } else drawableChoice = 0;
                }
                setWheelDrawables();
                isCaps = false;
                isSpchr = !isSpchr;
                break;
            case 310:
                if (isDark) {
                    drawableChoice -= 3;
                } else {
                    drawableChoice += 3;
                }
                setWheelDrawables();
                isDark = !isDark;
                saveSettings();
                break;
            case 315:
                ic.commitText(".com", 4);
                break;
            case 320:
                setCopyPasteLayout();
                break;
            case 700:
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            default:
                if (isCaps && Character.isLetter((char) code)) code -= 32;
                char chr = (char) code;
                if (settings.getBoolean("message_suggestion", false)) {
                    if (Character.isLetter(chr) || chr == ' ' || chr == ',')
                        msg += chr;
                    if (chr == '.') msg = "";
                    updateMessageView();
                }
                ic.commitText("" + chr, 1);
                if (chr == ' ' && settings.getBoolean("word_sync", false)) {
                    if (!currWord.equals("")) {
                        Cursor c = readDB.rawQuery("select word from deletedWords where word like ('" + currWord + "')", new String[]{});
                        if (!c.moveToNext())
                            writeDB.execSQL("insert into userWords(word) values(\"" + currWord + "\");");
                    }
                }
                if (Character.isLetter(chr))
                    currWord += chr;
                else currWord = "";
                updateSuggestions();
        }
    }

    private void updateMessageView() {
        if (msg.equals("")) message.setText("");
        else {
            Cursor c = readDB.rawQuery("select msg from messageCollection where msg like ('" + msg + "%');", new String[]{});
            if (c.moveToNext()) message.setText(c.getString(0));
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("dark_mode", isDark);
        editor.apply();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Drawable[] getDrawables() {
        final Drawable[] drawables;
        switch (drawableChoice) {
            case 0:
                //smallLight
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.b_small_white);
                keyItems[1].get(10).setCode(98);
                drawables[1] = getDrawable(R.drawable.c_small_white);
                keyItems[1].get(9).setCode(99);
                drawables[2] = getDrawable(R.drawable.d_small_white);
                keyItems[1].get(8).setCode(100);
                drawables[3] = getDrawable(R.drawable.number_one_white);
                keyItems[1].get(7).setCode(49);
                drawables[4] = getDrawable(R.drawable.number_two_white);
                keyItems[1].get(6).setCode(50);
                drawables[5] = getDrawable(R.drawable.number_three_white);
                keyItems[1].get(5).setCode(51);
                drawables[6] = getDrawable(R.drawable.number_four_white);
                keyItems[1].get(4).setCode(52);
                drawables[7] = getDrawable(R.drawable.number_five_white);
                keyItems[1].get(3).setCode(53);
                drawables[8] = getDrawable(R.drawable.e_small_white);
                keyItems[1].get(2).setCode(101);
                drawables[9] = getDrawable(R.drawable.f_small_white);
                keyItems[1].get(1).setCode(102);
                drawables[10] = getDrawable(R.drawable.g_small_white);
                keyItems[1].get(0).setCode(103);
                drawables[11] = getDrawable(R.drawable.a_small_white);
                keyItems[1].get(11).setCode(97);
                //w3
                drawables[12] = getDrawable(R.drawable.j_small_white);
                keyItems[2].get(13).setCode(106);
                drawables[13] = getDrawable(R.drawable.k_small_white);
                keyItems[2].get(12).setCode(107);
                drawables[14] = getDrawable(R.drawable.l_small_white);
                keyItems[2].get(11).setCode(108);
                drawables[15] = getDrawable(R.drawable.comma_white);
                keyItems[2].get(10).setCode(44);
                drawables[16] = getDrawable(R.drawable.number_six_white);
                keyItems[2].get(9).setCode(54);
                drawables[17] = getDrawable(R.drawable.number_seven_white);
                keyItems[2].get(8).setCode(55);
                drawables[18] = getDrawable(R.drawable.number_eight_white);
                keyItems[2].get(7).setCode(56);
                drawables[19] = getDrawable(R.drawable.number_nine_white);
                keyItems[2].get(6).setCode(57);
                drawables[20] = getDrawable(R.drawable.number_zero_white);
                keyItems[2].get(5).setCode(48);
                drawables[21] = getDrawable(R.drawable.attherate_white);
                keyItems[2].get(4).setCode(64);
                drawables[22] = getDrawable(R.drawable.m_small_white);
                keyItems[2].get(3).setCode(109);
                drawables[23] = getDrawable(R.drawable.n_small_white);
                keyItems[2].get(2).setCode(110);
                drawables[24] = getDrawable(R.drawable.o_small_white);
                keyItems[2].get(1).setCode(111);
                drawables[25] = getDrawable(R.drawable.p_small_white);
                keyItems[2].get(0).setCode(112);
                drawables[26] = getDrawable(R.drawable.h_small_white);
                keyItems[2].get(15).setCode(104);
                drawables[27] = getDrawable(R.drawable.i_small_white);
                keyItems[2].get(14).setCode(105);
                //w4
                drawables[28] = getDrawable(R.drawable.t_small_white);
                keyItems[3].get(16).setCode(116);
                drawables[29] = getDrawable(R.drawable.u_small_white);
                keyItems[3].get(15).setCode(117);
                drawables[30] = getDrawable(R.drawable.dot_white);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.capital_white);
                keyItems[3].get(13).setCode(301);
                drawables[32] = getDrawable(R.drawable.special_light);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_white);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.white_to_dark);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_white);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_white);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_white);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_white);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.v_small_white);
                keyItems[3].get(4).setCode(118);
                drawables[41] = getDrawable(R.drawable.w_small_white);
                keyItems[3].get(3).setCode(119);
                drawables[42] = getDrawable(R.drawable.x_small_white);
                keyItems[3].get(2).setCode(120);
                drawables[43] = getDrawable(R.drawable.y_small_white);
                keyItems[3].get(1).setCode(121);
                drawables[44] = getDrawable(R.drawable.z_small_white);
                keyItems[3].get(0).setCode(122);
                drawables[45] = getDrawable(R.drawable.q_small_white);
                keyItems[3].get(19).setCode(113);
                drawables[46] = getDrawable(R.drawable.r_small_white);
                keyItems[3].get(18).setCode(114);
                drawables[47] = getDrawable(R.drawable.s_small_white);
                keyItems[3].get(17).setCode(115);
                //w1
                drawables[48] = getDrawable(R.drawable.done_white);
                drawables[49] = getDrawable(R.drawable.back_white);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            case 1:
                //largeLight
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.b_capital_white);
                keyItems[1].get(10).setCode(98);
                drawables[1] = getDrawable(R.drawable.c_capital_white);
                keyItems[1].get(9).setCode(99);
                drawables[2] = getDrawable(R.drawable.d_capital_white);
                keyItems[1].get(8).setCode(100);
                drawables[3] = getDrawable(R.drawable.number_one_white);
                keyItems[1].get(7).setCode(49);
                drawables[4] = getDrawable(R.drawable.number_two_white);
                keyItems[1].get(6).setCode(50);
                drawables[5] = getDrawable(R.drawable.number_three_white);
                keyItems[1].get(5).setCode(51);
                drawables[6] = getDrawable(R.drawable.number_four_white);
                keyItems[1].get(4).setCode(52);
                drawables[7] = getDrawable(R.drawable.number_five_white);
                keyItems[1].get(3).setCode(53);
                drawables[8] = getDrawable(R.drawable.e_capital_white);
                keyItems[1].get(2).setCode(101);
                drawables[9] = getDrawable(R.drawable.f_capital_white);
                keyItems[1].get(1).setCode(102);
                drawables[10] = getDrawable(R.drawable.g_capital_white);
                keyItems[1].get(0).setCode(103);
                drawables[11] = getDrawable(R.drawable.a_capital_white);
                keyItems[1].get(11).setCode(97);
                //w3
                drawables[12] = getDrawable(R.drawable.j_capital_white);
                keyItems[2].get(13).setCode(106);
                drawables[13] = getDrawable(R.drawable.k_capital_white);
                keyItems[2].get(12).setCode(107);
                drawables[14] = getDrawable(R.drawable.l_capital_white);
                keyItems[2].get(11).setCode(108);
                drawables[15] = getDrawable(R.drawable.comma_white);
                keyItems[2].get(10).setCode(44);
                drawables[16] = getDrawable(R.drawable.number_six_white);
                keyItems[2].get(9).setCode(54);
                drawables[17] = getDrawable(R.drawable.number_seven_white);
                keyItems[2].get(8).setCode(55);
                drawables[18] = getDrawable(R.drawable.number_eight_white);
                keyItems[2].get(7).setCode(56);
                drawables[19] = getDrawable(R.drawable.number_nine_white);
                keyItems[2].get(6).setCode(57);
                drawables[20] = getDrawable(R.drawable.number_zero_white);
                keyItems[2].get(5).setCode(48);
                drawables[21] = getDrawable(R.drawable.attherate_white);
                keyItems[2].get(4).setCode(64);
                drawables[22] = getDrawable(R.drawable.m_capital_white);
                keyItems[2].get(3).setCode(109);
                drawables[23] = getDrawable(R.drawable.n_capital_white);
                keyItems[2].get(2).setCode(110);
                drawables[24] = getDrawable(R.drawable.o_capital_white);
                keyItems[2].get(1).setCode(111);
                drawables[25] = getDrawable(R.drawable.p_capital_white);
                keyItems[2].get(0).setCode(112);
                drawables[26] = getDrawable(R.drawable.h_capital_white);
                keyItems[2].get(15).setCode(104);
                drawables[27] = getDrawable(R.drawable.i_capital_white);
                keyItems[2].get(14).setCode(105);
                //w4
                drawables[28] = getDrawable(R.drawable.t_capital_white);
                keyItems[3].get(16).setCode(116);
                drawables[29] = getDrawable(R.drawable.u_capital_white);
                keyItems[3].get(15).setCode(117);
                drawables[30] = getDrawable(R.drawable.dot_white);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.capital_white);
                keyItems[3].get(13).setCode(301);
                drawables[32] = getDrawable(R.drawable.special_light);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_white);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.white_to_dark);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_white);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_white);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_white);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_white);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.v_capital_white);
                keyItems[3].get(4).setCode(118);
                drawables[41] = getDrawable(R.drawable.w_capital_white);
                keyItems[3].get(3).setCode(119);
                drawables[42] = getDrawable(R.drawable.x_capital_white);
                keyItems[3].get(2).setCode(120);
                drawables[43] = getDrawable(R.drawable.y_capital_white);
                keyItems[3].get(1).setCode(121);
                drawables[44] = getDrawable(R.drawable.z_capital_white);
                keyItems[3].get(0).setCode(122);
                drawables[45] = getDrawable(R.drawable.q_capital_white);
                keyItems[3].get(19).setCode(113);
                drawables[46] = getDrawable(R.drawable.r_capital_white);
                keyItems[3].get(18).setCode(114);
                drawables[47] = getDrawable(R.drawable.s_capital_white);
                keyItems[3].get(17).setCode(115);
                //w1
                drawables[48] = getDrawable(R.drawable.done_white);
                drawables[49] = getDrawable(R.drawable.back_white);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            case 2://specialLight
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.single_quote_white);
                keyItems[1].get(10).setCode(39);
                drawables[1] = getDrawable(R.drawable.double_quote_white);
                keyItems[1].get(9).setCode(34);
                drawables[2] = getDrawable(R.drawable.comma_white);
                keyItems[1].get(8).setCode(44);
                drawables[3] = getDrawable(R.drawable.dot_white);
                keyItems[1].get(7).setCode(46);
                drawables[4] = getDrawable(R.drawable.equals_white);
                keyItems[1].get(6).setCode(61);
                drawables[5] = getDrawable(R.drawable.plus_white);
                keyItems[1].get(5).setCode(43);
                drawables[6] = getDrawable(R.drawable.minus_white);
                keyItems[1].get(4).setCode(45);
                drawables[7] = getDrawable(R.drawable.and_white);
                keyItems[1].get(3).setCode(38);
                drawables[8] = getDrawable(R.drawable.semicolon_white);
                keyItems[1].get(2).setCode(59);
                drawables[9] = getDrawable(R.drawable.left_tilt_line_white);
                keyItems[1].get(1).setCode(92);
                drawables[10] = getDrawable(R.drawable.colon_white);
                keyItems[1].get(0).setCode(58);
                drawables[11] = getDrawable(R.drawable.right_tilt_line_white);
                keyItems[1].get(11).setCode(47);
                //w3
                drawables[12] = getDrawable(R.drawable.braces_square_close_white);
                keyItems[2].get(13).setCode(93);
                drawables[13] = getDrawable(R.drawable.purnviram_white);
                keyItems[2].get(12).setCode(124);
                drawables[14] = getDrawable(R.drawable.arrow_right_white);
                keyItems[2].get(11).setCode(62);
                drawables[15] = getDrawable(R.drawable.underscore_white);
                keyItems[2].get(10).setCode(95);
                drawables[16] = getDrawable(R.drawable.question_mark_white);
                keyItems[2].get(9).setCode(63);
                drawables[17] = getDrawable(R.drawable.asterisc_white);
                keyItems[2].get(8).setCode(42);
                drawables[18] = getDrawable(R.drawable.factorial_white);
                keyItems[2].get(7).setCode(33);
                drawables[19] = getDrawable(R.drawable.hash_white);
                keyItems[2].get(6).setCode(35);
                drawables[20] = getDrawable(R.drawable.arrow_up_white);
                keyItems[2].get(5).setCode(94);
                drawables[21] = getDrawable(R.drawable.arrow_left_white);
                keyItems[2].get(4).setCode(60);
                drawables[22] = getDrawable(R.drawable.percentage_white);
                keyItems[2].get(3).setCode(37);
                drawables[23] = getDrawable(R.drawable.braces_circular_open_white);
                keyItems[2].get(2).setCode(40);
                drawables[24] = getDrawable(R.drawable.braces_circular_close_white);
                keyItems[2].get(1).setCode(41);
                drawables[25] = getDrawable(R.drawable.braces_curly_open_white);
                keyItems[2].get(0).setCode(123);
                drawables[26] = getDrawable(R.drawable.braces_curly_close_white);
                keyItems[2].get(15).setCode(125);
                drawables[27] = getDrawable(R.drawable.braces_square_open_white);
                keyItems[2].get(14).setCode(91);
                //w4
                drawables[28] = getDrawable(R.drawable.number_eight_white);
                keyItems[3].get(16).setCode(56);
                drawables[29] = getDrawable(R.drawable.number_nine_white);
                keyItems[3].get(15).setCode(57);
                drawables[30] = getDrawable(R.drawable.dot_white);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.attherate_white);
                keyItems[3].get(13).setCode(64);
                drawables[32] = getDrawable(R.drawable.alphabet_key_white);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_white);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.white_to_dark);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_white);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_white);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_white);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_white);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.number_zero_white);
                keyItems[3].get(4).setCode(48);
                drawables[41] = getDrawable(R.drawable.number_one_white);
                keyItems[3].get(3).setCode(49);
                drawables[42] = getDrawable(R.drawable.number_two_white);
                keyItems[3].get(2).setCode(50);
                drawables[43] = getDrawable(R.drawable.number_three_white);
                keyItems[3].get(1).setCode(51);
                drawables[44] = getDrawable(R.drawable.number_four_white);
                keyItems[3].get(0).setCode(52);
                drawables[45] = getDrawable(R.drawable.number_five_white);
                keyItems[3].get(19).setCode(53);
                drawables[46] = getDrawable(R.drawable.number_six_white);
                keyItems[3].get(18).setCode(54);
                drawables[47] = getDrawable(R.drawable.number_seven_white);
                keyItems[3].get(17).setCode(55);
                //w1
                drawables[48] = getDrawable(R.drawable.done_white);
                drawables[49] = getDrawable(R.drawable.back_white);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            case 3:
                //smallDark
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.b_small_dark);
                keyItems[1].get(10).setCode(98);
                drawables[1] = getDrawable(R.drawable.c_small_dark);
                keyItems[1].get(9).setCode(99);
                drawables[2] = getDrawable(R.drawable.d_small_dark);
                keyItems[1].get(8).setCode(100);
                drawables[3] = getDrawable(R.drawable.number_one_dark);
                keyItems[1].get(7).setCode(49);
                drawables[4] = getDrawable(R.drawable.number_two_dark);
                keyItems[1].get(6).setCode(50);
                drawables[5] = getDrawable(R.drawable.number_three_dark);
                keyItems[1].get(5).setCode(51);
                drawables[6] = getDrawable(R.drawable.number_four_dark);
                keyItems[1].get(4).setCode(52);
                drawables[7] = getDrawable(R.drawable.number_five_dark);
                keyItems[1].get(3).setCode(53);
                drawables[8] = getDrawable(R.drawable.e_small_dark);
                keyItems[1].get(2).setCode(101);
                drawables[9] = getDrawable(R.drawable.f_small_dark);
                keyItems[1].get(1).setCode(102);
                drawables[10] = getDrawable(R.drawable.g_small_dark);
                keyItems[1].get(0).setCode(103);
                drawables[11] = getDrawable(R.drawable.a_small_dark);
                keyItems[1].get(11).setCode(97);
                //w3
                drawables[12] = getDrawable(R.drawable.j_small_dark);
                keyItems[2].get(13).setCode(106);
                drawables[13] = getDrawable(R.drawable.k_small_dark);
                keyItems[2].get(12).setCode(107);
                drawables[14] = getDrawable(R.drawable.l_small_dark);
                keyItems[2].get(11).setCode(108);
                drawables[15] = getDrawable(R.drawable.comma_dark);
                keyItems[2].get(10).setCode(44);
                drawables[16] = getDrawable(R.drawable.number_six_dark);
                keyItems[2].get(9).setCode(54);
                drawables[17] = getDrawable(R.drawable.number_seven_dark);
                keyItems[2].get(8).setCode(55);
                drawables[18] = getDrawable(R.drawable.number_eight_dark);
                keyItems[2].get(7).setCode(56);
                drawables[19] = getDrawable(R.drawable.number_nine_dark);
                keyItems[2].get(6).setCode(57);
                drawables[20] = getDrawable(R.drawable.number_zero_dark);
                keyItems[2].get(5).setCode(48);
                drawables[21] = getDrawable(R.drawable.attherate_dark);
                keyItems[2].get(4).setCode(64);
                drawables[22] = getDrawable(R.drawable.m_small_dark);
                keyItems[2].get(3).setCode(109);
                drawables[23] = getDrawable(R.drawable.n_small_dark);
                keyItems[2].get(2).setCode(110);
                drawables[24] = getDrawable(R.drawable.o_small_dark);
                keyItems[2].get(1).setCode(111);
                drawables[25] = getDrawable(R.drawable.p_small_dark);
                keyItems[2].get(0).setCode(112);
                drawables[26] = getDrawable(R.drawable.h_small_dark);
                keyItems[2].get(15).setCode(104);
                drawables[27] = getDrawable(R.drawable.i_small_dark);
                keyItems[2].get(14).setCode(105);
                //w4
                drawables[28] = getDrawable(R.drawable.t_small_dark);
                keyItems[3].get(16).setCode(116);
                drawables[29] = getDrawable(R.drawable.u_small_dadrk);
                keyItems[3].get(15).setCode(117);
                drawables[30] = getDrawable(R.drawable.dot_dark);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.capital_dark);
                keyItems[3].get(13).setCode(301);
                drawables[32] = getDrawable(R.drawable.special_dark);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_dark);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.dark_to_white);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_dark);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_dark);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_dark);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_dark);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.v_small_dark);
                keyItems[3].get(4).setCode(118);
                drawables[41] = getDrawable(R.drawable.w_small_dark);
                keyItems[3].get(3).setCode(119);
                drawables[42] = getDrawable(R.drawable.x_small_dark);
                keyItems[3].get(2).setCode(120);
                drawables[43] = getDrawable(R.drawable.y_small_dark);
                keyItems[3].get(1).setCode(121);
                drawables[44] = getDrawable(R.drawable.z_small_dark);
                keyItems[3].get(0).setCode(122);
                drawables[45] = getDrawable(R.drawable.q_small_dark);
                keyItems[3].get(19).setCode(113);
                drawables[46] = getDrawable(R.drawable.r_small_dark);
                keyItems[3].get(18).setCode(114);
                drawables[47] = getDrawable(R.drawable.s_small_dark);
                keyItems[3].get(17).setCode(115);
                //w1
                drawables[48] = getDrawable(R.drawable.done_dark);
                drawables[49] = getDrawable(R.drawable.back_dark);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            case 4://largeDark
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.b_capital_dark);
                keyItems[1].get(10).setCode(98);
                drawables[1] = getDrawable(R.drawable.c_capital_dark);
                keyItems[1].get(9).setCode(99);
                drawables[2] = getDrawable(R.drawable.d_capital_dark);
                keyItems[1].get(8).setCode(100);
                drawables[3] = getDrawable(R.drawable.number_one_dark);
                keyItems[1].get(7).setCode(49);
                drawables[4] = getDrawable(R.drawable.number_two_dark);
                keyItems[1].get(6).setCode(50);
                drawables[5] = getDrawable(R.drawable.number_three_dark);
                keyItems[1].get(5).setCode(51);
                drawables[6] = getDrawable(R.drawable.number_four_dark);
                keyItems[1].get(4).setCode(52);
                drawables[7] = getDrawable(R.drawable.number_five_dark);
                keyItems[1].get(3).setCode(53);
                drawables[8] = getDrawable(R.drawable.e_capital_dark);
                keyItems[1].get(2).setCode(101);
                drawables[9] = getDrawable(R.drawable.f_capital_dark);
                keyItems[1].get(1).setCode(102);
                drawables[10] = getDrawable(R.drawable.g_capital_dark);
                keyItems[1].get(0).setCode(103);
                drawables[11] = getDrawable(R.drawable.a_capital_dark);
                keyItems[1].get(11).setCode(97);
                //w3
                drawables[12] = getDrawable(R.drawable.j_capital_dark);
                keyItems[2].get(13).setCode(106);
                drawables[13] = getDrawable(R.drawable.k_capital_dark);
                keyItems[2].get(12).setCode(107);
                drawables[14] = getDrawable(R.drawable.l_capital_dark);
                keyItems[2].get(11).setCode(108);
                drawables[15] = getDrawable(R.drawable.comma_dark);
                keyItems[2].get(10).setCode(44);
                drawables[16] = getDrawable(R.drawable.number_six_dark);
                keyItems[2].get(9).setCode(54);
                drawables[17] = getDrawable(R.drawable.number_seven_dark);
                keyItems[2].get(8).setCode(55);
                drawables[18] = getDrawable(R.drawable.number_eight_dark);
                keyItems[2].get(7).setCode(56);
                drawables[19] = getDrawable(R.drawable.number_nine_dark);
                keyItems[2].get(6).setCode(57);
                drawables[20] = getDrawable(R.drawable.number_zero_dark);
                keyItems[2].get(5).setCode(48);
                drawables[21] = getDrawable(R.drawable.attherate_dark);
                keyItems[2].get(4).setCode(64);
                drawables[22] = getDrawable(R.drawable.m_capital_dark);
                keyItems[2].get(3).setCode(109);
                drawables[23] = getDrawable(R.drawable.n_capital_dark);
                keyItems[2].get(2).setCode(110);
                drawables[24] = getDrawable(R.drawable.o_capital_dark);
                keyItems[2].get(1).setCode(111);
                drawables[25] = getDrawable(R.drawable.p_capital_dark);
                keyItems[2].get(0).setCode(112);
                drawables[26] = getDrawable(R.drawable.h_capital_dark);
                keyItems[2].get(15).setCode(104);
                drawables[27] = getDrawable(R.drawable.i_capital_dark);
                keyItems[2].get(14).setCode(105);
                //w4
                drawables[28] = getDrawable(R.drawable.t_capital_dark);
                keyItems[3].get(16).setCode(116);
                drawables[29] = getDrawable(R.drawable.u_capital_dark);
                keyItems[3].get(15).setCode(117);
                drawables[30] = getDrawable(R.drawable.dot_dark);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.capital_dark);
                keyItems[3].get(13).setCode(301);
                drawables[32] = getDrawable(R.drawable.special_dark);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_dark);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.dark_to_white);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_dark);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_dark);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_dark);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_dark);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.v_capital_dark);
                keyItems[3].get(4).setCode(118);
                drawables[41] = getDrawable(R.drawable.w_capital_dark);
                keyItems[3].get(3).setCode(119);
                drawables[42] = getDrawable(R.drawable.x_capital_dark);
                keyItems[3].get(2).setCode(120);
                drawables[43] = getDrawable(R.drawable.y_capital_dark);
                keyItems[3].get(1).setCode(121);
                drawables[44] = getDrawable(R.drawable.z_capital_dark);
                keyItems[3].get(0).setCode(122);
                drawables[45] = getDrawable(R.drawable.q_capital_dark);
                keyItems[3].get(19).setCode(113);
                drawables[46] = getDrawable(R.drawable.r_capital_dark);
                keyItems[3].get(18).setCode(114);
                drawables[47] = getDrawable(R.drawable.s_capital_dark);
                keyItems[3].get(17).setCode(115);
                //w1
                drawables[48] = getDrawable(R.drawable.done_dark);
                drawables[49] = getDrawable(R.drawable.back_dark);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            case 5://specialDark
                drawables = new Drawable[50];
                //w2
                drawables[0] = getDrawable(R.drawable.single_quote_dark);
                keyItems[1].get(10).setCode(39);
                drawables[1] = getDrawable(R.drawable.double_quote_dark);
                keyItems[1].get(9).setCode(34);
                drawables[2] = getDrawable(R.drawable.comma_dark);
                keyItems[1].get(8).setCode(44);
                drawables[3] = getDrawable(R.drawable.dot_dark);
                keyItems[1].get(7).setCode(46);
                drawables[4] = getDrawable(R.drawable.equals_dark);
                keyItems[1].get(6).setCode(61);
                drawables[5] = getDrawable(R.drawable.plus_dark);
                keyItems[1].get(5).setCode(43);
                drawables[6] = getDrawable(R.drawable.minus_dark);
                keyItems[1].get(4).setCode(45);
                drawables[7] = getDrawable(R.drawable.and_dark);
                keyItems[1].get(3).setCode(38);
                drawables[8] = getDrawable(R.drawable.semicolon_dark);
                keyItems[1].get(2).setCode(59);
                drawables[9] = getDrawable(R.drawable.left_tilt_line_dark);
                keyItems[1].get(1).setCode(92);
                drawables[10] = getDrawable(R.drawable.colon_dark);
                keyItems[1].get(0).setCode(58);
                drawables[11] = getDrawable(R.drawable.right_tilt_line_dark);
                keyItems[1].get(11).setCode(47);
                //w3
                drawables[12] = getDrawable(R.drawable.braces_square_close_dark);
                keyItems[2].get(13).setCode(93);
                drawables[13] = getDrawable(R.drawable.purnviram_dark);
                keyItems[2].get(12).setCode(124);
                drawables[14] = getDrawable(R.drawable.arrow_right_dark);
                keyItems[2].get(11).setCode(62);
                drawables[15] = getDrawable(R.drawable.underscore_dark);
                keyItems[2].get(10).setCode(95);
                drawables[16] = getDrawable(R.drawable.question_mark_dark);
                keyItems[2].get(9).setCode(63);
                drawables[17] = getDrawable(R.drawable.asterisk_dark);
                keyItems[2].get(8).setCode(42);
                drawables[18] = getDrawable(R.drawable.factorial_dark);
                keyItems[2].get(7).setCode(33);
                drawables[19] = getDrawable(R.drawable.hash_dark);
                keyItems[2].get(6).setCode(35);
                drawables[20] = getDrawable(R.drawable.arrow_up_dark);
                keyItems[2].get(5).setCode(94);
                drawables[21] = getDrawable(R.drawable.arrow_left_dark);
                keyItems[2].get(4).setCode(60);
                drawables[22] = getDrawable(R.drawable.percentage_dark);
                keyItems[2].get(3).setCode(37);
                drawables[23] = getDrawable(R.drawable.braces_circular_open_dark);
                keyItems[2].get(2).setCode(40);
                drawables[24] = getDrawable(R.drawable.braces_circular_close_dark);
                keyItems[2].get(1).setCode(41);
                drawables[25] = getDrawable(R.drawable.braces_curly_open_dark);
                keyItems[2].get(0).setCode(123);
                drawables[26] = getDrawable(R.drawable.braces_curly_close_dark);
                keyItems[2].get(15).setCode(125);
                drawables[27] = getDrawable(R.drawable.braces_square_open_dadrk);
                keyItems[2].get(14).setCode(91);
                //w4
                drawables[28] = getDrawable(R.drawable.number_eight_dark);
                keyItems[3].get(16).setCode(56);
                drawables[29] = getDrawable(R.drawable.number_nine_dark);
                keyItems[3].get(15).setCode(57);
                drawables[30] = getDrawable(R.drawable.dot_dark);
                keyItems[3].get(14).setCode(46);
                drawables[31] = getDrawable(R.drawable.attherate_dark);
                keyItems[3].get(13).setCode(64);
                drawables[32] = getDrawable(R.drawable.alphabet_key_dark);
                keyItems[3].get(12).setCode(305);
                drawables[33] = getDrawable(R.drawable.change_keyboard);
                keyItems[3].get(11).setCode(300);
                drawables[34] = getDrawable(R.drawable.settings_dark);
                keyItems[3].get(10).setCode(700);
                drawables[35] = getDrawable(R.drawable.dark_to_white);
                keyItems[3].get(9).setCode(310);
                drawables[36] = getDrawable(R.drawable.question_mark_dark);
                keyItems[3].get(8).setCode(63);
                drawables[37] = getDrawable(R.drawable.com_dark);
                keyItems[3].get(7).setCode(315);
                drawables[38] = getDrawable(R.drawable.copy_paste_dark);
                keyItems[3].get(6).setCode(320);
                drawables[39] = getDrawable(R.drawable.dollar_dark);
                keyItems[3].get(5).setCode(36);
                drawables[40] = getDrawable(R.drawable.number_zero_dark);
                keyItems[3].get(4).setCode(48);
                drawables[41] = getDrawable(R.drawable.number_one_dark);
                keyItems[3].get(3).setCode(49);
                drawables[42] = getDrawable(R.drawable.number_two_dark);
                keyItems[3].get(2).setCode(50);
                drawables[43] = getDrawable(R.drawable.number_three_dark);
                keyItems[3].get(1).setCode(51);
                drawables[44] = getDrawable(R.drawable.number_four_dark);
                keyItems[3].get(0).setCode(52);
                drawables[45] = getDrawable(R.drawable.number_five_dark);
                keyItems[3].get(19).setCode(53);
                drawables[46] = getDrawable(R.drawable.number_six_dark);
                keyItems[3].get(18).setCode(54);
                drawables[47] = getDrawable(R.drawable.number_seven_dark);
                keyItems[3].get(17).setCode(55);
                //w1
                drawables[48] = getDrawable(R.drawable.done_dark);
                drawables[49] = getDrawable(R.drawable.back_dark);
                keyItems[0].get(5).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(4).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(3).setCode(Keyboard.KEYCODE_DONE);
                keyItems[0].get(2).setCode(Keyboard.KEYCODE_DONE);
                break;
            default:
                drawables = null;
        }
        return drawables;
    }


    private void setCopyPasteLayout() {
        SharedPreferences sharedPreferences = getSharedPreferences("texts", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.copy_paste_layout, null);
        Button refreshButton = (Button) linearLayout.findViewById(R.id.refresh_button);
        Button backButton = (Button) linearLayout.findViewById(R.id.back_button);
        Button[] deleteButtons = {(Button) linearLayout.findViewById(R.id.del1),
                (Button) linearLayout.findViewById(R.id.del2),
                (Button) linearLayout.findViewById(R.id.del3),
                (Button) linearLayout.findViewById(R.id.del4),
                (Button) linearLayout.findViewById(R.id.del5)};
        final TextView[] copiedTexts = {(TextView) linearLayout.findViewById(R.id.edit1),
                (TextView) linearLayout.findViewById(R.id.edit2),
                (TextView) linearLayout.findViewById(R.id.edit3),
                (TextView) linearLayout.findViewById(R.id.edit4),
                (TextView) linearLayout.findViewById(R.id.edit5)};

        for (int i = 0; i < 5; i++) {
            copiedTexts[i].setText(sharedPreferences.getString("text" + i, ""));
            if (sharedPreferences.getString("text" + i, "").equals("")) {
                copiedTexts[i].setVisibility(View.INVISIBLE);
                deleteButtons[i].setVisibility(View.INVISIBLE);
            } else {
                copiedTexts[i].setVisibility(View.VISIBLE);
                deleteButtons[i].setVisibility(View.VISIBLE);
            }

            editor.putString("text" + i, copiedTexts[i].getText().toString());
            //copiedTexts[i].setWidth(displayMetrics.widthPixels*8/10);
            copiedTexts[i].setTextColor(Color.WHITE);
            final int finalI = i;
            copiedTexts[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (settings.getBoolean("vibrate", true))
                        vibrate();
                    paste(copiedTexts[finalI].getText().toString());
                }
            });
            deleteButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (finalI == 4) {
                        copiedTexts[finalI].setText("");
                        editor.putString("text" + finalI, "");
                        editor.apply();
                        return;
                    }
                    if (copiedTexts[finalI + 1].getText().toString().equals("")) {
                        copiedTexts[finalI].setText("");
                        editor.putString("text" + finalI, "");
                        editor.apply();
                    } else {
                        for (int j = finalI; j < 4; j++) {
                            copiedTexts[j].setText(copiedTexts[j + 1].getText().toString());
                            editor.putString("text" + j, copiedTexts[j + 1].getText().toString());
                        }
                        copiedTexts[4].setText("");
                        editor.putString("text4", "");
                        editor.apply();
                    }
                }
            });

            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vibrate();
                    setCopyPasteLayout();
                }
            });
        }
        String pretext = new String(getCopiedText());
        for (int i = 0; i < 5; i++) {
            if (!getCopiedText().equals("")) {
                if (!copiedTexts[i].getText().toString().equals("")) {
                    if (copiedTexts[i].getText().toString().equals(getCopiedText()))
                        break;
                    else {
                        String s = copiedTexts[i].getText().toString();
                        copiedTexts[i].setText(pretext);
                        editor.putString("text" + i, pretext);
                        editor.apply();
                        pretext = s;
                        continue;
                    }
                } else {
                    copiedTexts[i].setText(pretext);
                    editor.putString("text" + i, pretext);
                    editor.apply();
                    break;
                }
            } else break;
        }
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInputView(constraintLayout);
            }
        });
        setInputView(linearLayout);
    }

    private void paste(String s) {
        if (!s.equals("") || s != null) {
            getCurrentInputConnection().commitText(s, s.length());
        }
    }

    private String getCopiedText() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager.hasPrimaryClip()) {
            ClipData clipData = clipboardManager.getPrimaryClip();
            ClipData.Item item = clipData.getItemAt(0);
            if (item.getText() != null) {
                return item.getText().toString();
            } else return "";
        } else return "";
    }
}
