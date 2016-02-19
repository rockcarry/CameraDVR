package com.apical.cdr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GSensorMonitor {
    private static final String TAG = "GSensorMonitor";
    private static final float IMPACT_AA_LEVEL = 256;

    private SensorManager mSensorManager;
    private Context  mContext;
    private Listener mListener;

    public interface Listener {
        public void onGsensorImpactEvent();
    }

    public GSensorMonitor(Context context, Listener listener) {
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

    private SensorEventListener mGSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                float aa= x * x + y * y + z * z;
//              Log.d(TAG, "x = " + x + ", y = " + y + ", z = " + z + ", aa = " + aa);
                if (aa > IMPACT_AA_LEVEL) {
                    if (mListener != null) {
                        mListener.onGsensorImpactEvent();
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor , int accuracy) {}
    };
}
