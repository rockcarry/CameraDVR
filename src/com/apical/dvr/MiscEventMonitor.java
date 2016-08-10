package com.apical.dvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class MiscEventMonitor {
    private final static String TAG = "MiscEventMonitor";

    private Context           mContext       = null;
    private MiscEventListener mListerner     = null;
    private boolean           mUsbCamConnect = false;

    public interface MiscEventListener {
        public void onDeviceShutdown();
        public void onUsbCamStateChanged(boolean connected);
    };

    public MiscEventMonitor(Context c, MiscEventListener l) {
        mContext   = c;
        mListerner = l;
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction("android.hardware.usb.action.USB_CAMERA_PLUG_IN_OUT");
        mContext.registerReceiver(mReceiver, filter);
    }

    public void stop() {
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean isUsbCamConnected() {
        return mUsbCamConnect;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SHUTDOWN)) {
                if (mListerner != null) {
                    mListerner.onDeviceShutdown();
                }
            }
            else if (action.equals("android.hardware.usb.action.USB_CAMERA_PLUG_IN_OUT")) {
                Bundle bundle  = intent.getExtras();
                mUsbCamConnect = (bundle != null && bundle.getInt("UsbCameraState") == 1);
                if (mListerner != null) {
                    mListerner.onUsbCamStateChanged(mUsbCamConnect);
                }
            }
        }
    };
}




