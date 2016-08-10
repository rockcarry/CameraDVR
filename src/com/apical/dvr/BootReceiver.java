package com.apical.dvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraDVRBootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            //+ start record service
            if (false) {
                Intent recordservice = new Intent(context, com.apical.dvr.RecordService.class);
                context.startService(recordservice);
            }
            //- start record service
        } catch (Exception e) {
            Log.e(TAG, "Can't start load record service", e);
        }
    }
}

