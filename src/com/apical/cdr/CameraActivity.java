package com.apical.cdr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CameraActivity extends Activity
    implements View.OnClickListener, View.OnLongClickListener, SdcardManager.SdStateChangeListener
{
    private static final String TAG = "CameraActivity";

    private int            mCurMainCam;
    private int            mCurUsbCam;
    private SurfaceView    mFullPreview;
    private SurfaceView    mSmallPreview;
    private View           mFlashView;
    private RelativeLayout mCamVideoUI;
    private ImageView      mBtnGallery;
    private ImageView      mBtnSettings;
    private ImageView      mBtnShutter;
    private ImageView      mBtnMuteSW;
    private ImageView      mBtnCameraSW;
    private ImageView      mImpactLock;
    private TextView       mTxtRecTime;
    private AnimationManager   mAnimManager;
    private PowerManager.WakeLock mWakeLock;
    Handler mHandler = new Handler();

    private RecordService mRecServ = null;
    private ServiceConnection mRecServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serv) {
            mRecServ = ((RecordService.RecordBinder)serv).getService(CameraActivity.this);
            mRecServ.selectCamera(mCurMainCam, mCurUsbCam);

            updateCameraSwitchPreviewUI();

            updateButtonsState();
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

        // init settings
        Settings.init(this);

        mFullPreview = (SurfaceView)findViewById(R.id.camera_fullwin_preview);
        mFullPreview.getHolder().addCallback(mFullPreviewSurfaceHolderCallback);

        mSmallPreview = (SurfaceView)findViewById(R.id.camera_smallwin_preview);
        mSmallPreview.getHolder().addCallback(mSmallPreviewSurfaceHolderCallback);

        mFlashView  = (View)findViewById(R.id.view_flash_overlay);
        mCamVideoUI = (RelativeLayout)findViewById(R.id.view_camera_videoui);
        mBtnGallery = (ImageView)findViewById(R.id.btn_cdr_gallery);
        mBtnSettings= (ImageView)findViewById(R.id.btn_cdr_settings);
        mBtnShutter = (ImageView)findViewById(R.id.btn_cdr_shutter);
        mBtnMuteSW  = (ImageView)findViewById(R.id.btn_recmic_mute_switcher);
        mBtnCameraSW= (ImageView)findViewById(R.id.btn_cdr_camera_switcher);
        mImpactLock = (ImageView)findViewById(R.id.ic_impact_lock);
        mTxtRecTime = (TextView )findViewById(R.id.text_recording_time);

        mCamVideoUI .setOnLongClickListener(this);
        mBtnGallery .setOnClickListener(this);
        mBtnSettings.setOnClickListener(this);
        mBtnShutter .setOnClickListener(this);
        mBtnMuteSW  .setOnClickListener(this);
        mBtnCameraSW.setOnClickListener(this);

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

        updateButtonsState();

        if (mRecServ != null) {
            mRecServ.onResume();
        }

        showUIControls(true );
        showUIControls(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecServ != null) {
            mRecServ.onPause();
        }
    }

    @Override
    public void onBackPressed() {
        int type = Settings.get(Settings.KEY_HANDLE_BACK_KEY_TYPE, Settings.DEF_HANDLE_BACK_KEY_TYPE);
        switch (type) {
        case 0: super.onBackPressed(); break;
        case 1: moveTaskToBack(true);  break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_cdr_shutter:
            startRecording(!mRecServ.isRecording());
            break;
        case R.id.btn_recmic_mute_switcher:
            setRecMicMute(!mRecServ.getRecMicMuted());
            break;
        case R.id.btn_cdr_camera_switcher:
            switchCamera();
            break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
        case R.id.view_camera_videoui:
            if (SdcardManager.isSdcardInsert()) {
                int type = (mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1;
                mRecServ.takePhoto(type, new android.hardware.Camera.ShutterCallback() {
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

    @Override
    public void onSdStateChanged(boolean insert) {
        startRecording(insert);
    }

    public void updateImpactLockView() {
        boolean impact = mRecServ.getMediaSaver().getGsensorImpactFlag();
        if (mRecServ.isRecording() && impact) {
            mImpactLock.setVisibility(View.VISIBLE);
        }
        else {
            mImpactLock.setVisibility(View.GONE);
        }
    }

    private void startRecording(boolean start) {
        if (start) {
            if (SdcardManager.isSdcardInsert()){
                if (mRecServ.startRecording()) {
                    mWakeLock.acquire(); // acquire wake lock
                    mHandler.post(mRecordingTimeUpdater);
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
            mWakeLock.release(); // release wake lock
        }
        updateButtonsState();
        updateImpactLockView();
    }

    private void setRecMicMute(boolean mute) {
        mRecServ.setRecMicMuted(mute);
        updateButtonsState();
    }

    private void switchCamera() {
        mRecServ.switchCamera();
        updateCameraSwitchPreviewUI();
        updateButtonsState();
    }

    private void updateButtonsState() {
        if (mRecServ != null && mRecServ.isRecording()) {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_recording);
        }
        else {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_video);
        }

        if (mRecServ != null && mRecServ.getRecMicMuted()) {
            mBtnMuteSW.setImageResource(R.drawable.btn_new_recmic_mute_off);
        }
        else {
            mBtnMuteSW.setImageResource(R.drawable.btn_new_recmic_mute_on);
        }

        if (mRecServ != null) {
            switch (mRecServ.getCamSwitchState()) {
            case 0:
                mBtnCameraSW.setImageResource(R.drawable.btn_new_camera_switch_ab);
                break;
            case 1:
                mBtnCameraSW.setImageResource(R.drawable.btn_new_camera_switch_ba);
                break;
            case 2:
                mBtnCameraSW.setImageResource(R.drawable.btn_new_camera_switch_a );
                break;
            case 3:
                mBtnCameraSW.setImageResource(R.drawable.btn_new_camera_switch_b );
                break;
            }
        }
        else {
            mBtnCameraSW.setImageResource(R.drawable.btn_new_camera_switch_a);
        }
    }

    private void updateCameraSwitchPreviewUI() {
        int state = mRecServ.getCamSwitchState();
        int type  = (state & (1 << 0)) == 0 ? 0 : 1;
        switch (type) {
        case 0:
            mRecServ.setPreviewSurfaceHolderMainCam(mFullPreview.getHolder());
            mRecServ.setPreviewSurfaceHolderUsbCam (mSmallPreview.getHolder());
            break;
        case 1:
            mRecServ.setPreviewSurfaceHolderMainCam(mSmallPreview.getHolder());
            mRecServ.setPreviewSurfaceHolderUsbCam (mFullPreview.getHolder());
            break;
        }
        if ((state & (1 << 1)) == 0) {
            mSmallPreview.setVisibility(View.VISIBLE);
        }
        else {
            mSmallPreview.setVisibility(View.GONE);
        }
    }

    private SurfaceHolder.Callback mFullPreviewSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            if (mRecServ != null) {
                int type = (mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1;
                switch (type) {
                case 0: mRecServ.setPreviewSurfaceHolderMainCam(holder); break;
                case 1: mRecServ.setPreviewSurfaceHolderUsbCam (holder); break;
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            if (mRecServ != null) {
                int type = (mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1;
                switch (type) {
                case 0: mRecServ.setPreviewSurfaceHolderMainCam(null); break;
                case 1: mRecServ.setPreviewSurfaceHolderUsbCam (null); break;
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surfaceChanged");
        }
    };

    private SurfaceHolder.Callback mSmallPreviewSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            if (mRecServ != null) {
                int type = (mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1;
                switch (type) {
                case 1: mRecServ.setPreviewSurfaceHolderMainCam(holder); break;
                case 0: mRecServ.setPreviewSurfaceHolderUsbCam (holder); break;
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            if (mRecServ != null) {
                int type = (mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1;
                switch (type) {
                case 1: mRecServ.setPreviewSurfaceHolderMainCam(null); break;
                case 0: mRecServ.setPreviewSurfaceHolderUsbCam (null); break;
                }
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
            mBtnGallery .setVisibility(View.GONE);
            mBtnSettings.setVisibility(View.GONE);
            mBtnShutter .setVisibility(View.GONE);
            mBtnMuteSW  .setVisibility(View.GONE);
            mBtnCameraSW.setVisibility(View.GONE);
        }
    };

    private void showUIControls(boolean show) {
        if (show) {
            mHandler.removeCallbacks(mUIControlsHider);
            mBtnGallery .setVisibility(View.VISIBLE);
            mBtnSettings.setVisibility(View.VISIBLE);
            mBtnShutter .setVisibility(View.VISIBLE);
            mBtnMuteSW  .setVisibility(View.VISIBLE);
            mBtnCameraSW.setVisibility(View.VISIBLE);
        }
        else {
            mHandler.postDelayed(mUIControlsHider, 5000);
        }
    }
}




