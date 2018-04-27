package com.apical.dvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraDVRBootReceiver";
    private static final boolean ENABLE_RECORD_SERVICE  = false;
    private static final boolean ENABLE_DISK_RECYCLER   = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            // for record service
            if (ENABLE_RECORD_SERVICE) {
                Intent recordservice = new Intent(context, com.apical.dvr.RecordService.class);
                context.startService(recordservice);
            }

            // for disk recycler
            if (ENABLE_DISK_RECYCLER) {
                Intent service = new Intent(context, com.apical.dvr.DiskRecycler.class);
                context.startService(service);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't start load record service", e);
        }
    }
}

