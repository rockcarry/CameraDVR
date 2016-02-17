package com.apical.cdr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;

import com.apical.cdr.widget.ShutterButton;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CameraActivity extends Activity
    implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "CameraActivity";
    private static final String CAMERA_TEST_APP_SHARED_PREFS = "CAMERA_TEST_APP_SHARED_PREFS";
    private static final String CAMERA_TEST_APP_CURRENT_CAM  = "CAMERA_TEST_APP_CURRENT_CAM";

    private SharedPreferences mSharedPref;
    private int            mCurrentCamera;
    private SurfaceView    mPreview;
    private View           mFlashView;
    private RelativeLayout mCamVideoUI;
    private ShutterButton  mBtnShutter;
    private TextView       mTxtRecTime;
    private AnimationManager   mAnimManager;
    private PowerManager.WakeLock mWakeLock;
    Handler mHandler = new Handler();

    private RecordService.RecordBinder mRecServ = null;
    private ServiceConnection mRecServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serv) {
            mRecServ = (RecordService.RecordBinder)serv;
            mRecServ.selectCamera(mCurrentCamera);
            mRecServ.setPreviewSurfaceHolder(mPreview.getHolder());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecServ = null;
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSharedPref     = getSharedPreferences(CAMERA_TEST_APP_SHARED_PREFS, MODE_PRIVATE);
        mCurrentCamera  = mSharedPref.getInt(CAMERA_TEST_APP_CURRENT_CAM , 0);

        mPreview = (SurfaceView)findViewById(R.id.camera_preview_view);
        mPreview.getHolder().addCallback(mPreviewSurfaceHolderCallback);

        mFlashView  = (View)findViewById(R.id.view_flash_overlay);
        mCamVideoUI = (RelativeLayout)findViewById(R.id.view_camera_videoui);
        mBtnShutter = (ShutterButton)findViewById(R.id.btn_startstop_record);
        mTxtRecTime = (TextView)findViewById(R.id.text_recording_time);

        mCamVideoUI.setOnLongClickListener(this);
        mBtnShutter.setOnClickListener(this);

        // start record service
        Intent i = new Intent(CameraActivity.this, RecordService.class);
        startService(i);

        // bind record service
        bindService(i, mRecServiceConn, Context.BIND_AUTO_CREATE);

        mAnimManager = new AnimationManager();

        // for wake lock
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);

        // audo hide controls
        showUIControls(false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // stop recording
        mRecServ.stopRecording();

        // unbind record service
        unbindService(mRecServiceConn);

        // stop record service
        Intent i = new Intent(CameraActivity.this, RecordService.class);
        stopService(i);

        // for wake lock
        if (mWakeLock != null) {
            mWakeLock.release();
        }

        mHandler.removeCallbacks(mRecordingTimeUpdater);
        mHandler.removeCallbacks(mUIControlsHider     );

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRecServ != null && mRecServ.isRecording()) {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_recording);
        }
        else {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_video);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_startstop_record:
            if (!mRecServ.isRecording()) {
                if (SdcardManager.isSdcardInsert()){
                    if (mRecServ.startRecording()) {
                        mWakeLock.acquire(); // acquire wake lock
                        mHandler.post(mRecordingTimeUpdater);
                        mBtnShutter.setImageResource(R.drawable.btn_new_shutter_recording);
                        mTxtRecTime.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    // todo..
                }
            }
            else {
                mHandler.removeCallbacks(mRecordingTimeUpdater);
                mTxtRecTime.setVisibility(View.GONE);
                mRecServ.stopRecording();
                mBtnShutter.setImageResource(R.drawable.btn_new_shutter_video);
                mWakeLock.release(); // release wake lock
            }
            break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
        case R.id.view_camera_videoui:
            if (SdcardManager.isSdcardInsert()) {
                mRecServ.takePhoto(new android.hardware.Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        mAnimManager.startFlashAnimation(mFlashView);
                    }
                });
            }
            else {
                // todo..
            }
            return true;
        }
        return false;
    }

    @Override  
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showUIControls(true );
            showUIControls(false);
        }
        return super.dispatchTouchEvent(ev);
    }

    private SurfaceHolder.Callback mPreviewSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            if (mRecServ != null) {
                mRecServ.setPreviewSurfaceHolder(holder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            if (mRecServ != null) {
                mRecServ.setPreviewSurfaceHolder(null);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surfaceChanged");
        }
    };

    private Runnable mRecordingTimeUpdater = new Runnable() {
        private boolean blink = false;

        @Override
        public void run() {
            mHandler.postDelayed(this, 1000);

            SimpleDateFormat f = new SimpleDateFormat("mm:ss");
            long time = System.currentTimeMillis() - mRecServ.getRecordingStartTime();
            if (time < 0 || time > mRecServ.getRecordingMaxDuration()) {
                time = 0;
            }
            mTxtRecTime.setText(f.format(time - TimeZone.getDefault().getRawOffset()));
            mTxtRecTime.setCompoundDrawablesWithIntrinsicBounds(
                blink ? R.drawable.ic_recording_indicator_0 : R.drawable.ic_recording_indicator_1, 0, 0, 0);
            blink = !blink;
        }
    };

    private Runnable mUIControlsHider = new Runnable() {
        @Override
        public void run() {
            mBtnShutter.setVisibility(View.GONE);
        }
    };

    private void showUIControls(boolean show) {
        if (show) {
            mHandler.removeCallbacks(mUIControlsHider);
            mBtnShutter.setVisibility(View.VISIBLE);
        }
        else {
            mHandler.postDelayed(mUIControlsHider, 5000);
        }
    }
}




