package com.apical.dvr;

import android.app.TabActivity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class BrowserActivity extends TabActivity
{
    private static final String TAG = "BrowserActivity";

    private ListView mListViewNormalVideoA;
    private ListView mListViewLockedVideoA;
    private ListView mListViewPhotoA;
    private ListView mListViewNormalVideoB;
    private ListView mListViewLockedVideoB;
    private ListView mListViewPhotoB;
    private MediaListAdapter mAdapterNormalVideoA;
    private MediaListAdapter mAdapterLockedVideoA;
    private MediaListAdapter mAdapterPhotoA;
    private MediaListAdapter mAdapterNormalVideoB;
    private MediaListAdapter mAdapterLockedVideoB;
    private MediaListAdapter mAdapterPhotoB;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        TabHost tabhost = getTabHost();
        if (false) {
            String[] tabnames = getResources().getStringArray(R.array.category_two_camera);
            tabhost.addTab(tabhost.newTabSpec(tabnames[0]).setIndicator(tabnames[0], null).setContent(R.id.tab_normal_video_a));
            tabhost.addTab(tabhost.newTabSpec(tabnames[1]).setIndicator(tabnames[1], null).setContent(R.id.tab_locked_video_a));
            tabhost.addTab(tabhost.newTabSpec(tabnames[2]).setIndicator(tabnames[2], null).setContent(R.id.tab_photo_a));
            tabhost.addTab(tabhost.newTabSpec(tabnames[3]).setIndicator(tabnames[3], null).setContent(R.id.tab_normal_video_b));
            tabhost.addTab(tabhost.newTabSpec(tabnames[4]).setIndicator(tabnames[4], null).setContent(R.id.tab_locked_video_b));
            tabhost.addTab(tabhost.newTabSpec(tabnames[5]).setIndicator(tabnames[5], null).setContent(R.id.tab_photo_b));
        } else {
            String[] tabnames = getResources().getStringArray(R.array.category_one_camera);
            tabhost.addTab(tabhost.newTabSpec(tabnames[0]).setIndicator(tabnames[0], null).setContent(R.id.tab_normal_video_a));
            tabhost.addTab(tabhost.newTabSpec(tabnames[1]).setIndicator(tabnames[1], null).setContent(R.id.tab_locked_video_a));
            tabhost.addTab(tabhost.newTabSpec(tabnames[2]).setIndicator(tabnames[2], null).setContent(R.id.tab_photo_a));
        }

        TabWidget tw = getTabWidget();
        for (int i=0; i<tw.getChildCount(); i++) {
            TextView tv = (TextView)tw.getChildAt(i).findViewById(android.R.id.title);
            tv.setPadding(2, 5, 2, 5);
            tv.setTextSize(20);
            tw.getChildAt(i).getLayoutParams().height = 50;
        }

        mAdapterNormalVideoA  = new MediaListAdapter(this, SdcardManager.DIRECTORY_VIDEO  + "/A_VID_");
        mAdapterLockedVideoA  = new MediaListAdapter(this, SdcardManager.DIRECTORY_IMPACT + "/A_VID_");
        mAdapterPhotoA        = new MediaListAdapter(this, SdcardManager.DIRECTORY_PHOTO  + "/A_IMG_");
        mAdapterNormalVideoB  = new MediaListAdapter(this, SdcardManager.DIRECTORY_VIDEO  + "/B_VID_");
        mAdapterLockedVideoB  = new MediaListAdapter(this, SdcardManager.DIRECTORY_IMPACT + "/B_VID_");
        mAdapterPhotoB        = new MediaListAdapter(this, SdcardManager.DIRECTORY_PHOTO  + "/B_IMG_");

        mListViewNormalVideoA = (ListView) findViewById(R.id.lv_normal_video_a);
        mListViewLockedVideoA = (ListView) findViewById(R.id.lv_locked_video_a);
        mListViewPhotoA       = (ListView) findViewById(R.id.lv_photo_a       );
        mListViewNormalVideoB = (ListView) findViewById(R.id.lv_normal_video_b);
        mListViewLockedVideoB = (ListView) findViewById(R.id.lv_locked_video_b);
        mListViewPhotoB       = (ListView) findViewById(R.id.lv_photo_b       );

        mListViewNormalVideoA.setAdapter(mAdapterNormalVideoA);
        mListViewLockedVideoA.setAdapter(mAdapterLockedVideoA);
        mListViewPhotoA      .setAdapter(mAdapterPhotoA      );
        mListViewNormalVideoB.setAdapter(mAdapterNormalVideoB);
        mListViewLockedVideoB.setAdapter(mAdapterLockedVideoB);
        mListViewPhotoB      .setAdapter(mAdapterPhotoB      );

        mListViewNormalVideoA.setOnItemClickListener(mAdapterNormalVideoA);
        mListViewLockedVideoA.setOnItemClickListener(mAdapterLockedVideoA);
        mListViewPhotoA      .setOnItemClickListener(mAdapterPhotoA);
        mListViewNormalVideoB.setOnItemClickListener(mAdapterNormalVideoB);
        mListViewLockedVideoB.setOnItemClickListener(mAdapterLockedVideoB);
        mListViewPhotoB      .setOnItemClickListener(mAdapterPhotoB);
        mListViewNormalVideoA.setOnItemLongClickListener(mAdapterNormalVideoA);
        mListViewLockedVideoA.setOnItemLongClickListener(mAdapterLockedVideoA);
        mListViewPhotoA      .setOnItemLongClickListener(mAdapterPhotoA);
        mListViewNormalVideoB.setOnItemLongClickListener(mAdapterNormalVideoB);
        mListViewLockedVideoB.setOnItemLongClickListener(mAdapterLockedVideoB);
        mListViewPhotoB      .setOnItemLongClickListener(mAdapterPhotoB);

        mAdapterNormalVideoA.reload();
        mAdapterLockedVideoA.reload();
        mAdapterPhotoA      .reload();
        mAdapterNormalVideoB.reload();
        mAdapterLockedVideoB.reload();
        mAdapterPhotoB      .reload();

        getContentResolver().registerContentObserver(MediaStore.Video .Media.EXTERNAL_CONTENT_URI, false, mVideoObserver);
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mPhotoObserver);
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mVideoObserver);
        getContentResolver().unregisterContentObserver(mPhotoObserver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private Handler mHandler = new Handler();
    private ContentObserver mVideoObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            mAdapterNormalVideoA.reload();
            mAdapterLockedVideoA.reload();
            mAdapterNormalVideoB.reload();
            mAdapterLockedVideoB.reload();
            mAdapterNormalVideoA.notifyDataSetChanged();
            mAdapterLockedVideoA.notifyDataSetChanged();
            mAdapterNormalVideoB.notifyDataSetChanged();
            mAdapterLockedVideoB.notifyDataSetChanged();
        }
    };

    private ContentObserver mPhotoObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            mAdapterPhotoA.reload();
            mAdapterPhotoB.reload();
            mAdapterPhotoA.notifyDataSetChanged();
            mAdapterPhotoB.notifyDataSetChanged();
        }
    };
}

class MediaListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private Context             mContext;
    private ContentResolver     mResolver;
    private List<MediaListItem> mMediaList;
    private String              mMediaPath;
    private boolean             mIsPhoto;

    public MediaListAdapter(Context context, String path) {
        mContext     = context;
        mResolver    = mContext.getContentResolver();
        mMediaList   = new ArrayList();
        mMediaPath   = path;
        mIsPhoto     = path.startsWith(SdcardManager.DIRECTORY_PHOTO);
    }

    public void reload() {
        String[] vidmediacols = new String[] {
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
        };
        String[] imgmediacols = new String[] {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media._ID,
        };

        mMediaList.clear();
        if (mIsPhoto) {
            Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgmediacols,
                                            MediaStore.Images.Media.DATA + " like ? ",
                                            new String[] { mMediaPath + "%" },
                                            MediaStore.Images.Media.DATE_TAKEN + " desc");
            if (cursor.moveToFirst()) {
                do {
                    MediaListItem item= new MediaListItem();
                    item.fl_path      = cursor.getString(0);
                    item.fl_name      = cursor.getString(1);
                    item.fl_detail1   = String.format("%10s", String.format("%dx%d", cursor.getInt(2), cursor.getInt(3)));
                    item.fl_detail2   = "";
                    item.fl_size      = formatFileSizeString(cursor.getInt(4));
                    item.fl_thumb     = cursor.getInt(5);
                    mMediaList.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            Cursor cursor = mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, vidmediacols,
                                            MediaStore.Video.Media.DATA + " like ? ",
                                            new String[] { mMediaPath + "%" },
                                            MediaStore.Video.Media.DATE_TAKEN + " desc");
            if (cursor.moveToFirst()) {
                do {
                    MediaListItem item= new MediaListItem();
                    item.fl_path      = cursor.getString(0);
                    item.fl_name      = cursor.getString(1);
                    item.fl_detail1   = String.format("%5dp", cursor.getInt(3));
                    item.fl_detail2   = formatDurationString(cursor.getInt(4));
                    item.fl_size      = formatFileSizeString(cursor.getInt(5));
                    item.fl_thumb     = cursor.getInt(6);
                    mMediaList.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.file_item, null);
            holder = new ViewHolder();
            holder.fi_image   = (ImageView) convertView.findViewById(R.id.fi_file_image  );
            holder.fl_name    = (TextView ) convertView.findViewById(R.id.fi_file_name   );
            holder.fl_detail1 = (TextView ) convertView.findViewById(R.id.fi_file_detail1);
            holder.fl_detail2 = (TextView ) convertView.findViewById(R.id.fi_file_detail2);
            holder.fl_size    = (TextView ) convertView.findViewById(R.id.fi_file_size   );
            holder.fl_name   .setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.fl_detail1.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.fl_detail2.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.fl_size   .setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MediaListItem item = mMediaList.get(position);
        if (item.fl_bitmap == null) {
            if (mIsPhoto) {
                item.fl_bitmap = MediaStore.Images.Thumbnails.getThumbnail(mResolver, item.fl_thumb, MediaStore.Images.Thumbnails.MINI_KIND, null);
            } else {
                item.fl_bitmap = MediaStore.Video .Thumbnails.getThumbnail(mResolver, item.fl_thumb, MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
        }
        holder.fi_image  .setImageBitmap(item.fl_bitmap);
        holder.fl_name   .setText(item.fl_name);
        holder.fl_detail1.setText(item.fl_detail1);
        holder.fl_detail2.setText(item.fl_detail2);
        holder.fl_size   .setText(item.fl_size);
        return convertView;
    }

    @Override
    public final int getCount() {
        return mMediaList.size();
    }

    @Override
    public final Object getItem(int position) {
        return mMediaList.get(position);
    }

    @Override
    public final long getItemId(int position) {
        return position;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaListItem item = mMediaList.get(position);
        if (mIsPhoto) {
            openPhoto(mContext, item.fl_path, item.fl_name);
        } else {
            playVideo(mContext, item.fl_path, item.fl_name);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return true;
    }

    public static String formatDurationString(int duration) {
        int min = duration / 60000;
        int sec = duration % 60000 / 1000;
        String str = "";
        if (min != 0 && sec != 0) {
            str = String.format("%dmin%ds", min, sec);
        } else if (min != 0) {
            str = String.format("%dmin", min);
        } else {
            str = String.format("%ds", sec);
        }
        return String.format("%8s", str);
    }

    public static String formatFileSizeString(int size) {
        String str = "";
        if (size < 1024) {
            str = String.format("%d B", size);
        } else if (size < 1024L * 1024L) {
            str = String.format("%.2f KB", (double)size / 1024);
        } else if (size < 1024L * 1024L * 1024L) {
            str = String.format("%.2f MB", (double)size / (1024 * 1024));
        } else {
            str = String.format("%.2f GB", (double)size / (1024L * 1024L * 1024));
        }
        return String.format("%8s", str);
    }

    public static void openPhoto(Context context, String path, String title) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + path), "image/*");
        context.startActivity(intent);
    }

    public static void playVideo(Context context, String path, String title) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + path), "video/*");
        intent.putExtra(MediaStore.PLAYLIST_TYPE, MediaStore.PLAYLIST_TYPE_CUR_FOLDER);
        intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
        context.startActivity(intent);
    }

    class MediaListItem {
        Bitmap  fl_bitmap;
        int     fl_thumb;
        String  fl_path;
        String  fl_name;
        String  fl_detail1;
        String  fl_detail2;
        String  fl_size;
    }

    class ViewHolder {
        ImageView  fi_image;
        TextView   fl_name;
        TextView   fl_detail1;
        TextView   fl_detail2;
        TextView   fl_size;
    }
}
