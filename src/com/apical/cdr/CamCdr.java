package com.apical.cdr;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.SurfaceHolder;

public class CamCdr {
    private static int      MAX_CAMCDR_DEV_NUM = 8;
    private static CamCdr[] sCamCdrListInst    = new CamCdr[MAX_CAMCDR_DEV_NUM];
    private static String[] sCamCdrListDev     = new String[MAX_CAMCDR_DEV_NUM];

    private int  m_nCamCdrDevID ;
    private long m_hCamCdrNative;

    public static int CAMCDR_PIXFMT_AUTO  = 0x0;
    public static int CAMCDR_PIXFMT_YUYV  = 0x56595559;
    public static int CAMCDR_PIXFMT_MJPEG = 0x47504a4d;

    public static CamCdr open(String dev, int sub, int fmt, int w, int h) {
        boolean devused = false;
        int     freeidx = 0;
        for (int i=0; i<sCamCdrListDev.length; i++) {
            if (sCamCdrListDev[i] != null && sCamCdrListDev[i].equals(dev)) {
                devused = true;
            }
            if (sCamCdrListDev[i] == null) {
                freeidx = i;
            }
        }
        if (devused) {
            return null;
        }

        sCamCdrListDev [freeidx] = new String(dev);
        sCamCdrListInst[freeidx] = new CamCdr();
        sCamCdrListInst[freeidx].m_nCamCdrDevID  = freeidx;
        sCamCdrListInst[freeidx].m_hCamCdrNative = sCamCdrListInst[freeidx].nativeInit(dev, sub, fmt, w, h);
        return sCamCdrListInst[freeidx];
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (holder != null && holder.getSurfaceFrame().right != 0 && holder.getSurfaceFrame().bottom != 0) {
            nativeSetPreviewSurface(m_hCamCdrNative, holder.getSurface());
        }
        else {
            nativeSetPreviewSurface(m_hCamCdrNative, null);
        }
    }

    public void setPreviewTexture(SurfaceTexture texture) {
        nativeSetPreviewTexture(m_hCamCdrNative, texture);
    }

    public void startPreview() {
        nativeStartPreview(m_hCamCdrNative);
    }

    public void stopPreview() {
        nativeStopPreview(m_hCamCdrNative);
    }

    public void takePicture(Camera.ShutterCallback sc, Camera.PictureCallback rawpc, Camera.PictureCallback jpgpc) {
        // todo...
    }

    public void release() {
        nativeClose(m_hCamCdrNative);
        sCamCdrListDev [m_nCamCdrDevID] = null;
        sCamCdrListInst[m_nCamCdrDevID] = null;
    }

    private static native long nativeInit (String dev, int sub, int fmt, int w, int h);
    private static native void nativeClose(long hCamCdr);
    private static native void nativeSetPreviewSurface(long hCamCdr, Object surface);
    private static native void nativeSetPreviewTexture(long hCamCdr, Object surface);
    private static native void nativeStartPreview(long hCamCdr);
    private static native void nativeStopPreview (long hCamCdr);

    static {
        System.loadLibrary("camcdr_jni");
    }
};



