package com.apical.cdr;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PixelFormat;
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
    private ImageView     mCdrButton     = null;
    private boolean       mDisplayed     = false;
    private int           mLastFloatPosX = -1;
    private int           mLastFloatPosY = -1;
    private boolean       mMoveFloatFlag = false;
    private int           cdr_btn_state  = 0;

    public FloatWindow(Context context) {
        mContext     = context;
        mWinMan      = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mFloatLayout = (LinearLayout )LayoutInflater.from(mContext).inflate(R.layout.float_win, null);
        mCdrButton   = (ImageView    )mFloatLayout.findViewById(R.id.camera_cdr_btn);

        mLayoutParams  = new LayoutParams();
        mLayoutParams.type    = LayoutParams.TYPE_PHONE;
        mLayoutParams.format  = PixelFormat.RGBA_8888;
        mLayoutParams.flags   = LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.x       = 0;
        mLayoutParams.y       = 0;
        mLayoutParams.width   = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height  = WindowManager.LayoutParams.WRAP_CONTENT;

        mCdrButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    switch (cdr_btn_state) {
                    case 1: mCdrButton.setBackgroundResource(R.drawable.cdr_float_btn_normal_1); break;
                    case 2: mCdrButton.setBackgroundResource(R.drawable.cdr_float_btn_record_1); break;
                    }
                    mLastFloatPosX = (int) event.getRawX();
                    mLastFloatPosY = (int) event.getRawY();
                    mMoveFloatFlag = false;
                    break;
                case MotionEvent.ACTION_UP:
                    switch (cdr_btn_state) {
                    case 1: mCdrButton.setBackgroundResource(R.drawable.cdr_float_btn_normal_0); break;
                    case 2: mCdrButton.setBackgroundResource(R.drawable.cdr_float_btn_record_0); break;
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
                    }
                    break;
                }
                return ret;
            }
        });

        mCdrButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent        i = new Intent();
                ComponentName c = new ComponentName("com.apical.cdr", "com.apical.cdr.CameraActivity");
                i.setComponent(c);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(i);
            }
        });
    }

    public void showFloat(boolean recording) {
        Log.d(TAG, "showFloat " + recording);
        mCdrButton.setBackgroundResource(recording ? R.drawable.cdr_float_btn_record_0 : R.drawable.cdr_float_btn_normal_0);
        if (mDisplayed) {
            mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
        }
        else {
            mWinMan.addView(mFloatLayout, mLayoutParams);
        }
        cdr_btn_state = recording ? 2 : 1;
        mDisplayed    = true;
    }

    public void hideFloat() {
        Log.d(TAG, "hideFloat");
        mWinMan.removeView(mFloatLayout);
        mDisplayed    = false;
    }

    public void updateFloat(boolean recording) {
        Log.d(TAG, "updateFloat " + recording);
        if (!mDisplayed) {
            return;
        }
        mCdrButton.setBackgroundResource(recording ? R.drawable.cdr_float_btn_record_0 : R.drawable.cdr_float_btn_normal_0);
        mWinMan.updateViewLayout(mFloatLayout, mLayoutParams);
        cdr_btn_state = recording ? 2 : 1;
    }
}

