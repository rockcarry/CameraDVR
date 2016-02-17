package com.apical.cdr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.android.camera.exif.Exif;
import com.android.camera.exif.ExifInterface;

public class RecordService extends Service implements
    MediaRecorder.OnErrorListener,
    MediaRecorder.OnInfoListener
{
    private static final String TAG = "RecordService";
    private static final int CAMERA_VIDEO_WIDTH  = 800;
    private static final int CAMERA_VIDEO_HEIGHT = 600;
    private static final int CAMERA_VIDEO_FRATE  = 30;
    private static final int CAMERA_AUDIO_CHNUM  = 1;
    private static final int CAMERA_AUDIO_SRATE  = 8000;
    private static final int RECORD_VIDEO_BITRATE= 3000000;
    private static final int RECORD_AUDIO_BITRATE= 12200;
    private static final int RECORD_MAX_DURATION = 60 * 1000;

    private RecordBinder    mBinder      = null;
    private Camera          mCamDev      = null;
    private MediaRecorder   mRecorder    = null;
    private boolean         mRecording   = false;
    private MediaSaver      mMediaSaver  = null;
    private SurfaceHolder   mHolder      = null;
    private SurfaceView     mSurViewNull = null;
    private String          mCurVideoFile= null;
    private LocationManager mLocManager  = null;
    private SdcardManager   mSdManager   = null;
    private Handler         mHandler     = new Handler();
    private boolean         mTakePhotoInProgress = false;
    private long            mRecordingStartTime  = Long.MAX_VALUE;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        mBinder      = new RecordBinder ();
        mRecorder    = new MediaRecorder();
        mMediaSaver  = new MediaSaver (this);
        mSurViewNull = new SurfaceView(this);

        // location manager
        mLocManager = new LocationManager(this, new LocationManager.Listener() {
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
        mLocManager.recordLocation(true);

        // sdcard manager
        mSdManager = new SdcardManager(this, mMediaSaver, new SdcardManager.SDStateChangeListener() {
            @Override
            public void onSDStateChanged(boolean insert) {
            }
        });
        // start sd state monitor & disk recycler
        mSdManager.startSdStateMonitor();
        mSdManager.startDiskRecycle();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // remove watermark updater
        mHandler.removeCallbacks(mWaterMarkUpdater);

        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }

        if (mCamDev != null) {
            mCamDev.stopPreview();
            mCamDev.release();
            mCamDev = null;
        }

        // stop disk recycle
        mSdManager.stopSdStateMonitor();
        mSdManager.stopDiskRecycle();

        // stop location monitor
        mLocManager.recordLocation(false);
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
        public RecordService getService() {
            return RecordService.this;
        }
    }

    public boolean startRecording() {
        if (mRecording) return mRecording;

        try {
            mCamDev.unlock();
            mRecorder.setCamera(mCamDev);
            mRecorder.setPreviewDisplay(null);

            mRecorder.setAudioSource (MediaRecorder.AudioSource.CAMCORDER); 
            mRecorder.setVideoSource (MediaRecorder.VideoSource.CAMERA);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mRecorder.setVideoSize(CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT);
            mRecorder.setVideoFrameRate(CAMERA_VIDEO_FRATE);
            mRecorder.setVideoEncodingBitRate(RECORD_VIDEO_BITRATE);

            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioChannels(CAMERA_AUDIO_CHNUM);
            mRecorder.setAudioSamplingRate(CAMERA_AUDIO_SRATE);
            mRecorder.setAudioEncodingBitRate(RECORD_AUDIO_BITRATE);

            mCurVideoFile = getNewRecordFileName();
            mRecorder.setMaxDuration(RECORD_MAX_DURATION);
            mRecorder.setOutputFile(mCurVideoFile);
            mRecorder.setOnErrorListener(RecordService.this);
            mRecorder.setOnInfoListener (RecordService.this);
            mRecorder.prepare();
            mRecorder.start();
            mRecording = true;
            mRecordingStartTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mRecording;
    }

    public void stopRecording() {
        if (!mRecording) return;

        // stop & release recorder, then lock camdev
        mRecorder.setOnErrorListener(null);
        mRecorder.setOnInfoListener (null);
        mRecorder.stop();
        mRecorder.release();
        mCamDev  .lock();

        // add video to media saver
        mMediaSaver.addVideo(mCurVideoFile, CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT);

        // re-create a new recorder for next recordnig
        mRecorder  = new MediaRecorder();
        mRecording = false;
    }

    public boolean isRecording() {
        return mRecording;
    }

    public int getRecordingMaxDuration() {
        return RECORD_MAX_DURATION;
    }

    public long getRecordingStartTime() {
        return mRecordingStartTime;
    }

    public void takePhoto(Camera.ShutterCallback sc) {
        if (mTakePhotoInProgress) return;

        mTakePhotoInProgress = true;
        mCamDev.takePicture(sc, null,
            new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera cam) {
                    Log.d(TAG, "takePhoto onPictureTaken");
                    Location      loc  = mLocManager.getCurrentLocation();
                    ExifInterface exif = Exif.getExif(data);
                    int    orientation = Exif.getOrientation(exif);

                    mMediaSaver.addImage(data,
                        getNewPhotoFileName(), System.currentTimeMillis(),
                        loc, 0, 0, orientation, exif);

                    mTakePhotoInProgress = false;
                }
            });
    }

    public void setPreviewSurfaceHolder(SurfaceHolder holder) {
        if (holder == null) {
            holder = mSurViewNull.getHolder();
        }
        else {
            mHolder = holder;
        }
        try {
            mCamDev.stopPreview();
            mCamDev.setPreviewDisplay(holder);
            mCamDev.startPreview();

            //++ enable watermark
            SystemProperties.set("sys.watermark.pos", "22-22");
            mHandler.postDelayed(mWaterMarkUpdater, 50);
            //-- enable watermark
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectCamera(int n) {
        if (mCamDev != null) {
            mCamDev.stopPreview();
            mCamDev.release();
        }

        switch (n) {
        case 0:
            mCamDev = Camera.open(0);
            break;
        case 1:
            mCamDev = Camera.open(1);
            break;
        }

        Camera.Parameters params = mCamDev.getParameters();
        params .setPreviewSize(CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT);
        mCamDev.setParameters(params);
        setPreviewSurfaceHolder(mHolder);
    }

    public static String getNewRecordFileName() {
        File dir_dcim  = new File(SdcardManager.DIRECTORY_DCIM );
        File dir_video = new File(SdcardManager.DIRECTORY_VIDEO);
        if (!dir_dcim .exists()) dir_dcim .mkdirs();
        if (!dir_video.exists()) dir_video.mkdirs();

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("'VID'_yyyyMMdd_HHmmss");
        return SdcardManager.DIRECTORY_VIDEO + "/" + df.format(date) + ".mp4";
    }

    public static String getNewPhotoFileName() {
        File dir_dcim  = new File(SdcardManager.DIRECTORY_DCIM );
        File dir_photo = new File(SdcardManager.DIRECTORY_PHOTO);
        if (!dir_dcim .exists()) dir_dcim .mkdirs();
        if (!dir_photo.exists()) dir_photo.mkdirs();

        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return SdcardManager.DIRECTORY_PHOTO + "/" + df.format(date) + ".jpg";
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d(TAG, "onInfo what = " + what + ", extra = " + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            mRecordingStartTime = Long.MAX_VALUE;
            stopRecording ();
            startRecording();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d(TAG, "onError what = " + what + ", extra = " + extra);
    }

    private Runnable mWaterMarkUpdater = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(this, 1000);
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss|- - -");
            SystemProperties.set("sys.watermark.msg", df.format(date));
        }
    };
}


