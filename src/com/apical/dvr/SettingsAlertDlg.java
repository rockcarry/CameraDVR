package com.apical.dvr;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apical.dvr.R;

class SettingsAlertDlg {
    private String TAG = "SettingsAlertDlg";

    Context mContext;
    AlertDialog mDialog;

    DialogListener listener;

    TextView title;
    View titleParting;

    TextView msg;
    View msgParting;

    LinearLayout items;
    List<LinearLayout> itemList = new ArrayList<LinearLayout>();

    LinearLayout buttons;
    TextView pos;
    TextView neg;
    TextView last;

    public static SettingsAlertDlg dialogInstance = null;
    public static int no = 0;

    public interface DialogListener {
        public void onClick(int state);
    }

    private SettingsAlertDlg(Context c) {
        mContext = c;

        no++;

        mDialog = new AlertDialog.Builder(c).create();
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogInstance = null;
            }
        });

        mDialog.show();

        Window window = mDialog.getWindow();
        window.setContentView(R.layout.alert_dialog);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = 472;
        window.setAttributes(params);

        title = (TextView)window.findViewById(R.id.alert_dialog_title);
        titleParting = (View)window.findViewById(R.id.alert_dialog_title_parting_line);

        msg = (TextView)window.findViewById(R.id.alert_dialog_message);
        msgParting = (View)window.findViewById(R.id.alert_dialog_message_parting_line);

        items = (LinearLayout)window.findViewById(R.id.alert_dialog_items);

        buttons = (LinearLayout)window.findViewById(R.id.alert_dialog_buttons);
        pos  = (TextView)window.findViewById(R.id.alert_dialog_pos);
        neg  = (TextView)window.findViewById(R.id.alert_dialog_neg);
        last = (TextView)window.findViewById(R.id.alert_dialog_last);
    }

    public static SettingsAlertDlg getInstance(Context c) {
        if (dialogInstance == null) {
            dialogInstance = new SettingsAlertDlg(c);
            return dialogInstance;
        }
        else {
            return null;
        }
    }

    public void setTitle(int titleid) {
        title.setText(titleid);
        title.setVisibility(View.VISIBLE);
        titleParting.setVisibility(View.VISIBLE);
    }

    public void setMessage(int msgid) {
        msg.setText(msgid);
        msg.setVisibility(View.VISIBLE);
        msgParting.setVisibility(View.VISIBLE);
    }

    public void setCallback(DialogListener l) {
        listener = l;
    }

    public void addItem(int itemid, boolean isSelected, boolean isLast) {
        LinearLayout view = new LinearLayout(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 64);
        view.setLayoutParams(lp);
        view.setOrientation(LinearLayout.HORIZONTAL);
        if (!isLast)
            view.setBackgroundResource(R.drawable.alert_dialog_option_mid);
        else
            view.setBackgroundResource(R.drawable.alert_dialog_option_down);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        leftParams.gravity = Gravity.CENTER;
        leftParams.setMargins(26, 0, 0, 0);
        TextView tv = new TextView(mContext);
        tv.setLayoutParams(leftParams);
        tv.setGravity(Gravity.LEFT);
        tv.setText(itemid);
        tv.setTextSize(28);
        tv.setTextColor(mContext.getResources().getColorStateList(R.color.text_gray));
        view.addView(tv);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rightParams.gravity = Gravity.CENTER;
        rightParams.setMargins(0, 0, 26, 0);
        ImageView iv = new ImageView(mContext);
        iv.setLayoutParams(rightParams);
        iv.setImageResource(R.drawable.item_choice);
        view.addView(iv);

        view.setOnTouchListener(new android.view.View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    int state = 0;
                    int i = 0;
                    int cnt = 0;
                    for (LinearLayout view : itemList) {
                        cnt = view.getChildCount();
                        for (int j = 0; j < cnt; j++) {
                            view.getChildAt(j).setSelected(false);
                        }
                        view.setSelected(false);
                        if (view == v)
                            state = i;
                        i++;
                    }
                    listener.onClick(state);
                    v.setSelected(true);
                    mDialog.dismiss();
                    break;

                case MotionEvent.ACTION_UP:
                    if (mDialog != null) {
                        Log.i(TAG, "UP dismiss over");
                    }
                    break;
                }
                return true;
            }
        });

        if (isSelected)
            iv.setSelected(true);

        itemList.add(view);
        items.addView(view);
        if (!isLast) {
            View partingLine = new View(mContext);
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
            vlp.setMargins(8, 0, 8, 0);
            partingLine.setLayoutParams(vlp);
            partingLine.setBackgroundResource(R.color.parting_line_dark);
            items.addView(partingLine);
        }
    }

    public void setButtons() {
        buttons.setVisibility(View.VISIBLE);
        pos.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(0);
                if (mDialog != null ) {
                    mDialog.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });

        neg.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialog != null ) {
                    mDialog.dismiss();
                    Log.i(TAG, "UP dismiss over");
                }
            }
        });
    }

    public void setLast(int lastid) {
        last.setText(lastid);
        last.setVisibility(View.VISIBLE);
    }
}

