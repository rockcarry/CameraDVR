package com.apical.dvr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;

import com.apical.dvr.R;


public class SettingsDialog extends Dialog {
    private String TAG = "SettingsDialog";

    private ImageView mDismiss;

    private TextView mImpactDetectText;
    private int mImpactDetectState;

    private LinearLayout mVideoQuality;
    private int mVideoQualityState;
    private TextView mVideoQualityText;

    private LinearLayout mVideoDuration;
    private int mVideoDurationState;
    private TextView mVideoDurationText;

    private ImageView mFlipSwitcher;
    private ImageView mPowerOnRecord;
    private ImageView mWatermark;

    private LinearLayout mFormatSD;
    private LinearLayout mRestore;

    //++ for adas
    private TextView mDistanceDetectText;
    private int mDistanceDetectState;
    //-- for adas

    //
    private Context mContext;
    //

    public SettingsDialog(Context context, int theme) {
        super(context, theme);
        mContext = context;
    }

    public SettingsDialog(Context context) {
        super(context, R.style.SettingsDialog);
        mContext = context;
        setContentView(R.layout.settings_dialog);

        widgetInit();    
    }

    private void widgetInit() {
        // dismiss
        mDismiss = (ImageView)findViewById(R.id.settings_back);
        mDismiss.setOnClickListener(dismissClickListener);

        // impact
        mImpactDetectText = (TextView)findViewById(R.id.impact_detect_level_text);

        // video quality
        mVideoQuality = (LinearLayout)findViewById(R.id.video_quality);
        mVideoQuality.setOnClickListener(settingsClickListener);
        mVideoQualityText = (TextView)findViewById(R.id.video_quality_text);

        // video duration
        mVideoDuration = (LinearLayout)findViewById(R.id.video_duration);
        mVideoDuration.setOnClickListener(settingsClickListener);
        mVideoDurationText = (TextView)findViewById(R.id.video_duration_text);

        // camera flip
        mFlipSwitcher = (ImageView)findViewById(R.id.flip_switcher);
        mFlipSwitcher.setOnClickListener(settingsCheckedChangeListener);

        mPowerOnRecord = (ImageView)findViewById(R.id.poweron_recording_switch);
        mPowerOnRecord.setOnClickListener(settingsCheckedChangeListener);

        mWatermark = (ImageView)findViewById(R.id.watermark_switch);
        mWatermark.setOnClickListener(settingsCheckedChangeListener);

        mFormatSD = (LinearLayout)findViewById(R.id.format_sd);
        mFormatSD.setOnClickListener(settingsClickListener);

        mRestore = (LinearLayout)findViewById(R.id.restore);
        mRestore.setOnClickListener(settingsClickListener);

        // impact detect level setting
        findViewById(R.id.impact_detect_level).setOnClickListener(settingsClickListener);

        mDistanceDetectText = (TextView)findViewById(R.id.distance_detect_level_text);
        findViewById(R.id.distance_detect_level).setOnClickListener(settingsClickListener);
    }

    protected View.OnClickListener dismissClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {  
            dismiss();
        }
    };

    protected View.OnClickListener settingsCheckedChangeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isChecked = !v.isSelected();
            switch (v.getId()) {
            case R.id.poweron_recording:
                break;
            case R.id.watermark_switch:
                break;
            case R.id.flip_switcher:
                break;
            }
            v.setSelected(isChecked);
        }
    };

    protected View.OnClickListener settingsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.impact_detect_level:
                break;
            case R.id.video_quality:
                break;
            case R.id.video_duration:
                break;
            case R.id.format_sd:
                break;
            case R.id.restore:
                break;
            //++ for adas
            case R.id.distance_detect_level:
                break;
            //-- for adas
            }
        }
    };
}


