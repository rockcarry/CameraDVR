package com.apical.cdr;

import android.hardware.Camera;
import android.view.Surface;

public class UsbCam {
    public boolean open(int dev) { return false; }
    public void setPreviewDisplay(Surface surface) {}
    public void startPreview() {}
    public void stopPreview() {}
    public void takePicture(Camera.ShutterCallback sc, Camera.PictureCallback pc) {}
    public void release() {}
};



