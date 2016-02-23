package com.apical.cdr;

import android.hardware.Camera;
import android.view.SurfaceHolder;

public class UsbCam {
    public static UsbCam open(int dev) { return null; }
    public void setPreviewDisplay(SurfaceHolder holder) {}
    public void startPreview() {}
    public void stopPreview() {}
    public void takePicture(Camera.ShutterCallback sc, Camera.PictureCallback rawpc, Camera.PictureCallback jpgpc) {}
    public void release() {}
};



