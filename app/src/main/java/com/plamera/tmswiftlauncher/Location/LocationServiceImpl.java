package com.plamera.tmswiftlauncher.Location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.google.gson.Gson;
import com.plamera.tmswiftlauncher.R;

public class LocationServiceImpl extends Service implements LocationListener, LocationService {

    private Location location;
    private LocationManager locationManager;
    private static LocationServiceImpl serviceInstance;
    private Context context;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60;
    private static final String TAG = LocationServiceImpl.class.getSimpleName();

    private LocationServiceImpl(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        class LooperThread extends Thread {
            public Handler mHandler;
            public void run() {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
                LocationServiceImpl.this.setLocation();
                mHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        Log.e(TAG, "No network/GPS Switched off." + msg);
                    }
                };
            }
        }
        new LooperThread().run();
    }

    public static LocationServiceImpl getInstance(Context context) {
        if (serviceInstance == null) {
            synchronized (LocationServiceImpl.class) {
                if (serviceInstance == null) {
                    serviceInstance = new LocationServiceImpl(context);
                }
            }
        }
        return serviceInstance;
    }

    /**
     * In this method, it gets the latest location updates from gps/ network.
     */
    private void setLocation() {
        if (locationManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= 23 &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        Preference.putString(context, context.getResources().getString(R.string.shared_pref_location),
                                new Gson().toJson(location));
                    }
                }

                if (location == null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this);
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            Preference.putString(context, context.getResources().getString(R.string.shared_pref_location),
                                    new Gson().toJson(location));
                        }
                    }
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "No network/GPS Switched off.", e);
            }
        }
    }

    public Location getLastKnownLocation() {
        return location;
    }

    @Override
    public Location getLocation() {
        if (location == null) {
            location = new Gson().fromJson(Preference.getString(context, context.getResources().getString(
                    R.string.shared_pref_location)), Location.class);
        }
        return location;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Preference.putString(context, context.getResources().getString(R.string.shared_pref_location),
                    new Gson().toJson(location));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        serviceInstance = null;
        serviceInstance = getInstance(context);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
