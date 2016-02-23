package com.apical.cdr;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    //++ settings for float button
    public static final String KEY_FLOAT_BTN_POS_X    = "CDR_FLOAT_BTN_POS_X";
    public static final String KEY_FLOAT_BTN_POS_Y    = "CDR_FLOAT_BTN_POS_Y";
    public static final String KEY_FLOAT_BTN_POS_SAVE = "CDR_FLOAT_BTN_POS_SAVE";

    public static final int DEF_FLOAT_BTN_POS_X    = 0;
    public static final int DEF_FLOAT_BTN_POS_Y    = 0;
    public static final int DEF_FLOAT_BTN_POS_SAVE = 1;
    //-- settings for float button

    //++ settings for camera switch state
    public static final String KEY_CAMERA_SWITCH_STATE_VALUE = "KEY_CAMERA_SWITCH_STATE_VALUE";
    public static final String KEY_CAMERA_SWITCH_STATE_SAVE  = "KEY_CAMERA_SWITCH_STATE_SAVE";

    public static final int DEF_CAMERA_SWITCH_STATE_VALUE = 0;
    public static final int DEF_CAMERA_SWITCH_STATE_SAVE = 1;
    //-- settings for camera switch state

    //++ settings for back key handler
    public static final String KEY_HANDLE_BACK_KEY_TYPE = "KEY_HANDLE_BACK_KEY_TYPE";
    public static final int DEF_HANDLE_BACK_KEY_TYPE = 1;
    //-- settings for back key handler

    private static final String TAG = "GSensorMonitor";
    private static final String CDR_SETTINGS_SHARED_PREFS = "CDR_SETTINGS_SHARED_PREFS";

    private static Context           mContext;
    private static SharedPreferences mSharedPref;

    public static void init(Context c) {
        mContext    = c;
        mSharedPref = mContext.getSharedPreferences(CDR_SETTINGS_SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public static void set(String key, int value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int get(String key, int def) {
        return mSharedPref.getInt(key, def);
    }
};

