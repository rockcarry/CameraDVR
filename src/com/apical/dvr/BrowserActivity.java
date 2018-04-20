package com.apical.dvr;

import android.app.TabActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.util.Log;

public class BrowserActivity extends TabActivity
{
    private static final String TAG = "BrowserActivity";

    private TabHost mTabHost = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        mTabHost = getTabHost();
        if (false) {
            String[] tab_names = getResources().getStringArray(R.array.category_two_camera);
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[0]).setIndicator(tab_names[0], null).setContent(R.id.tab_all_video_a));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[1]).setIndicator(tab_names[1], null).setContent(R.id.tab_lock_video_a));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[2]).setIndicator(tab_names[2], null).setContent(R.id.tab_photo_a));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[3]).setIndicator(tab_names[3], null).setContent(R.id.tab_all_video_b));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[4]).setIndicator(tab_names[4], null).setContent(R.id.tab_lock_video_b));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[5]).setIndicator(tab_names[5], null).setContent(R.id.tab_photo_b));
        } else {
            String[] tab_names = getResources().getStringArray(R.array.category_one_camera);
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[0]).setIndicator(tab_names[0], null).setContent(R.id.tab_all_video_a));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[1]).setIndicator(tab_names[1], null).setContent(R.id.tab_lock_video_a));
            mTabHost.addTab(mTabHost.newTabSpec(tab_names[2]).setIndicator(tab_names[2], null).setContent(R.id.tab_photo_a));
        }

        TabWidget tw = getTabWidget();
        for (int i=0; i<tw.getChildCount(); i++) {
            TextView tv = (TextView)tw.getChildAt(i).findViewById(android.R.id.title);
            tv.setPadding(2, 5, 2, 5);
            tv.setTextSize(20);
            tw.getChildAt(i).getLayoutParams().height = 50;
        }
    }

    @Override
    public void onDestroy() {
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
}




