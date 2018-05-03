/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apical.dvr;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A class implementing {@link com.android.camera.app.MediaManager}.
 */
public class MediaManager {
    private static final String TAG = "MediaManager";

    // for media save
    private ContentResolver mResolver = null;

    private static MediaManager mInstance = null;
    public static MediaManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MediaManager(context);
        }
        return mInstance;
    }

    public MediaManager(Context context) {
        mResolver = context.getContentResolver();
    }

    public void addImage(String path, long date, Location loc, int width, int height, int orientation) {
        new ImageSaveTask(path, date, loc, width, height, orientation, mResolver).execute();
    }

    public void addVideo(String path, long date, int w, int h, long duration, boolean impact) {
        new VideoSaveTask(path, date, w, h, duration, impact, mResolver).execute();
    }

    public void delImage(String path) {
        delImages(new String[] { path }, null);
    }

    public void delVideo(String path) {
        delVideos(new String[] { path }, null);
    }

    public void setVideoLockType(String path, boolean lock) {
        setVideosLockType(new String[] { path }, lock, null);
    }

    public void delImages(String[] paths, Handler h) {
        new ImagesDelTask(paths, mResolver, h).execute();
    }

    public void delVideos(String[] paths, Handler h) {
        new VideosDelTask(paths, mResolver, h).execute();
    }

    public void setVideosLockType(String[] paths, boolean lock, Handler h) {
        new VideosSetLockTask(paths, lock, mResolver, h).execute();
    }

    private class ImageSaveTask extends AsyncTask <Void, Void, Uri> {
        private String          mPath;
        private long            mDate;
        private Location        mLoc;
        private int             mWidth;
        private int             mHeight;
        private int             mOrientation;
        private ContentResolver mResolver;

        public ImageSaveTask(String path, long date, Location loc, int width, int height,
                             int orientation, ContentResolver resolver) {
            mPath        = path;
            mDate        = date;
            mLoc         = loc;
            mWidth       = width;
            mHeight      = height;
            mOrientation = orientation;
            mResolver    = resolver;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
            File file = new File(mPath);
            long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());
            long fileLength = file.length();

            ContentValues values = new ContentValues();
//          values.put(ImageColumns.TITLE, title);
//          values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
            values.put(ImageColumns.DATE_TAKEN, mDate);
            values.put(ImageColumns.MIME_TYPE , "image/jpeg");
            values.put(ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(ImageColumns.ORIENTATION, mOrientation);
            values.put(ImageColumns.DATA  , mPath);
            values.put(ImageColumns.SIZE  , fileLength);
            values.put(MediaColumns.WIDTH , mWidth);
            values.put(MediaColumns.HEIGHT, mHeight);

            if (mLoc != null) {
                values.put(ImageColumns.LATITUDE , mLoc.getLatitude ());
                values.put(ImageColumns.LONGITUDE, mLoc.getLongitude());
            }

            Uri uri = null;
            try {
                uri = mResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Throwable th)  {
                // This can happen when the external volume is already mounted, but
                // MediaScanner has not notify MediaProvider to add that volume.
                // The picture is still safe and MediaScanner will find it and
                // insert it into MediaProvider. The only problem is that the user
                // cannot click the thumbnail to review the picture.
                Log.e(TAG, "Failed to write MediaStore" + th);
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
        }
    }

    private class VideoSaveTask extends AsyncTask <Void, Void, Uri> {
        private String  mPath;
        private long    mDate;
        private int     mWidth;
        private int     mHeight;
        private long    mDuration;
        private boolean mImpact;
        private ContentResolver mResolver;

        public VideoSaveTask(String path, long date, int w, int h, long duration, boolean impact, ContentResolver r) {
            mPath     = path;
            mDate     = date;
            mWidth    = w;
            mHeight   = h;
            mDuration = duration;
            mImpact   = impact;
            mResolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            ContentValues values = null;
            Uri           uri    = null;
            try {
                if (mImpact) {
                    String pathold = mPath;
                    String pathnew = mPath.replace("DVR_Video", "DVR_Impact");
                    new File(pathold).renameTo(new File(pathnew));
                    mPath = pathnew;
                }

                values = new ContentValues();
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.DATA, mPath);
                values.put(MediaStore.Video.Media.DATE_TAKEN, mDate);
                values.put(MediaStore.Video.Media.WIDTH   , mWidth );
                values.put(MediaStore.Video.Media.HEIGHT  , mHeight);
                values.put(MediaStore.Video.Media.DURATION, mDuration);
                uri = mResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                mResolver.update(uri, values, null, null);
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                Log.e(TAG, "failed to add video to media store", e);
                uri = null;
            } finally {
                Log.v(TAG, "Current video URI: " + uri);
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            // todo...
        }
    }

    private class ImagesDelTask extends AsyncTask <Void, Void, Uri> {
        private String[]        mPaths;
        private ContentResolver mResolver;
        private Handler         mHandler;

        public ImagesDelTask(String[] paths, ContentResolver r, Handler h) {
            mPaths    = paths;
            mResolver = r;
            mHandler  = h;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            for (String path : mPaths) {
                String params[] = new String[] { path };
                mResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " LIKE ?", params);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(BrowserActivity.MSG_DELETE_IMAGES_DONE);
            }
        }
    }

    private class VideosDelTask extends AsyncTask <Void, Void, Uri> {
        private String[]        mPaths;
        private ContentResolver mResolver;
        private Handler         mHandler;

        public VideosDelTask(String[] paths, ContentResolver r, Handler h) {
            mPaths    = paths;
            mResolver = r;
            mHandler  = h;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            for (String path : mPaths) {
                String params[] = new String[] { path };
                mResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + " LIKE ?", params);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(BrowserActivity.MSG_DELETE_VIDEOS_DONE);
            }
        }
    }

    private class VideosSetLockTask extends AsyncTask <Void, Void, Uri> {
        private String[]        mPaths;
        private boolean         mLock;
        private ContentResolver mResolver;
        private Handler         mHandler;

        public VideosSetLockTask(String[] paths, boolean lock, ContentResolver r, Handler h) {
            mPaths    = paths;
            mLock     = lock;
            mResolver = r;
            mHandler  = h;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            for (String path : mPaths) {
                if (mLock == path.startsWith(SdcardManager.DIRECTORY_IMPACT)) {
                    continue;
                }

                String newpath;
                if (mLock) {
                    newpath = path.replace(SdcardManager.DIRECTORY_VIDEO, SdcardManager.DIRECTORY_IMPACT);
                } else {
                    newpath = path.replace(SdcardManager.DIRECTORY_IMPACT, SdcardManager.DIRECTORY_VIDEO);
                }
                String params[] = new String[] { path };
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, newpath);
                mResolver.update(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Video.Media.DATA + " LIKE ?", params);
                (new File(path)).renameTo(new File(newpath));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(BrowserActivity.MSG_SET_LOCK_TYPE_DONE);
            }
        }
    }
}


