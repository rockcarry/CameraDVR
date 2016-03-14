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

package com.apical.cdr;

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

import com.android.camera.exif.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A class implementing {@link com.android.camera.app.MediaSaver}.
 */
public class MediaSaver implements GSensorMonitor.ImpactEventListener {
    private static final String TAG = "MediaSaver";

    /** The memory limit for unsaved image is 20MB. */
    private static final int SAVE_TASK_MEMORY_LIMIT = 20 * 1024 * 1024;

    /** Memory used by the total queued save request, in bytes. */
    private long mMemoryUse = 0;

    // for media save
    private ContentResolver mResolver = null;

    // impact flag & listener
    private boolean mImpactFlag = false;

    public MediaSaver(Context context) {
        mResolver = context.getContentResolver();
    }

    public void addImage(final byte[] data, String path, long date, Location loc, int width,
            int height, int orientation, ExifInterface exif) {
        if (mMemoryUse >= SAVE_TASK_MEMORY_LIMIT) {
            Log.e(TAG, "cannot add image when the queue is full");
            return;
        }

        ImageSaveTask t = new ImageSaveTask(data, path, date,
                (loc == null) ? null : new Location(loc),
                width, height, orientation, exif, mResolver);

        mMemoryUse += data.length;

        t.execute();
    }

    public void delImage(String path) {
        new ImageDelTask(path, mResolver).execute();
    }

    public void addVideo(String path, int w, int h) {
        new VideoSaveTask(path, w, h, mResolver).execute();
    }

    public void delVideo(String path) {
        new VideoDelTask(path, mResolver).execute();
    }

    @Override
    public void onGsensorImpactEvent(boolean f) {
        mImpactFlag = f;
    }

    private class ImageSaveTask extends AsyncTask <Void, Void, Uri> {
        private final byte[] data;
        private final String path;
        private final long date;
        private final Location loc;
        private int width, height;
        private final int orientation;
        private final ExifInterface exif;
        private final ContentResolver resolver;

        public ImageSaveTask(byte[] data, String path, long date, Location loc,
                             int width, int height, int orientation, ExifInterface exif,
                             ContentResolver resolver) {
            this.data = data;
            this.path = path;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
            if (width == 0 || height == 0) {
                // Decode bounds
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                width = options.outWidth;
                height = options.outHeight;
            }

            // write photo file
            long fileLength = writeFile(path, data, exif);
            if (fileLength <= 0) return null;

            File file = new File(path);
            long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());

            ContentValues values = new ContentValues();
//          values.put(ImageColumns.TITLE, title);
//          values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
            values.put(ImageColumns.DATE_TAKEN, date);
            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
            values.put(ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
            // Clockwise rotation in degrees. 0, 90, 180, or 270.
            values.put(ImageColumns.ORIENTATION, orientation);
            values.put(ImageColumns.DATA, path);
            values.put(ImageColumns.SIZE, data.length);
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
            mMemoryUse -= data.length;
        }
    }

    private class ImageDelTask extends AsyncTask <Void, Void, Uri> {
        private String path;
        private final ContentResolver resolver;

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
        private String path;
        private int    width;
        private int    height;
        private final ContentResolver resolver;

        public VideoSaveTask(String path, int w, int h, ContentResolver r) {
            this.path     = path;
            this.width    = w;
            this.height   = h;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            ContentValues values = null;
            Uri           uri    = null;
            try {
                if (mImpactFlag) {
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
        private final ContentResolver resolver;

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

    /**
     * Writes the JPEG data to a file. If there's EXIF info, the EXIF header
     * will be added.
     *
     * @param path The path to the target file.
     * @param jpeg The JPEG data.
     * @param exif The EXIF info. Can be {@code null}.
     *
     * @return The size of the file. -1 if failed.
     */
    private static long writeFile(String path, byte[] jpeg, ExifInterface exif) {
        if (exif != null) {
            try {
                exif.writeExif(jpeg, path);
                File f = new File(path);
                return f.length();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else {
            return writeFile(path, jpeg);
        }
        return -1;
    }

    /**
     * Writes the data to a file.
     *
     * @param path The path to the target file.
     * @param data The data to save.
     *
     * @return The size of the file. -1 if failed.
     */
    private static long writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            return data.length;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return -1;
    }
}


