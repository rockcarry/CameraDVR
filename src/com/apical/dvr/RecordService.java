package com.apical.dvr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordService extends Service
{
    private static final String TAG = "RecordService";
    private static final int MSG_UPDATE_WATERMARK = 1;
    private static final int MSG_SWITCH_NEXT_FILE = 2;

    private RecordBinder     mBinder              = null;
    private ffRecorder       mRecorder            = null;
    private boolean          mRecording           = false;
    private MediaSaver       mMediaSaver          = null;
    private String           mCurVideoFile        = null;
    private GSensorMonitor   mGSensorMon          = null;
    private LocationMonitor  mLocationMon         = null;
    private SdcardManager    mSdManager           = null;
    private MiscEventMonitor mMiscEventMon        = null;
    private FloatWindow      mFloatWin            = null;
    private MainActivity     mActivity            = null;
    private int              mRecordDuration      = 0;
    private long             mRecordStartTimeA    = 0;
    private long             mRecordStartTimeB    = 0;
    private String           mRecordFileNameA     = "";
    private String           mRecordFileNameB     = "";
    private boolean          mWatermarkEnable     = false;
    private long             mImpactTimeStamp     = 0;
    private boolean          mImpactSaveFlag      = false;
    private int              mCamSwitchState      = 0;
    private MediaPlayer      mShutterMP           = null;

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        Settings.init(this);
        mCamSwitchState = Settings.get(Settings.KEY_CAMERA_SWITCH_STATE_VALUE, Settings.DEF_CAMERA_SWITCH_STATE_VALUE);
        mRecordDuration = Settings.get(Settings.KEY_RECORD_DURATION          , Settings.DEF_RECORD_DURATION          );

        mBinder     = new RecordBinder();
        mMediaSaver = new MediaSaver(this);
        mRecorder   = ffRecorder.getInstance(this);

        int quality    = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);
        int cam_main_w = quality == 0 ? 1280 : 1920;
        int cam_main_h = quality == 0 ? 720  : 1080;
        mRecorder.init(cam_main_w, cam_main_h, 0, 0);

        // encoder0 (1080p encoder), audio source is mic, video source is main camera
        mRecorder.setAudioSource(0, 0);
        mRecorder.setVideoSource(0, 0);

        // encoder1 (720p encoder ), audio source is mic, video source is main camera
        mRecorder.setAudioSource(1, 0);
        mRecorder.setVideoSource(1, 0);

        // encoder2 (vga  encoder ), audio source is mic, video source is usb camera
        mRecorder.setAudioSource(2, 0);
        mRecorder.setVideoSource(2, 1);

        if (1 == Settings.get(Settings.KEY_RECORD_MIC_MUTE, Settings.DEF_RECORD_MIC_MUTE)) {
            mRecorder.setMicMute(0, true);
        }

        // gsensor monitor
        mGSensorMon = new GSensorMonitor(this, new GSensorMonitor.ImpactEventListener() {
            @Override
            public void onGsensorImpactEvent(long time) {
                mImpactTimeStamp = time; // update impact time
                mImpactSaveFlag  = true; // update impact save flag
                mActivity.onGsensorImpactEvent(time); // call mActivity's onGsensorImpactEvent
            }
        });
        // start gsensor monitor
        mGSensorMon.start();
        mGSensorMon.setImpactDetectLevel(Settings.get(Settings.KEY_IMPACT_DETECT_LEVEL, Settings.DEF_IMPACT_DETECT_LEVEL));

        // location monitor
        mLocationMon = new LocationMonitor(this, null);
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
                } else {
                    stopRecording();
                }
                if (mActivity != null) {
                    mActivity.onSdStateChanged(insert);
                }
            }
        });
        // start sd state monitor
        mSdManager.start();

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
                if (mRecording) {
                    if (connected && SystemClock.uptimeMillis() < mRecordStartTimeA + 55000) {
                        mRecordStartTimeB= SystemClock.uptimeMillis();
                        mRecordFileNameB = getNewRecordFileName(1);
                        mRecorder.startRecording( 2, mRecordFileNameB);
                        mRecorder.startRecording(-1, null);
                    } else {
                        mRecorder.stopRecording( 2);
                        mRecorder.stopRecording(-1);
                    }
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

        // for take photo shutter
        mShutterMP = MediaPlayer.create(this, R.raw.shutter);

        // for watermark
        setWatermarkEnable(Settings.get(Settings.KEY_WATERMARK_ENABLE, Settings.DEF_WATERMARK_ENABLE) == 0 ? false : true);
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

        // remove messages
        mHandler.removeMessages(0);

        // stop recording
        stopRecording();

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        // stop msic event monitor
        mMiscEventMon.stop();

        // stop sd state monitor
        mSdManager.stop();

        // stop location monitor
        mLocationMon.recordLocation(false);

        // stop gsensor monitor
        mGSensorMon.stop();

        // for take photo shutter
        mShutterMP.release();
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
        public RecordService getService(MainActivity activity) {
            mActivity = activity;
            return RecordService.this;
        }
    }

    public boolean startRecording() {
        if (mRecording) return mRecording;
        if (!SdcardManager.isSdcardInsert()) return mRecording;

        // update mRecording flag
        mRecording = true;

        // clear impact time stamp or not
        if (false) {
            mImpactTimeStamp = 0;
        }

        // update impact save flag
//      mImpactSaveFlag = mImpactTimeStamp + Settings.DEF_IMPACT_DURATION > SystemClock.uptimeMillis();
        mImpactSaveFlag = false;

        // update mRecordingStartTime
        mRecordStartTimeA = SystemClock.uptimeMillis();
        mRecordStartTimeB = SystemClock.uptimeMillis();
        mHandler.sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE, mRecordDuration);

        // start recording
        if (true) {
            int quality = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);
            int encidx  = quality == 0 ? 1 : 0;
            mRecordFileNameA = getNewRecordFileName(0);
            mRecorder.startRecording(encidx, mRecordFileNameA);
        }
        if (mMiscEventMon.isUsbCamConnected()) {
            mRecordFileNameB = getNewRecordFileName(1);
            mRecorder.startRecording(2, mRecordFileNameB);
        }
        if (true) {
            mRecorder.startRecording(-1, null);
        }

        // update float window
        mFloatWin.updateFloat(mRecording);

        // play shutter sound
        mShutterMP.seekTo(0);
        mShutterMP.start();

        // acquire wake lock
        mWakeLock.acquire();

        return mRecording;
    }

    public void stopRecording() {
        if (!mRecording) return;
        else mRecording = false;

        mHandler.removeMessages(MSG_SWITCH_NEXT_FILE);

        mRecorder.stopRecording( 0);
        mRecorder.stopRecording( 1);
        mRecorder.stopRecording(-1);

        long stoptime = SystemClock.uptimeMillis();
        if (true) {
            int quality = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);
            int w = quality == 0 ? 1280 : 1920;
            int h = quality == 0 ? 720  : 1080;
            mMediaSaver.addVideo(mRecordFileNameA, System.currentTimeMillis(), w, h, stoptime - mRecordStartTimeA, mImpactSaveFlag);
        }
        if (mMiscEventMon.isUsbCamConnected()) {
            mMediaSaver.addVideo(mRecordFileNameB, System.currentTimeMillis(), 640, 480, stoptime - mRecordStartTimeB, mImpactSaveFlag);
        }

        // update float window
        mFloatWin.updateFloat(mRecording);

        // release wake lock
        mWakeLock.release();
    }

    public boolean isRecording() {
        return mRecording;
    }

    public boolean getRecMicMute() {
        boolean mute = mRecorder.getMicMute(0);
        return mRecorder.getMicMute(0);
    }

    public void setRecMicMute(boolean mute) {
        Settings.set(Settings.KEY_RECORD_MIC_MUTE, mute ? 1 : 0);
        mRecorder.setMicMute(0, mute);
    }

    public int getCamSwitchState() {
        return mMiscEventMon.isUsbCamConnected() ? mCamSwitchState : 2;
    }

    public void switchCamera() {
        if (mMiscEventMon.isUsbCamConnected()) {
            mCamSwitchState += 1;
            mCamSwitchState %= 4;
        } else {
            mCamSwitchState = 2;
        }

        if (Settings.get(Settings.KEY_CAMERA_SWITCH_STATE_SAVE, Settings.DEF_CAMERA_SWITCH_STATE_SAVE) == 1) {
            Settings.set(Settings.KEY_CAMERA_SWITCH_STATE_VALUE, mCamSwitchState);
        }
    }

    public void setWatermarkEnable(boolean en) {
        if (en) {
            mHandler.sendEmptyMessage(MSG_UPDATE_WATERMARK);
        } else {
            mHandler.removeMessages(MSG_UPDATE_WATERMARK);
            mRecorder.setWatermark(0, 32, 32, "");
        }
        mWatermarkEnable = en;
    }

    public void setCamMainVideoQuality(int quality) {
        mRecorder.resetCamera(0, quality == 0 ? 1280 : 1920, quality == 0 ? 720 : 1080, -1);
    }

    public long getImpactTime() { return mImpactTimeStamp; }
    public void setImpactDetectLevel(int l) { if (mGSensorMon != null) mGSensorMon.setImpactDetectLevel(l); }

    public int getRecordingMaxDuration() {
        return mRecordDuration;
    }

    public void setRecordingMaxDuration(int duration) {
        mRecordDuration = duration;
        Settings.set(Settings.KEY_RECORD_DURATION, duration);
    }

    public long getRecordingStartTime() {
        return mRecordStartTimeA;
    }

    public void takePhoto(int type) {
        mRecorder.takePhoto(type, getNewPhotoFileName(type), new ffRecorder.takePhotoCallback() {
            @Override
            public void onPhotoTaken(String filename, int width, int height) {
                mMediaSaver.addImage(filename,
                    System.currentTimeMillis(),
                    mLocationMon.getCurrentLocation(),
                    width, height, 0);
            }
        });

        // play shutter sound
        mShutterMP.seekTo(0);
        mShutterMP.start();
    }

    public void setCamMainPreviewTexture(SurfaceTexture st) {
        if (mRecorder != null) {
            mRecorder.setPreviewTexture(0, st);
            if (st != null) {
                mRecorder.startPreview(0);
            } else {
                mRecorder.stopPreview(0);
            }
        }
    }

    public void setCamUsbPreviewTexture(SurfaceTexture st) {
        if (mRecorder != null) {
            mRecorder.setPreviewTexture(1, st);
            if (st != null) {
                mRecorder.startPreview(1);
            } else {
                mRecorder.stopPreview(1);
            }
        }
    }

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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final long last_start_a;
            final long last_start_b;
            switch (msg.what) {
            case MSG_UPDATE_WATERMARK:
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_WATERMARK, 1000);
                Date           date = new Date(System.currentTimeMillis());
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss\n");
                float         speed = mLocationMon.getCurrentSpeed();
                String    watermark = df.format(date) + (speed < 0 ? " - - - " : (" " + (int)(speed * 3.6) + " KM/H"));
                if (mWatermarkEnable && mRecorder != null) {
                    mRecorder.setWatermark(0, 88, 68, watermark);
                }
                break;

            case MSG_SWITCH_NEXT_FILE:
                // update mRecordStartTime
                last_start_a = mRecordStartTimeA;
                last_start_b = mRecordStartTimeB;
                mRecordStartTimeA = SystemClock.uptimeMillis();
                mRecordStartTimeB = SystemClock.uptimeMillis();
                mHandler.sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE, mRecordDuration);

                //++ switch to next record file
                new Thread() {
                    @Override
                    public void run() {
                        String newNameA = getNewRecordFileName(0);
                        String newNameB = getNewRecordFileName(1);
                        int    quality  = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);

                        if (true) {
                            mRecorder.startRecording(quality == 0 ? 1 : 0, newNameA);
                        }
                        if (mMiscEventMon.isUsbCamConnected()) {
                            mRecorder.startRecording(2, newNameB);
                        }
                        if (true) {
                            mRecorder.startRecording(-1, null);
                        }

                        long stoptime = SystemClock.uptimeMillis();
                        if (true) {
                            int w = quality == 0 ? 1280 : 1920;
                            int h = quality == 0 ? 720  : 1080;
                            mMediaSaver.addVideo(mRecordFileNameA, System.currentTimeMillis(), w, h, stoptime - last_start_a, mImpactSaveFlag);
                        }
                        if (mMiscEventMon.isUsbCamConnected()) {
                            mMediaSaver.addVideo(mRecordFileNameB, System.currentTimeMillis(), 640, 480, stoptime - last_start_b, mImpactSaveFlag);
                        }

                        mRecordFileNameA = newNameA;
                        mRecordFileNameB = newNameB;

                        // update impact save flag
                        mImpactSaveFlag = mImpactTimeStamp + Settings.DEF_IMPACT_DURATION > SystemClock.uptimeMillis();
                    }
                }.start();
                //-- switch to next record file
                break;
            }
        }
    };
}
