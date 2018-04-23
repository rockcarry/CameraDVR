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
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A class implementing {@link com.android.camera.app.MediaSaver}.
 */
public class MediaSaver {
    private static final String TAG = "MediaSaver";

    // for media save
    private ContentResolver mResolver = null;

    private static MediaSaver mInstance = null;
    public static MediaSaver getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MediaSaver(context);
        }
        return mInstance;
    }

    public MediaSaver(Context context) {
        mResolver = context.getContentResolver();
    }

    public void addImage(String path, long date, Location loc, int width, int height, int orientation) {
        ImageSaveTask t = new ImageSaveTask(path, date,
                (loc == null) ? null : new Location(loc),
                width, height, orientation, mResolver);
        t.execute();
    }

    public void delImage(String path) {
        new ImageDelTask(path, mResolver).execute();
    }

    public void addVideo(String path, long date, int w, int h, long duration, boolean impact) {
        new VideoSaveTask(path, date, w, h, duration, impact, mResolver).execute();
    }

    public void delVideo(String path) {
        new VideoDelTask(path, mResolver).execute();
    }

    private class ImageSaveTask extends AsyncTask <Void, Void, Uri> {
        private String path;
        private long date;
        private Location loc;
        private int width, height;
        private int orientation;
        private ContentResolver resolver;

        public ImageSaveTask(String path, long date, Location loc, int width, int height,
                             int orientation, ContentResolver resolver) {
            this.path = path;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.resolver = resolver;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
            File file = new File(path);
            long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());
            long fileLength = file.length();

            ContentValues values = new ContentValues();
//          values.put(ImageColumns.TITLE, title);
//          values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
            values.put(ImageColumns.DATE_TAKEN, date);
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            values.put(ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(ImageColumns.ORIENTATION, orientation);
            values.put(ImageColumns.DATA, path);
            values.put(ImageColumns.SIZE, fileLength);
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);

            if (loc != null) {
                values.put(ImageColumns.LATITUDE, loc.getLatitude());
                values.put(ImageColumns.LONGITUDE, loc.getLongitude());
            }

            Uri uri = null;
            try {
                uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
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

    private class ImageDelTask extends AsyncTask <Void, Void, Uri> {
        private String path;
        private ContentResolver resolver;

        public ImageDelTask(String path, ContentResolver r) {
            this.path     = path;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            String params[] = new String[] { this.path };
            this.resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " LIKE ?", params);
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            // todo...
        }
    }

    private class VideoSaveTask extends AsyncTask <Void, Void, Uri> {
        private String  path;
        private long    date;
        private int     width;
        private int     height;
        private long    duration;
        private boolean impact;
        private ContentResolver resolver;

        public VideoSaveTask(String path, long date, int w, int h, long duration, boolean impact, ContentResolver r) {
            this.path     = path;
            this.date     = date;
            this.width    = w;
            this.height   = h;
            this.duration = duration;
            this.impact   = impact;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            ContentValues values = null;
            Uri           uri    = null;
            try {
                if (impact) {
                    String pathold = path;
                    String pathnew = path.replace("DVR_Video", "DVR_Impact");
                    File fileold = new File(pathold);
                    File filenew = new File(pathnew);
                    fileold.renameTo(filenew);
                    path = pathnew;
                }

                values = new ContentValues();
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.DATA, path);
                values.put(MediaStore.Video.Media.DATE_TAKEN, date);
                values.put(MediaStore.Video.Media.WIDTH, width);
                values.put(MediaStore.Video.Media.HEIGHT, height);
                values.put(MediaStore.Video.Media.DURATION, duration);
                uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                resolver.update(uri, values, null, null);
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

    private class VideoDelTask extends AsyncTask <Void, Void, Uri> {
        private String path;
        private ContentResolver resolver;

        public VideoDelTask(String path, ContentResolver r) {
            this.path     = path;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            String params[] = new String[] { this.path };
            this.resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + " LIKE ?", params);
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            // todo...
        }
    }
}


