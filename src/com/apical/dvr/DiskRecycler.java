
package com.apical.dvr;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.*;
import java.util.*;

public class DiskRecycler extends Service {
    private static final String TAG = "DiskRecycler";

    private ServiceBinder mBinder = new ServiceBinder();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        startDiskRecycle(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopDiskRecycle();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
//      return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class ServiceBinder extends Binder {
        public DiskRecycler getService() {
            return DiskRecycler.this;
        }
    }

    //++ disk recycle ++//
    private DiskRecycleThread mRecycleThread = null;
    public void startDiskRecycle(Context c) {
        if (mRecycleThread == null) {
            mRecycleThread = new DiskRecycleThread(c);
        }
        mRecycleThread.start();
    }

    public void stopDiskRecycle() {
        if (mRecycleThread != null) {
            mRecycleThread.mStopCheck = true;
            mRecycleThread = null;
        }
    }

    public void recycleDiskNow() {
        if (mRecycleThread != null) {
            mRecycleThread.startRecycleNow();
        }
    }

    public void waitRecycleDone() {
        if (mRecycleThread != null) {
            mRecycleThread.waitRecycleDone();
        }
    }
    //-- disk recycle --//
}

class DiskStorage {
    private static final String TAG = "DiskStorage";

    public static final int  DVR_PHOTO_KEEP_NUM  = 100;
    public static final int  DVR_VIDEO_KEEP_NUM  = 10;
    public static final int  DVR_IMPACT_KEEP_NUM = 10;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 500*1024*1024l; // 500M

    public static void makeCdrDirs() {
        File dir_dcim   = new File(SdcardManager.DIRECTORY_DCIM  );
        File dir_photo  = new File(SdcardManager.DIRECTORY_PHOTO );
        File dir_video  = new File(SdcardManager.DIRECTORY_VIDEO );
        File dir_impact = new File(SdcardManager.DIRECTORY_IMPACT);
        if (!dir_dcim  .exists()) dir_dcim  .mkdirs();
        if (!dir_photo .exists()) dir_photo .mkdirs();
        if (!dir_video .exists()) dir_video .mkdirs();
        if (!dir_impact.exists()) dir_impact.mkdirs();
    }

    public static long getAvailableSpace() {
        String state = Environment.getStorageState(new File(SdcardManager.DVR_SD_ROOT));
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return -1;
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return -1;
        }
        try {
            StatFs stat = new StatFs(SdcardManager.DIRECTORY_DCIM);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return -1;
    }
}

class DiskRecycleThread extends Thread
{
    private final static String TAG = "DiskRecycleThread";
    private final static int DISK_RECYCLE_PERIOD = 30 * 1000;

    private Context         mContext   = null;
    private ContentResolver mResolver  = null;
    private Object          mStartEvent= new Object();
    private Object          mDoneEvent = new Object();
    public  boolean         mStopCheck = false;

    public DiskRecycleThread(Context c) {
        mContext  = c;
        mResolver = c.getContentResolver();
    }

    public void startRecycleNow() {
        synchronized (mStartEvent) {
            mStartEvent.notify();
        }
    }

    public void waitRecycleDone() {
        synchronized (mDoneEvent) {
            try {
                mDoneEvent.wait(2 * 1000);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void run() {
        DiskStorage.makeCdrDirs(); // make cdr dirs

        while (!mStopCheck) {
            long avail   = DiskStorage.getAvailableSpace();
            long recycle = DiskStorage.LOW_STORAGE_THRESHOLD_BYTES - avail;
            Log.d(TAG, "===ck=== avail = " + avail + ", recycle = " + recycle);

            if (avail >= 0) {
                recycle = recycleDirectorySpace(SdcardManager.DIRECTORY_VIDEO , recycle, DiskStorage.DVR_VIDEO_KEEP_NUM , 0);
                recycle = recycleDirectorySpace(SdcardManager.DIRECTORY_IMPACT, recycle, DiskStorage.DVR_IMPACT_KEEP_NUM, 0);
                recycle = recycleDirectorySpace(SdcardManager.DIRECTORY_PHOTO , recycle, DiskStorage.DVR_PHOTO_KEEP_NUM , 1);
                if (recycle > 0) {
                    Log.e(TAG, "===ck=== recycle disk space failed: " + recycle);
                }
            }

            synchronized (mDoneEvent) {
                try {
                    mDoneEvent.notify();
                } catch (Exception e) { e.printStackTrace(); }
            }

            synchronized (mStartEvent) {
                try {
                    mStartEvent.wait(DISK_RECYCLE_PERIOD);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private void delImage(String path) {
        new ImageDelTask(path, mResolver).execute();
    }

    private void delVideo(String path) {
        new VideoDelTask(path, mResolver).execute();
    }

    private long recycleDirectorySpace(String path, long recycle, int keep, int type) {
        if (recycle > 0) {
            File dir = new File(path);
            if (!dir.exists()) {
                Log.e(TAG, "can't find " + path + " directory !");
            }
            else {
                File[] files = dir.listFiles();
                int    num   = files.length - keep;
                if (num > 0) {
                    sortFilesByLastModified(files);

                    for (File f : files) {
                        recycle -= f.length();

                        try {
                            switch (type) {
                            case 0: delVideo(f.getCanonicalPath()); break;
                            case 1: delImage(f.getCanonicalPath()); break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (recycle <= 0 || --num <= 0) {
                            break;
                        }
                    }
                }
            }
        }
        return recycle;
    }

    public static void sortFilesByLastModified(File[] source) {
        class FileComparator implements Comparator <File> {
            @Override
            public int compare(File left, File right) {
                long a = left .lastModified();
                long b = right.lastModified();
                if      (a > b) return  1;
                else if (a < b) return -1;
                else            return  0;
            }
        }

        if (source != null) {
            Arrays.sort(source, new FileComparator());
        }
    }

    private class ImageDelTask extends AsyncTask <Void, Void, Uri> {
        private String mPath;
        private final ContentResolver mResolver;

        public ImageDelTask(String path, ContentResolver r) {
            mPath     = path;
            mResolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            String params[] = new String[] { mPath };
            try {
                mResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " LIKE ?", params);
            } catch (Exception e) { e.printStackTrace(); }
            File f = new File(mPath); if (f.exists()) f.delete();
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            // todo...
        }
    }

    private class VideoDelTask extends AsyncTask <Void, Void, Uri> {
        private String mPath;
        private final ContentResolver mResolver;

        public VideoDelTask(String path, ContentResolver r) {
            mPath     = path;
            mResolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            String params[] = new String[] { mPath };
            try {
                mResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + " LIKE ?", params);
            } catch (Exception e) { e.printStackTrace(); }
            File f = new File(mPath); if (f.exists()) f.delete();
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            // todo...
        }
    }
}

