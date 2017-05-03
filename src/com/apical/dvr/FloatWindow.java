package com.apical.dvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.util.Log;

public class FloatWindow {
    private static final String TAG = "FloatWindow";

    private Context       mContext       = null;
    private WindowManager mWinMan        = null;
    private LinearLayout  mFloatLayout   = null;
    private LayoutParams  mLayoutParams  = null;
    private ImageView     mDvrButton     = null;
    private boolean       mDisplayed     = false;
    private int           mLastFloatPosX = -1;
    private int           mLastFloatPosY = -1;
    private boolean       mMoveFloatFlag = false;
    private int           mDvrBtnState   = 0;
    private boolean       mRecording     = false;
    private boolean       mIsScreenOn    = true;
    private Handler       mHandler       = new Handler();

    public FloatWindow(Context context) {
        mContext     = context;
        mWinMan      = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mFloatLayout = (LinearLayout )LayoutInflater.from(mContext).inflate(R.layout.float_win, null);
        mDvrButton   = (ImageView    )mFloatLayout.findViewById(R.id.camera_dvr_btn);

        mLayoutParams  = new LayoutParams();
        mLayoutParams.type    = LayoutParams.TYPE_PHONE;
        mLayoutParams.format  = PixelFormat.RGBA_8888;
        mLayoutParams.flags   = LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.x       = Settings.get(Settings.KEY_FLOAT_BTN_POS_X, Settings.DEF_FLOAT_BTN_POS_X);
        mLayoutParams.y       = Settings.get(Settings.KEY_FLOAT_BTN_POS_Y, Settings.DEF_FLOAT_BTN_POS_Y);
        mLayoutParams.width   = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height  = WindowManager.LayoutParams.WRAP_CONTENT;

        mDvrButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    switch (mDvrBtnState) {
                    case 1: mDvrButton.setBackgroundResource(R.drawable.dvr_float_btn_normal_1); break;
                    case 2: mDvrButton.setBackgroundResource(R.drawable.dvr_float_btn_record_1); break;
                    }
                    mLastFloatPosX = (int) event.getRawX();
                    mLastFloatPosY = (int) event.getRawY();
                    mMoveFloatFlag = false;
                    break;
                case MotionEvent.ACTION_UP:
                    switch (mDvrBtnState) {
                    case 1: mDvrButton.setBackgroundResource(R.drawable.dvr_float_btn_normal_0); break;
                    case 2: mDvrButton.setBackgroundResource(R.drawable.dvr_float_btn_record_0); break;
                    }
                    ret = mMoveFloatFlag;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();
                    if (!(  x >= mLastFloatPosX && x < mLastFloatPosX + mFloatLayout.getMeasuredWidth()
                         && y >= mLastFloatPosY && y < mLastFloatPosY + mFloatLayout.getMeasuredHeight() ) )
                    {
                        mMoveFloatFlag = true;
                    }
                    if (mMoveFloatFlag) {
                        mLayoutParams.x = x - mFloatLayout.getMeasuredWidth () / 2;
                        mLayoutParams.y = y - mFloatLayout.getMeasuredHeight() / 2 - 25;
                        mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);

                        if (Settings.get(Settings.KEY_FLOAT_BTN_POS_SAVE, Settings.DEF_FLOAT_BTN_POS_SAVE) != 0) {
                            Settings.set(Settings.KEY_FLOAT_BTN_POS_X, mLayoutParams.x);
                            Settings.set(Settings.KEY_FLOAT_BTN_POS_Y, mLayoutParams.y);
                        }
                    }
                    break;
                }
                return ret;
            }
        });

        mDvrButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CameraActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(i);
            }
        });
    }

    public void create() {
        // register system event receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON );
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mSystemEventReceiver, filter);
    }

    public void destroy() {
        // unregister system event receiver
        mContext.unregisterReceiver(mSystemEventReceiver);
    }

    public void showFloat(boolean recording) {
        mRecording = recording;
        mHandler.removeCallbacks(mShowFloatRunnable);
        mHandler.postDelayed(mShowFloatRunnable, 800);
    }

    public void hideFloat() {
        mHandler.removeCallbacks(mShowFloatRunnable);
        if (mDisplayed) {
            mWinMan.removeView(mFloatLayout);
            mDisplayed = false;
        }
    }

    public void updateFloat(boolean recording) {
        mRecording = recording;
        mDvrButton.setBackgroundResource(mRecording ? R.drawable.dvr_float_btn_record_0 : R.drawable.dvr_float_btn_normal_0);
        if (mDisplayed) {
            mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
        }
        mDvrBtnState = mRecording ? 2 : 1;
    }

    private Runnable mShowFloatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsScreenOn) {
                return;
            }

            mDvrButton.setBackgroundResource(mRecording ? R.drawable.dvr_float_btn_record_0 : R.drawable.dvr_float_btn_normal_0);
            if (mDisplayed) {
                mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
            }
            else {
                mWinMan.addView(mFloatLayout, mLayoutParams);
            }
            mDvrBtnState = mRecording ? 2 : 1;
            mDisplayed   = true;
        }
    };

    private BroadcastReceiver mSystemEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mIsScreenOn = true;
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mIsScreenOn = false;
            }
        }
    };
}

