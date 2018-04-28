package com.apical.dvr;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.view.SurfaceView;
import android.util.Log;
import java.io.*;

public class ffRecorder {
    private static final String TAG = "ffRecorder";
    private SurfaceTexture     mSurTextNull= null;
    private SurfaceView        mSurViewNull= null;
    private Camera          [] mCameraDevs = new Camera          [2];
    private boolean         [] mWaterMarkEn= new boolean         [2];
    private MediaRecorder   [] mRecorders  = new MediaRecorder   [3];
    private CamcorderProfile[] mProfiles   = new CamcorderProfile[3];
    private Camera          [] mRecCamIdx  = new Camera          [3];
    private boolean         [] mRecordEn   = new boolean         [3];
    private boolean            mMicMute    = false;
    private Context            mContext    = null;

    private static ffRecorder mSingleInstance = null;
    public static ffRecorder getInstance(Context context) {
        if (mSingleInstance == null) {
            mSingleInstance = new ffRecorder();
            mSingleInstance.mContext = context;
            mSingleInstance.mSurTextNull = new SurfaceTexture(0);
            mSingleInstance.mSurViewNull = new SurfaceView(context);
        }
        return mSingleInstance;
    }

    public void init(int cam_main_w, int cam_main_h, int cam_usb_w, int cam_usb_h) {
        try {
            mCameraDevs[0] = Camera.open(0);
            if (mCameraDevs[0] != null) {
                Camera.Parameters params = mCameraDevs[0].getParameters();
                params.setPreviewSize(cam_main_w, cam_main_h);
                params.setPictureSize(cam_main_w, cam_main_h);
                mCameraDevs[0].setParameters(params);
                mCameraDevs[0].startPreview();
            }
        } catch (Exception e) {}

        try {
            mCameraDevs[1] = Camera.open(1);
            if (mCameraDevs[1] != null) {
                Camera.Parameters params = mCameraDevs[1].getParameters();
                params.setPreviewSize(cam_usb_w, cam_usb_h);
                params.setPictureSize(cam_usb_w, cam_usb_w);
                mCameraDevs[1].setParameters(params);
                mCameraDevs[1].startPreview();
            }
        } catch (Exception e) {}

        mProfiles[0] = CamcorderProfile.get(0, CamcorderProfile.QUALITY_720P);
        mProfiles[1] = CamcorderProfile.get(0, CamcorderProfile.QUALITY_480P);
        mProfiles[2] = CamcorderProfile.get(0, CamcorderProfile.QUALITY_480P);

        for (int i=0; i<mRecorders.length; i++) {
            mRecorders[i] = new MediaRecorder();
        }
    }

    public void release() {
        mSingleInstance = null;

        for (MediaRecorder recorder : mRecorders) {
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
        }

        for (Camera cam : mCameraDevs) {
            if (cam != null) {
                cam.release();
                cam = null;
            }
        }
    }

    public boolean getMicMute(int micidx) {
        return mMicMute;
    }

    public void setMicMute(int micidx, boolean mute) {
        mMicMute = mute;
    }

    public void resetCamera(int camidx, int w, int h, int frate) {
        if (mCameraDevs[camidx] == null) return;
        try {
            Camera.Parameters params = mCameraDevs[camidx].getParameters();
            params.setPreviewSize(w, h);
            params.setPictureSize(w, h);
            mCameraDevs[camidx].stopPreview();
            mCameraDevs[camidx].setParameters(params);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setWatermark(int camidx, int x, int y, String watermark) {
        if (mCameraDevs[camidx] == null) return;
        mWaterMarkEn[camidx] = watermark != null && !watermark.equals("");
    }

    public void setPreviewDisplay(int camidx, SurfaceView win) {
        if (mCameraDevs[camidx] == null) return;
        if (win == null) win = mSurViewNull;
        try { mCameraDevs[camidx].setPreviewDisplay(win.getHolder()); } catch (Exception e) { e.printStackTrace(); }
    }

    public void setPreviewTexture(int camidx, SurfaceTexture win) {
        if (mCameraDevs[camidx] == null) return;
        if (win == null) win = mSurTextNull;
        try { mCameraDevs[camidx].setPreviewTexture(win); } catch (Exception e) { e.printStackTrace(); }
    }

    public void startPreview(int camidx) {
        // do nothing
    }

    public void stopPreview(int camidx) {
        // do nothing
    }

    public void startRecording(int encidx, String filename) {
        if (encidx == -1) return;
        try {
            if (mRecordEn [encidx] == false) {
                mRecCamIdx[encidx].unlock();
                mRecorders[encidx].setCamera(mRecCamIdx[encidx]);
                mRecorders[encidx].setAudioSource(mMicMute ? MediaRecorder.AudioSource.REMOTE_SUBMIX : MediaRecorder.AudioSource.MIC);
                mRecorders[encidx].setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorders[encidx].setProfile(mProfiles[encidx]);
                mRecorders[encidx].setOutputFile(filename);
                mRecorders[encidx].prepare();
                mRecorders[encidx].start();
                mRecordEn [encidx] = true;
            } else {
                if (true) {
                    mRecorders[encidx].stop();
                    mRecCamIdx[encidx].lock();
                    mRecCamIdx[encidx].unlock();
                    mRecorders[encidx].setCamera(mRecCamIdx[encidx]);
                    mRecorders[encidx].setAudioSource(mMicMute ? MediaRecorder.AudioSource.REMOTE_SUBMIX : MediaRecorder.AudioSource.MIC);
                    mRecorders[encidx].setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    mRecorders[encidx].setProfile(mProfiles[encidx]);
                    mRecorders[encidx].setOutputFile(filename);
                    mRecorders[encidx].prepare();
                    mRecorders[encidx].start();
                } else {
                    mRecorders[encidx].setOutputFile(filename);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopRecording(int encidx) {
        if (encidx == -1) return;
        try {
            if (mRecordEn [encidx] == true) {
                mRecorders[encidx].stop();
                mRecCamIdx[encidx].lock();
                mRecordEn [encidx] = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setAudioSource(int encidx, int source) {
        // do nothing
    }

    public void setVideoSource(int encidx, int source) {
        mRecCamIdx[encidx] = mCameraDevs[source];
    }

    public void takePhoto(int camidx, String filename, takePhotoCallback callback) {
        final int               idx = camidx;
        final String            ffn = filename;
        final takePhotoCallback fcb = callback;
        mCameraDevs[camidx].takePicture(
            new android.hardware.Camera.ShutterCallback() {
                @Override
                public void onShutter() {}
            },
            null,
            new android.hardware.Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte [] jpegData, Camera camera) {
                    if (writeFile(ffn, jpegData) && fcb != null) {
                        Camera.Parameters params = mCameraDevs[idx].getParameters();
                        android.hardware.Camera.Size size = params.getPreviewSize();
                        fcb.onPhotoTaken(ffn, size.width, size.height);
                    }
                }
            }
        );
    }

    public interface takePhotoCallback {
        public void onPhotoTaken(String filename, int w, int h);
    }

    private static boolean writeFile(String path, byte[] data) {
        boolean ret = false;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            ret = true;
        } catch (Exception e) {
            Log.e(TAG, "failed to write file data !", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}



