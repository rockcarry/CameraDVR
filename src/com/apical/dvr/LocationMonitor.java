/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;


/**
 * A class that handles everything about location.
 */
public class LocationMonitor {
    private static final String TAG = "LocationMonitor";

    private Context         mContext;
    private Listener        mListener;
    private LocationManager mLocationManager;
    private Location        mLastLocation;
    private int             mUsedInFix;
    private float           mSpeed = -1;

    public interface Listener {
        public void onGpsSpeedChanged(float speed);
    }

    public LocationMonitor(Context context, Listener listener) {
        mContext  = context;
        mListener = listener;
    }

    public Location getCurrentLocation() {
        return mLastLocation;
    }

    public float getCurrentSpeed() {
        return mSpeed;
    }

    public void recordLocation(boolean recordLocation) {
        if (recordLocation) {
            startReceivingLocationUpdates();
        } else {
            stopReceivingLocationUpdates();
        }
    }

    private void startReceivingLocationUpdates() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        if (mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000, 1, mLocationListener);
                mLocationManager.addGpsStatusListener(mGpsStatusListener);
            } catch (SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            }
            Log.d(TAG, "startReceivingLocationUpdates");

            mLastLocation = new Location(LocationManager.GPS_PROVIDER);
        }
    }

    private void stopReceivingLocationUpdates() {
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
            Log.d(TAG, "stopReceivingLocationUpdates");
        }
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            switch (arg1) {
            case LocationProvider.AVAILABLE:
                updateGpsSpeed(-1);
                break;
            case LocationProvider.OUT_OF_SERVICE:
                updateGpsSpeed(-1);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                updateGpsSpeed(-1);
                break;
            }
        }

        @Override
        public void onProviderEnabled(String arg0) {
        }

        @Override
        public void onProviderDisabled(String arg0) {
            updateGpsSpeed(-1);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location != null && mUsedInFix >= 3) {
                updateGpsSpeed(location.getSpeed());
                mLastLocation.set(location);
            } else {
                updateGpsSpeed(-1);
            }
        }
    };

    GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int arg0) {
            switch (arg0) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                int usedinfix = 0;

                if (gpsStatus != null) {
                    Iterator<GpsSatellite> satellites = gpsStatus
                            .getSatellites().iterator();

                    if (satellites != null) {
                        while (satellites.hasNext()) {
                            GpsSatellite satellite = satellites.next();

                            if (satellite.usedInFix()) {
                                usedinfix++;
                            }
                        }
                    }
                }
                mUsedInFix = usedinfix;
                if (mUsedInFix < 3) {
                    updateGpsSpeed(-1);
                }
                break;

            case GpsStatus.GPS_EVENT_STARTED:
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                break;
            }
        }
    };

    private void updateGpsSpeed(float speed) {
        if (mListener != null) {
            mListener.onGpsSpeedChanged(speed);
        }
        mSpeed = speed;
    }
}
