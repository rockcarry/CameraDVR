package com.apical.cdr;

import android.hardware.Camera;
import android.view.SurfaceHolder;

public class UsbCam {
    private static int      MAX_USBCAM_DEV_NUM = 8;
    private static UsbCam[] sUsbCamInstances   = new UsbCam[MAX_USBCAM_DEV_NUM];

    private int  m_nUsbCamDevID ;
    private long m_hUsbCamNative;

    public static UsbCam open(int id) {
        if (id > MAX_USBCAM_DEV_NUM || sUsbCamInstances[id] != null) {
            return null;
        }
        sUsbCamInstances[id] = new UsbCam();
        sUsbCamInstances[id].m_nUsbCamDevID  = id;
        sUsbCamInstances[id].m_hUsbCamNative = sUsbCamInstances[id].nativeInit("/dev/video" + id);
        return sUsbCamInstances[id];
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (holder.getSurfaceFrame().right != 0 && holder.getSurfaceFrame().bottom != 0) {
            nativeSetPreviewSurface(m_hUsbCamNative,
                holder.getSurface(),
                holder.getSurfaceFrame().right,
                holder.getSurfaceFrame().bottom);
        }
    }

    public void startPreview() {
        nativeStartPreview(m_hUsbCamNative);
    }

    public void stopPreview() {
        nativeStopPreview(m_hUsbCamNative);
    }

    public void takePicture(Camera.ShutterCallback sc, Camera.PictureCallback rawpc, Camera.PictureCallback jpgpc) {
        // todo...
    }

    public void release() {
        nativeClose(m_hUsbCamNative);
        sUsbCamInstances[m_nUsbCamDevID] = null;
    }

    private static native long nativeInit (String dev);
    private static native void nativeClose(long husbcam);
    private static native void nativeSetPreviewSurface(long husbcam, Object surface, int w, int h);
    private static native void nativeStartPreview(long husbcam);
    private static native void nativeStopPreview (long husbcam);

    static {
        System.loadLibrary("usbcam_jni");
    }
};



