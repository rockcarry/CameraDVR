package com.apical.dvr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class CameraActivity extends Activity
    implements View.OnClickListener,
               View.OnLongClickListener,
               SdcardManager.SdStateChangeListener,
               GSensorMonitor.ImpactEventListener,
               MiscEventMonitor.MiscEventListener
{
    private static final String TAG = "CameraActivity";
    private static final int MSG_AUTO_HIDE_UI_BUTTONS       = 1;
    private static final int MSG_UPDATE_RECORDING_INDICATOR = 2;
    private static final int MSG_UPDATE_IMPACT_LOCK_VIEW    = 3;

    private TextureView      mCamMainPreview;
    private TextureView      mCamUsbPreview;
    private View             mFlashView;
    private RelativeLayout   mCamVideoUI;
    private ImageView        mBtnGallery;
    private ImageView        mBtnSettings;
    private ImageView        mBtnShutter;
    private ImageView        mBtnMuteSW;
    private ImageView        mBtnCameraSW;
    private ImageView        mImpactLock;
    private TextView         mTxtRecTime;
    private AnimationManager mAnimManager;
    private boolean          mIndicatorShown;

    private FrameLayout.LayoutParams mCamMainPreviewLayoutParams;
    private FrameLayout.LayoutParams mCamUsbPreviewLayoutParams ;

    private RecordService mRecServ = null;
    private ServiceConnection mRecServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serv) {
            mRecServ = ((RecordService.RecordBinder)serv).getService(CameraActivity.this);
            mRecServ.setCamMainPreviewTexture(mCamMainTexture);
            mRecServ.setCamUsbPreviewTexture (mCamUsbTexture );
            mRecServ.onResume();
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

        mCamMainPreview = (TextureView)findViewById(R.id.camera_main_preview);
        mCamMainPreview.setSurfaceTextureListener(mCamMainSurfaceTextureListener);

        mCamUsbPreview = (TextureView)findViewById(R.id.camera_usb_preview);
        mCamUsbPreview.setSurfaceTextureListener(mCamUsbSurfaceTextureListener);

        mCamMainPreviewLayoutParams = (FrameLayout.LayoutParams) mCamMainPreview.getLayoutParams();
        mCamUsbPreviewLayoutParams  = (FrameLayout.LayoutParams) mCamUsbPreview .getLayoutParams();

        mFlashView  = (View)findViewById(R.id.view_flash_overlay);
        mCamVideoUI = (RelativeLayout)findViewById(R.id.view_camera_videoui);
        mBtnGallery = (ImageView)findViewById(R.id.btn_dvr_gallery);
        mBtnSettings= (ImageView)findViewById(R.id.btn_dvr_settings);
        mBtnShutter = (ImageView)findViewById(R.id.btn_dvr_shutter);
        mBtnMuteSW  = (ImageView)findViewById(R.id.btn_recmic_mute_switcher);
        mBtnCameraSW= (ImageView)findViewById(R.id.btn_dvr_camera_switcher);
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

        // audo hide controls
        showUIControls(false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // unbind record service
        unbindService(mRecServiceConn);

        // stop record service
        Intent i = new Intent(CameraActivity.this, RecordService.class);
        stopService(i);

        // remove all messages
        mHandler.removeMessages(0);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateButtonsState();
        updateImpactLockView();

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
        case R.id.btn_dvr_shutter:
            startRecording(!mRecServ.isRecording());
            break;
        case R.id.btn_recmic_mute_switcher:
            setRecMicMute(!mRecServ.getRecMicMute());
            break;
        case R.id.btn_dvr_camera_switcher:
            switchCamera();
            break;
        case R.id.btn_dvr_settings: {
                SettingsDialog dlg = new SettingsDialog(this);
                dlg.show();
            }
            break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
        case R.id.view_camera_videoui:
            if (SdcardManager.isSdcardInsert()) {
                mRecServ.takePhoto((mRecServ.getCamSwitchState() & (1 << 0)) == 0 ? 0 : 1);
                mAnimManager.startFlashAnimation(mFlashView);
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
        updateButtonsState();
    }

    @Override
    public void onGsensorImpactEvent(long time) {
        mHandler.removeMessages(MSG_UPDATE_IMPACT_LOCK_VIEW);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_IMPACT_LOCK_VIEW, Settings.DEF_IMPACT_DURATION);
        updateImpactLockView();
    }

    @Override
    public void onDeviceShutdown() {}

    @Override
    public void onUsbCamStateChanged(boolean connected) {
        mRecServ.setCamUsbPreviewTexture(mCamUsbTexture);
        updateCameraSwitchPreviewUI();
        updateButtonsState();
    }

    private void setRecMicMute(boolean mute) {
        mRecServ.setRecMicMute(mute);
        updateButtonsState();
    }

    private void startRecording(boolean start) {
        if (start) {
            if (SdcardManager.isSdcardInsert()){
                mRecServ.startRecording();
            }
        }
        else {
            mRecServ.stopRecording();
        }
        updateButtonsState();
        updateImpactLockView();
    }

    private void switchCamera() {
        mRecServ.switchCamera();
        updateCameraSwitchPreviewUI();
        updateButtonsState();
    }

    private void updateImpactLockView() {
        boolean impact = (mRecServ != null) && mRecServ.isRecording()
                         && (mRecServ.getImpactTime() + Settings.DEF_IMPACT_DURATION) > SystemClock.uptimeMillis();
        if (impact) {
            mImpactLock.setVisibility(View.VISIBLE);
        }
        else {
            mImpactLock.setVisibility(View.INVISIBLE);
        }
    }

    private void updateButtonsState() {
        if (mRecServ != null && mRecServ.isRecording()) {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_recording);

            //++ for recording indicator
            if (!mIndicatorShown) {
                mHandler.sendEmptyMessage(MSG_UPDATE_RECORDING_INDICATOR);
                mTxtRecTime.setVisibility(View.VISIBLE);
                mIndicatorShown = true;
            }
            //-- for recording indicator

            mBtnSettings.setVisibility(View.INVISIBLE);
        }
        else {
            mBtnShutter.setImageResource(R.drawable.btn_new_shutter_video);

            //++ for recording indicator
            mHandler.removeMessages(MSG_UPDATE_RECORDING_INDICATOR);
            mTxtRecTime.setVisibility(View.INVISIBLE);
            mIndicatorShown = false;
            //-- for recording indicator

            mBtnSettings.setVisibility(View.VISIBLE);
        }

        if (mRecServ != null && mRecServ.getRecMicMute()) {
            mBtnMuteSW.setImageResource(R.drawable.btn_new_recmic_mute_off);
        }
        else {
            mBtnMuteSW.setImageResource(R.drawable.btn_new_recmic_mute_on );
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
        mCamMainPreview.setVisibility(View.INVISIBLE);
        mCamUsbPreview .setVisibility(View.INVISIBLE);
        switch (state) {
        case 0: // ab
            mCamMainPreview.setLayoutParams(mCamMainPreviewLayoutParams);
            mCamUsbPreview .setLayoutParams(mCamUsbPreviewLayoutParams );
            mCamUsbPreview .bringToFront();
            mCamMainPreview.setVisibility(View.VISIBLE);
            mCamUsbPreview .setVisibility(View.VISIBLE);
            break;
        case 1: // ba
            mCamUsbPreview .setLayoutParams(mCamMainPreviewLayoutParams);
            mCamMainPreview.setLayoutParams(mCamUsbPreviewLayoutParams );
            mCamMainPreview.bringToFront();
            mCamMainPreview.setVisibility(View.VISIBLE);
            mCamUsbPreview .setVisibility(View.VISIBLE);
            break;
        case 2: // a
            mCamMainPreview.setLayoutParams(mCamMainPreviewLayoutParams);
            mCamMainPreview.bringToFront();
            mCamMainPreview.setVisibility(View.VISIBLE);
            break;
        case 3: // b
            mCamUsbPreview .setLayoutParams(mCamMainPreviewLayoutParams);
            mCamUsbPreview .bringToFront();
            mCamUsbPreview .setVisibility(View.VISIBLE);
            break;
        }
    }

    private SurfaceTexture mCamMainTexture = null;
    private TextureView.SurfaceTextureListener mCamMainSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mCamMainTexture = texture;
            if (mRecServ != null) {
                mRecServ.setCamMainPreviewTexture(texture);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            mCamMainTexture = null;
            if (mRecServ != null) {
                mRecServ.setCamMainPreviewTexture(null);
            }
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private SurfaceTexture mCamUsbTexture = null;
    private TextureView.SurfaceTextureListener mCamUsbSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mCamUsbTexture = texture;
            if (mRecServ != null) {
                mRecServ.setCamUsbPreviewTexture(texture);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            mCamUsbTexture = null;
            if (mRecServ != null) {
                mRecServ.setCamUsbPreviewTexture(null);
            }
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private Handler mHandler = new Handler() {
        private boolean blink = false;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_AUTO_HIDE_UI_BUTTONS:
                mBtnGallery .setVisibility(View.INVISIBLE);
                mBtnSettings.setVisibility(View.INVISIBLE);
                mBtnShutter .setVisibility(View.INVISIBLE);
                mBtnMuteSW  .setVisibility(View.INVISIBLE);
                mBtnCameraSW.setVisibility(View.INVISIBLE);
                break;

            case MSG_UPDATE_RECORDING_INDICATOR:
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_RECORDING_INDICATOR, 1000);
                SimpleDateFormat f = new SimpleDateFormat("mm:ss");
                long time = SystemClock.uptimeMillis() - mRecServ.getRecordingStartTime();
                if (time < 0 || time > mRecServ.getRecordingMaxDuration()) {
                    time = 0;
                }
                mTxtRecTime.setText(f.format(time - TimeZone.getDefault().getRawOffset()));
                mTxtRecTime.setCompoundDrawablesWithIntrinsicBounds(
                    blink ? R.drawable.ic_recording_indicator_0 : R.drawable.ic_recording_indicator_1, 0, 0, 0);
                blink = !blink;
                break;

            case MSG_UPDATE_IMPACT_LOCK_VIEW:
                updateImpactLockView();
                break;
            }
        }
    };

    private void showUIControls(boolean show) {
        if (show) {
            mHandler.removeMessages(MSG_AUTO_HIDE_UI_BUTTONS);
            mBtnGallery .setVisibility(View.VISIBLE);
            mBtnShutter .setVisibility(View.VISIBLE);
            mBtnMuteSW  .setVisibility(View.VISIBLE);
            mBtnCameraSW.setVisibility(View.VISIBLE);
            if (mRecServ != null && !mRecServ.isRecording()) {
                mBtnSettings.setVisibility(View.VISIBLE);
            }
        }
        else {
            mHandler.sendEmptyMessageDelayed(MSG_AUTO_HIDE_UI_BUTTONS, 5000);
        }
    }
}




