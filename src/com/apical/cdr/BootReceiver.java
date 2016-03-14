package com.apical.cdr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraCdrBootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            //+ start record service
            if (true) {
                Intent recordservice = new Intent(context, com.apical.cdr.RecordService.class);
                context.startService(recordservice);
            }
            //- start record service
        } catch (Exception e) {
            Log.e(TAG, "Can't start load record service", e);
        }
    }
}

