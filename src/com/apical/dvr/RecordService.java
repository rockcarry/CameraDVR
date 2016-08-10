package com.apical.dvr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.android.camera.exif.Exif;
import com.android.camera.exif.ExifInterface;

public class RecordService extends Service
{
    private static final String TAG = "RecordService";
    private static final int CAMERA_VIDEO_WIDTH   = 800;
    private static final int CAMERA_VIDEO_HEIGHT  = 600;
    private static final int RECORD_MAX_DURATION  = 60 * 1000;

    private RecordBinder     mBinder              = null;
    private MediaRecorder    mRecorder            = null;
    private boolean          mRecording           = false;
    private MediaSaver       mMediaSaver          = null;
    private String           mCurVideoFile        = null;
    private GSensorMonitor   mGSensorMon          = null;
    private LocationMonitor  mLocationMon         = null;
    private SdcardManager    mSdManager           = null;
    private MiscEventMonitor mMiscEventMon        = null;
    private FloatWindow      mFloatWin            = null;
    private Handler          mHandler             = new Handler();
    private CameraActivity   mActivity            = null;
    private long             mRecordingStartTime  = Long.MAX_VALUE;
    private long             mImpactStartTime     = Long.MAX_VALUE;
    private boolean          mImpactEventFlag     = false;
    private boolean          mRecMicMuted         = false;
    private int              mCamSwitchState      = 0;

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        Settings.init(this);
        mCamSwitchState = Settings.get(Settings.KEY_CAMERA_SWITCH_STATE_VALUE, Settings.DEF_CAMERA_SWITCH_STATE_VALUE);

        mBinder     = new RecordBinder();
        mMediaSaver = new MediaSaver(this);
        mRecorder   = MediaRecorder.getInstance();
        mRecorder.init();

        // gsensor monitor
        mGSensorMon = new GSensorMonitor(this, mGSensorImpactListener);
        // start gsensor monitor
        mGSensorMon.start();

        // location monitor
        mLocationMon = new LocationMonitor(this, new LocationMonitor.Listener() {
            @Override
            public void showGpsOnScreenIndicator(boolean hasSignal) {
            }
            @Override
            public void hideGpsOnScreenIndicator() {
            }
            @Override
            public void onGpsSpeedChanged(float speed) {
            }
        });
        // start location monitor
        mLocationMon.recordLocation(true);

        // sdcard manager
        mSdManager = new SdcardManager(this, mMediaSaver, new SdcardManager.SdStateChangeListener() {
            @Override
            public void onSdStateChanged(boolean insert) {
                if (insert) {
                    if (Settings.get(Settings.KEY_INSERTSD_AUTO_RECORD, Settings.DEF_INSERTSD_AUTO_RECORD) == 1) {
                        startRecording();
                    }
                }
                else {
                    stopRecording();
                }
                if (mActivity != null) {
                    mActivity.onSdStateChanged(insert);
                }
            }
        });
        // start sd state monitor & disk recycler
        mSdManager.startSdStateMonitor();
        mSdManager.startDiskRecycle();

        // misc event monitor
        mMiscEventMon = new MiscEventMonitor(this, new MiscEventMonitor.MiscEventListener() {
            @Override
            public void onDeviceShutdown() {
                stopRecording();
            }

            @Override
            public void onUsbCamStateChanged(boolean connected) {
                mRecorder.resetCamera(1, -1, -1, -1);
                mCamSwitchState = connected ? 0 : 2;
                if (mActivity != null) {
                    mActivity.onUsbCamStateChanged(connected);
                }
            }
        });
        mMiscEventMon.start();

        // float window
        mFloatWin = new FloatWindow(this);
        mFloatWin.create();
        mFloatWin.showFloat(mRecording);

        // for wake lock
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // for wake lock
        if (mWakeLock != null) {
            mWakeLock.release();
        }

        // hide float window
        mFloatWin.hideFloat();
        mFloatWin.destroy();

        // remove watermark updater
        mHandler.removeCallbacks(mWaterMarkUpdater);

        // stop recording
        stopRecording();

        if (mRecorder != null) {
            mRecorder.stopRecording( 0);
            mRecorder.stopRecording( 1);
            mRecorder.stopRecording( 2);
            mRecorder.stopRecording(-1);
            mRecorder.release();
            mRecorder = null;
        }

