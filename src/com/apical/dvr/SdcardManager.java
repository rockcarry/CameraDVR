package com.apical.dvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.android.internal.os.storage.ExternalStorageFormatter;

public class SdcardManager {
    private final static String TAG = "SdcardManager";

    // dvr pathes
    public static final String DVR_SD_ROOT      = "/mnt/sdcard";
    public static final String DIRECTORY_DCIM   = DVR_SD_ROOT    + "/DCIM";
    public static final String DIRECTORY_PHOTO  = DIRECTORY_DCIM + "/DVR_Photo";
    public static final String DIRECTORY_VIDEO  = DIRECTORY_DCIM + "/DVR_Video";
    public static final String DIRECTORY_IMPACT = DIRECTORY_DCIM + "/DVR_Impact";

    // disk recycle thread
    private Context           mContext       = null;
    private MediaSaver        mMediaSaver    = null;
    private SdStateChangeListener mListerner = null;

    public SdcardManager(Context c, MediaSaver ms, SdStateChangeListener l) {
        mContext    = c;
        mMediaSaver = ms;
        mListerner  = l;
    }

    public interface SdStateChangeListener {
        public void onSdStateChanged(boolean insert);
    };

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT  );
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        mContext.registerReceiver(mMediaChangeReceiver, filter);
    }

    public void stop() {
        mContext.unregisterReceiver(mMediaChangeReceiver);
    }

    public static boolean isSdcardInsert() {
        /*
        String state = Environment.getStorageState(new File(DVR_SD_ROOT));
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return false;
        }*/
        return true;
    }

    public static void formatSDcard(Context context) {
        StorageManager storageManager = StorageManager.from(context);
        final StorageVolume[] storageVolumes = storageManager.getVolumeList();

        Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
        intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
        intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, storageVolumes[1]);
        context.startService(intent);
    }

    public static int getBlockSize() {
        String state = Environment.getStorageState(new File(DVR_SD_ROOT));

        if (TextUtils.equals(state, Environment.MEDIA_MOUNTED)) {
            try {
                StatFs stat = new StatFs(DVR_SD_ROOT);
                if (state != null) {
                    return stat.getBlockSize();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static long getTotalSpace() {
        String state = Environment.getStorageState(new File(DVR_SD_ROOT));
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return -1;
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return -1;
        }
        try {
            StatFs stat = new StatFs(DIRECTORY_DCIM);
            return stat.getBlockCount() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return -1;
    }

    public static long getAvailableSpace() {
        String state = Environment.getStorageState(new File(DVR_SD_ROOT));
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return -1;
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return -1;
        }
        try {
            StatFs stat = new StatFs(DIRECTORY_DCIM);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return -1;
    }

    public static void makeDvrDirs() {
        File dir_dcim  = new File(DIRECTORY_DCIM  );
        File dir_photo = new File(DIRECTORY_PHOTO );
        File dir_video = new File(DIRECTORY_VIDEO );
        File dir_impact= new File(DIRECTORY_IMPACT);
        if (!dir_dcim  .exists()) dir_dcim  .mkdirs();
        if (!dir_photo .exists()) dir_photo .mkdirs();
        if (!dir_video .exists()) dir_video .mkdirs();
        if (!dir_impact.exists()) dir_impact.mkdirs();
    }

    private BroadcastReceiver mMediaChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Uri uri = intent.getData();
            String   path = uri.getPath();

            if (  action.equals(Intent.ACTION_MEDIA_EJECT)
               || action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ) {
                Log.i(TAG, "Intent.ACTION_MEDIA_EJECT path = " + path);
                if (path.equals(DVR_SD_ROOT)) {
                    Log.i(TAG, "sdcard removed");
                    if (mListerner != null) mListerner.onSdStateChanged(false);
                }
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Log.i(TAG, "Intent.ACTION_MEDIA_MOUNTED = " + path);
                if (path.equals(DVR_SD_ROOT)) {
                    Log.i(TAG, "sdcard inserted");
                    if (mListerner != null) mListerner.onSdStateChanged(true);
                }
            }
        }
    };
};





