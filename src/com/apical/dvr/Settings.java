package com.apical.dvr;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    //++ settings for float button
    public static final String KEY_FLOAT_BTN_POS_X    = "DVR_FLOAT_BTN_POS_X";
    public static final String KEY_FLOAT_BTN_POS_Y    = "DVR_FLOAT_BTN_POS_Y";
    public static final String KEY_FLOAT_BTN_POS_SAVE = "DVR_FLOAT_BTN_POS_SAVE";

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
    public static final int DEF_HANDLE_BACK_KEY_TYPE = 1; // 0 - finish activity and stop record service
                                                          // 1 - hide activity only
    //-- settings for back key handler

    //++ settings for power on auto record
    public static final String KEY_POWERON_AUTO_RECORD = "KEY_POWERON_AUTO_RECORD";
    public static final int DEF_POWERON_AUTO_RECORD = 0;
    //-- settings for power on auto record

    //++ settings for insert sdcard auto record
    public static final String KEY_INSERTSD_AUTO_RECORD = "KEY_INSERTSD_AUTO_RECORD";
    public static final int DEF_INSERTSD_AUTO_RECORD = 1;
    //-- settings for insert sdcard auto record

    //++ settings for record mic mute
    public static final String KEY_RECORD_MIC_MUTE = "KEY_RECORD_MIC_MUTE";
    public static final int DEF_RECORD_MIC_MUTE = 1;
    //-- settings for record mic mute


    //++ settings for video quality
    public static final String KEY_VIDEO_QUALITY = "KEY_VIDEO_QUALITY";
    public static final int DEF_VIDEO_QUALITY = 0;
    //-- settings for video quality

    //++ settings for record max duration
    public static final String KEY_RECORD_DURATION = "KEY_RECORD_DURATION";
    public static final int DEF_RECORD_DURATION = 60000;
    //-- settings for record max duration

    //++ settings for impact detect level
    public static final String KEY_IMPACT_DETECT_LEVEL = "KEY_IMPACT_DETECT_LEVEL";
    public static final int DEF_IMPACT_DETECT_LEVEL = 1; // 0 - high, 1 - middle, 2 - low, 3 - close
    //-- settings for impact detect level

    // default impact duration
    public static final int DEF_IMPACT_DURATION = 60000;

    //++ settings for watermark enable/disable
    public static final String KEY_WATERMARK_ENABLE = "KEY_WATERMARK_ENABLE";
    public static final int DEF_WATERMARK_ENABLE = 1;
    //-- settings for watermark enable/disable

    private static final String DVR_SETTINGS_SHARED_PREFS = "DVR_SETTINGS_SHARED_PREFS";
    private static Context           mContext;
    private static SharedPreferences mSharedPref;

    public static void init(Context c) {
        mContext    = c;
        mSharedPref = mContext.getSharedPreferences(DVR_SETTINGS_SHARED_PREFS, Context.MODE_PRIVATE);
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

