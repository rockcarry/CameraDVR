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
    private TextView mVideoQualityText;

    private LinearLayout mVideoDuration;
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
    private Context       mContext;
    private RecordService mRecServ;
    //

    public SettingsDialog(Context context, RecordService service) {
        super(context, R.style.SettingsDialog);
        mContext = context;
        mRecServ = service;
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

        { // video quality
            int[] ids   = {R.string.video_quality_720p, R.string.video_quality_1080p};
            int   state = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);
            mVideoQualityText.setText(ids[state]);
        }

        { // video duration
            int[] ids      = {R.string.video_duration_1min, R.string.video_duration_2min, R.string.video_duration_5min};
            int   duration = Settings.get(Settings.KEY_RECORD_DURATION, Settings.DEF_RECORD_DURATION);
            int   state    = duration == 60000 ? 0 : duration == 120000 ? 1 : 2;
            mVideoDurationText.setText(ids[state]);
        }

        { // impact detect level
            int[] ids   = {R.string.impact_leve_1, R.string.impact_leve_2, R.string.impact_leve_3, R.string.impact_leve_c};
            int   level = Settings.get(Settings.KEY_IMPACT_DETECT_LEVEL, Settings.DEF_IMPACT_DETECT_LEVEL);
            mImpactDetectText.setText(ids[level]);
        }
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
                {
                    SettingsAlertDlg dlg = SettingsAlertDlg.getInstance(mContext);
                    if (dlg == null) return;
                    int quality = Settings.get(Settings.KEY_IMPACT_DETECT_LEVEL, Settings.DEF_IMPACT_DETECT_LEVEL);
                    dlg.setTitle(R.string.impact_detect_level);
                    dlg.addItem(R.string.impact_leve_1, quality== 0, false);
                    dlg.addItem(R.string.impact_leve_2, quality== 1, false);
                    dlg.addItem(R.string.impact_leve_3, quality== 2, false);
                    dlg.addItem(R.string.impact_leve_c, quality== 3, true );
                    dlg.setCallback(new SettingsAlertDlg.DialogListener() {
                        @Override
                        public void onClick(int state) {
                            int[] ids = {R.string.impact_leve_1, R.string.impact_leve_2, R.string.impact_leve_3, R.string.impact_leve_c};
                            mRecServ.setImpactDetectLevel(state);
                            Settings.set(Settings.KEY_IMPACT_DETECT_LEVEL, state);
                            mImpactDetectText.setText(ids[state]);
                        }
                    });
                }
                break;
            case R.id.video_quality:
                {
                    SettingsAlertDlg dlg = SettingsAlertDlg.getInstance(mContext);
                    if (dlg == null) return;
                    int quality = Settings.get(Settings.KEY_VIDEO_QUALITY, Settings.DEF_VIDEO_QUALITY);
                    dlg.setTitle(R.string.video_quality);
                    dlg.addItem(R.string.video_quality_720p , quality == 0, false);
                    dlg.addItem(R.string.video_quality_1080p, quality == 1, true );
                    dlg.setCallback(new SettingsAlertDlg.DialogListener() {
                        @Override
                        public void onClick(int state) {
                            int[] ids = {R.string.video_quality_720p, R.string.video_quality_1080p};
                            mRecServ.setCamMainVideoQuality(state);
                            Settings.set(Settings.KEY_VIDEO_QUALITY, state);
                            mVideoQualityText.setText(ids[state]);
                        }
                    });
                }
                break;
            case R.id.video_duration:
                {
                    SettingsAlertDlg dlg = SettingsAlertDlg.getInstance(mContext);
                    if (dlg == null) return;
                    int duration = Settings.get(Settings.KEY_RECORD_DURATION, Settings.DEF_RECORD_DURATION);
                    dlg.setTitle(R.string.video_duration);
                    dlg.addItem(R.string.video_duration_1min, duration == 60000 , false);
                    dlg.addItem(R.string.video_duration_2min, duration == 120000, false);
                    dlg.addItem(R.string.video_duration_5min, duration == 300000, true );
                    dlg.setCallback(new SettingsAlertDlg.DialogListener() {
                        @Override
                        public void onClick(int state) {
                            int[] ids = {R.string.video_duration_1min, R.string.video_duration_2min, R.string.video_duration_5min};
                            int   duration = state == 0 ? 60000 : state == 1 ? 120000 : 300000;
                            mRecServ.setRecordingMaxDuration(duration);
                            Settings.set(Settings.KEY_RECORD_DURATION, duration);
                            mVideoDurationText.setText(ids[state]);
                        }
                    });
                }
                break;
            case R.id.format_sd:
                {
                    SettingsAlertDlg dlg = SettingsAlertDlg.getInstance(mContext);
                    if (dlg == null) return;
                    dlg.setTitle(R.string.format_sd_title);
                    dlg.setMessage(R.string.format_sd_message);
                    dlg.setCallback(new SettingsAlertDlg.DialogListener() {
                        @Override
                        public void onClick(int state) {
                            SdcardManager.formatSDcard(mContext);
                            SdcardManager.makeDvrDirs();
                        }
                    });
                    dlg.setButtons();
                }
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


