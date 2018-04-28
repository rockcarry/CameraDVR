package com.apical.dvr;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class BrowserActivity extends TabActivity implements View.OnClickListener
{
    private static final String TAG = "BrowserActivity";
    public  static final int MSG_UPDATE_VIEW_LIST    = 100;
    public  static final int MSG_ENABLE_MULTI_SELECT = 101;

    private ListView mListViewNormalVideoA;
    private ListView mListViewLockedVideoA;
    private ListView mListViewPhotoA;
    private ListView mListViewNormalVideoB;
    private ListView mListViewLockedVideoB;
    private ListView mListViewPhotoB;
    private LinearLayout mLayoutMultiSelMenu;
    private Button       mBtnSelectAll;
    private Button       mBtnLock;
    private Button       mBtnUnlock;
    private Button       mBtnDelete;
    private Button       mBtnCancel;
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

        mAdapterNormalVideoA  = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_VIDEO  + "/A_VID_");
        mAdapterLockedVideoA  = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_IMPACT + "/A_VID_");
        mAdapterPhotoA        = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_PHOTO  + "/A_IMG_");
        mAdapterNormalVideoB  = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_VIDEO  + "/B_VID_");
        mAdapterLockedVideoB  = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_IMPACT + "/B_VID_");
        mAdapterPhotoB        = new MediaListAdapter(this, mHandler, SdcardManager.DIRECTORY_PHOTO  + "/B_IMG_");

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

        mLayoutMultiSelMenu   = (LinearLayout) findViewById(R.id.layout_multisel_menu);
        mBtnSelectAll         = (Button      ) findViewById(R.id.btn_select_all);
        mBtnLock              = (Button      ) findViewById(R.id.btn_lock      );
        mBtnUnlock            = (Button      ) findViewById(R.id.btn_unlock    );
        mBtnDelete            = (Button      ) findViewById(R.id.btn_delete    );
        mBtnCancel            = (Button      ) findViewById(R.id.btn_cancel    );
        mBtnSelectAll.setOnClickListener(this);
        mBtnLock     .setOnClickListener(this);
        mBtnUnlock   .setOnClickListener(this);
        mBtnDelete   .setOnClickListener(this);
        mBtnCancel   .setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_select_all:
            break;
        case R.id.btn_lock:
            break;
        case R.id.btn_unlock:
            break;
        case R.id.btn_delete:
            break;
        case R.id.btn_cancel:
            mLayoutMultiSelMenu.setVisibility(View.GONE);
            break;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_VIEW_LIST:
                switch (msg.arg1) {
                case 0: mAdapterNormalVideoA.notifyDataSetChanged(); break;
                case 1: mAdapterLockedVideoA.notifyDataSetChanged(); break;
                case 2: mAdapterPhotoA.notifyDataSetChanged();       break;
                case 3: mAdapterNormalVideoB.notifyDataSetChanged(); break;
                case 4: mAdapterLockedVideoB.notifyDataSetChanged(); break;
                case 5: mAdapterPhotoB.notifyDataSetChanged();       break;
                }
                break;
            case MSG_ENABLE_MULTI_SELECT:
                mLayoutMultiSelMenu.setVisibility(View.VISIBLE);
                break;
            }
        }
    };

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
    private Handler             mHandler;
    private ContentResolver     mResolver;
    private List<MediaListItem> mMediaListOld = new ArrayList();
    private List<MediaListItem> mMediaListNew = new ArrayList();
    private String              mMediaPath;
    private boolean             mIsPhoto;

    public MediaListAdapter(Context context, Handler handler, String path) {
        mContext     = context;
        mHandler     = handler;
        mResolver    = mContext.getContentResolver();
        mMediaPath   = path;
        mIsPhoto     = path.startsWith(SdcardManager.DIRECTORY_PHOTO);
    }

    private Thread  mLoadThread     = null;
    private boolean mExitLoadThread = false;
    private void doLoadBitmaps() {
        // copy bitmap which already loaded from old list to new list
        for (MediaListItem ni : mMediaListNew) {
            for (MediaListItem oi : mMediaListOld) {
                if (ni.mi_thumb == oi.mi_thumb && oi.mi_bitmap != null) {
                    ni.mi_bitmap = oi.mi_bitmap;
                    break;
                }
            }
        }

        // clear old list to release memory
        mMediaListOld.clear();

        // load bitmaps which did not loaded
        for (MediaListItem ni : mMediaListNew) {
            if (mExitLoadThread) break;
            if (ni.mi_bitmap == null) {
                ni.mi_bitmap = mIsPhoto ?
                      MediaStore.Images.Thumbnails.getThumbnail(mResolver, ni.mi_thumb, MediaStore.Images.Thumbnails.MINI_KIND, null)
                    : MediaStore.Video .Thumbnails.getThumbnail(mResolver, ni.mi_thumb, MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
        }

        Message msg = new Message();
        msg.what = BrowserActivity.MSG_UPDATE_VIEW_LIST;
        if (mMediaPath.startsWith(SdcardManager.DIRECTORY_VIDEO  + "/A_VID_")) {
            msg.what = 0;
        } else if (mMediaPath.startsWith(SdcardManager.DIRECTORY_IMPACT + "/A_VID_")) {
            msg.what = 1;
        } else if (mMediaPath.startsWith(SdcardManager.DIRECTORY_PHOTO  + "/A_IMG_")) {
            msg.what = 2;
        } else if (mMediaPath.startsWith(SdcardManager.DIRECTORY_VIDEO  + "/B_VID_")) {
            msg.what = 3;
        } else if (mMediaPath.startsWith(SdcardManager.DIRECTORY_IMPACT + "/B_VID_")) {
            msg.what = 4;
        } else if (mMediaPath.startsWith(SdcardManager.DIRECTORY_PHOTO  + "/B_IMG_")) {
            msg.what = 5;
        }
        mHandler.sendMessage(msg);
    }

    class LoadBitmapsThread extends Thread {
        @Override
        public void run() {
            mExitLoadThread = false;
            doLoadBitmaps();
            mLoadThread = null;
        }
    }

    public void reload() {
        if (mLoadThread != null) {
            mExitLoadThread = true;
            try { mLoadThread.join(); } catch (Exception e) { e.printStackTrace(); }
        }

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

        mMediaListOld = mMediaListNew;
        mMediaListNew = new ArrayList();
        if (mIsPhoto) {
            Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgmediacols,
                                            MediaStore.Images.Media.DATA + " like ? ",
                                            new String[] { mMediaPath + "%" },
                                            MediaStore.Images.Media.DATE_TAKEN + " desc");
            if (cursor.moveToFirst()) {
                do {
                    MediaListItem item= new MediaListItem();
                    item.mi_path      = cursor.getString(0);
                    item.mi_name      = cursor.getString(1);
                    item.mi_detail1   = String.format("%10s", String.format("%dx%d", cursor.getInt(2), cursor.getInt(3)));
                    item.mi_detail2   = "";
                    item.mi_size      = formatFileSizeString(cursor.getInt(4));
                    item.mi_thumb     = cursor.getInt(5);
                    mMediaListNew.add(item);
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
                    item.mi_path      = cursor.getString(0);
                    item.mi_name      = cursor.getString(1);
                    item.mi_detail1   = String.format("%5dp", cursor.getInt(3));
                    item.mi_detail2   = formatDurationString(cursor.getInt(4));
                    item.mi_size      = formatFileSizeString(cursor.getInt(5));
                    item.mi_thumb     = cursor.getInt(6);
                    mMediaListNew.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        mLoadThread = new LoadBitmapsThread();
        mLoadThread.start();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.file_item, null);
            holder = new ViewHolder();
            holder.mi_image   = (ImageView) convertView.findViewById(R.id.fi_file_image  );
            holder.mi_name    = (TextView ) convertView.findViewById(R.id.fi_file_name   );
            holder.mi_detail1 = (TextView ) convertView.findViewById(R.id.fi_file_detail1);
            holder.mi_detail2 = (TextView ) convertView.findViewById(R.id.fi_file_detail2);
            holder.mi_size    = (TextView ) convertView.findViewById(R.id.fi_file_size   );
            holder.mi_name   .setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.mi_detail1.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.mi_detail2.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            holder.mi_size   .setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MediaListItem item = mMediaListNew.get(position);
        holder.mi_image  .setImageBitmap(item.mi_bitmap);
        holder.mi_name   .setText(item.mi_name);
        holder.mi_detail1.setText(item.mi_detail1);
        holder.mi_detail2.setText(item.mi_detail2);
        holder.mi_size   .setText(item.mi_size);
        return convertView;
    }

    @Override
    public final int getCount() {
        return mMediaListNew.size();
    }

    @Override
    public final Object getItem(int position) {
        return mMediaListNew.get(position);
    }

    @Override
    public final long getItemId(int position) {
        return position;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaListItem item = mMediaListNew.get(position);
        if (mIsPhoto) {
            openPhoto(mContext, item.mi_path, item.mi_name);
        } else {
            playVideo(mContext, item.mi_path, item.mi_name);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final int     final_pos  = position;
        final boolean final_lock = mMediaPath.startsWith(SdcardManager.DIRECTORY_IMPACT);

        String[] items = null;
        if (mIsPhoto) {
            items = new String[3];
            items[0] = mContext.getString(R.string.open);
            items[1] = mContext.getString(R.string.delete);
            items[2] = mContext.getString(R.string.multi_select);
        } else {
            items = new String[4];
            items[0] = mContext.getString(R.string.play);
            items[1] = final_lock ? mContext.getString(R.string.unlock) : mContext.getString(R.string.lock);
            items[2] = mContext.getString(R.string.delete);
            items[3] = mContext.getString(R.string.multi_select);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MediaListItem item = mMediaListNew.get(final_pos);
                if (mIsPhoto) {
                    switch (which) {
                    case 0: openPhoto(mContext, item.mi_path, item.mi_name); break;
                    case 1: MediaManager.getInstance(mContext).delImage(item.mi_path); break;
                    case 2: mHandler.sendEmptyMessage(BrowserActivity.MSG_ENABLE_MULTI_SELECT); break;
                    }
                } else {
                    switch (which) {
                    case 0: playVideo(mContext, item.mi_path, item.mi_name); break;
                    case 1: MediaManager.getInstance(mContext).setVideoLockType(item.mi_path, !final_lock); break;
                    case 2: MediaManager.getInstance(mContext).delVideo(item.mi_path); break;
                    case 3: mHandler.sendEmptyMessage(BrowserActivity.MSG_ENABLE_MULTI_SELECT); break;
                    }
                }
            }
        });
        builder.create().show();
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
        boolean mi_selected;
        Bitmap  mi_bitmap;
        int     mi_thumb;
        String  mi_path;
        String  mi_name;
        String  mi_detail1;
        String  mi_detail2;
        String  mi_size;
    }

    class ViewHolder {
        ImageView  mi_image;
        TextView   mi_name;
        TextView   mi_detail1;
        TextView   mi_detail2;
        TextView   mi_size;
    }
}
