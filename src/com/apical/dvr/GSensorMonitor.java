package com.apical.dvr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

public class GSensorMonitor {
    private static final String TAG = "GSensorMonitor";

    private SensorManager  mSensorManager = null;
    private Context        mContext       = null;
    private int            mImpactLevel   = 1;
    private ImpactEventListener mListener = null;

    public interface ImpactEventListener {
        public void onGsensorImpactEvent(long time);
    }

    public GSensorMonitor(Context context, ImpactEventListener listener) {
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mContext = context;
        mListener = listener;
    }

    public void start() {
        mSensorManager.registerListener(mGSensorListener,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        mSensorManager.unregisterListener(mGSensorListener);
    }

    public void setImpactDetectLevel(int l) {
        mImpactLevel = l;
    }

    private SensorEventListener mGSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                float aa= x * x + y * y + z * z;
//              Log.d(TAG, "x = " + x + ", y = " + y + ", z = " + z + ", aa = " + aa);
                //++ impact level
                int level;
                switch (mImpactLevel) {
                case 0: level = 256; break;
                case 1: level = 276; break;
                case 2: level = 296; break;
                case 3: level = 0x7fffffff; break;
                default:level = 276; break;
                }
                //-- impact level
                if (aa > level) {
                    if (mListener != null) {
                        mListener.onGsensorImpactEvent(SystemClock.uptimeMillis());
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
}