        // stop msic event monitor
        mMiscEventMon.stop();

        // stop disk recycle
        mSdManager.stopSdStateMonitor();
        mSdManager.stopDiskRecycle();

        // stop location monitor
        mLocationMon.recordLocation(false);

        // stop gsensor monitor
        mGSensorMon.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public class RecordBinder extends Binder {
        public RecordService getService(CameraActivity activity) {
            mActivity = activity;
            return RecordService.this;
        }
    }

    public boolean startRecording() {
        if (mRecording) return mRecording;
        if (!SdcardManager.isSdcardInsert()) return mRecording;

        // update float window
        mFloatWin.updateFloat(mRecording);

        // acquire wake lock
        mWakeLock.acquire();

        return mRecording;
    }

    public void stopRecording() {
        if (!mRecording) return;
        else mRecording = false;

        // add video to media saver
        mMediaSaver.addVideo(mCurVideoFile, CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT);

        // update float window
        mFloatWin.updateFloat(mRecording);

        // release wake lock
        mWakeLock.release();
    }

    public boolean isRecording() {
        return mRecording;
    }

    public boolean getRecMicMuted() {
        return mRecMicMuted;
    }

    public void setRecMicMuted(boolean mute) {
        mRecMicMuted = mute;
    }

    public int getCamSwitchState() {
        return mCamSwitchState;
    }

    public void switchCamera() {
        if (mMiscEventMon.isUsbCamConnected()) {
            mCamSwitchState += 1;
            mCamSwitchState %= 4;
        }
        else {
            mCamSwitchState = 2;
        }

        if (Settings.get(Settings.KEY_CAMERA_SWITCH_STATE_SAVE, Settings.DEF_CAMERA_SWITCH_STATE_SAVE) == 1) {
            Settings.set(Settings.KEY_CAMERA_SWITCH_STATE_VALUE, mCamSwitchState);
        }
    }

    public int getRecordingMaxDuration() {
        return RECORD_MAX_DURATION;
    }

    public long getRecordingStartTime() {
        return mRecordingStartTime;
    }

    public void takePhoto(int type) {
        switch (type) {
        case 0: // main camera
            break;
        case 1: // usb camera
            break;
        }
    }

    public void setCamMainPreviewTexture(SurfaceTexture st) {
        if (mRecorder != null) {
            mRecorder.setPreviewTexture(0, st);
            if (st != null) {
                mRecorder.startPreview(0);
            }
            else {
                mRecorder.stopPreview(0);
            }
        }
    }

    public void setCamUsbPreviewTexture(SurfaceTexture st) {
        if (mRecorder != null) {
            mRecorder.setPreviewTexture(1, st);
            if (st != null) {
                mRecorder.startPreview(1);
            }
            else {
                mRecorder.stopPreview(1);
            }
        }
    }

    public boolean getImpactEventFlag() { return mImpactEventFlag; }

    public static String getNewRecordFileName(int type) {
        SdcardManager.makeDvrDirs(); // make dvr dirs

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("'VID'_yyyyMMdd_HHmmss");
        return SdcardManager.DIRECTORY_VIDEO + "/" + (type == 0 ? "A_" : "B_") + df.format(date) + ".mp4";
    }

    public static String getNewPhotoFileName(int type) {
        SdcardManager.makeDvrDirs(); // make dvr dirs

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return SdcardManager.DIRECTORY_PHOTO + "/" + (type == 0 ? "A_" : "B_") + df.format(date) + ".jpg";
    }

    public void onResume() {
        mFloatWin.hideFloat();
    }

    public void onPause() {
        mFloatWin.showFloat(mRecording);
    }

    public MediaSaver getMediaSaver() {
        return mMediaSaver;
    }

    private Runnable mWaterMarkUpdater = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(this, 1000);
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss|- - -");
        }
    };

    GSensorMonitor.ImpactEventListener mGSensorImpactListener = new GSensorMonitor.ImpactEventListener() {
        @Override
        public void onGsensorImpactEvent(boolean flag) {
            mImpactEventFlag = true;
            mImpactStartTime = SystemClock.uptimeMillis();
            mMediaSaver.onGsensorImpactEvent(mImpactEventFlag);
            mActivity  .onGsensorImpactEvent(mImpactEventFlag);
        }
    };
}


