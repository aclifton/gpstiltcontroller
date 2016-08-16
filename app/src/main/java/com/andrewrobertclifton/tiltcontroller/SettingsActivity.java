package com.andrewrobertclifton.tiltcontroller;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by user on 8/4/16.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFERENCE_RUNNING = "running";
    public static final String PREFERENCE_MAX_MOVE = "max_move";
    public static final String PREFERENCE_TILT_THRESHOLD = "tilt_threshold";

    private static final int PERMISSION_REQUEST = 87879;

    private SharedPreferences sharedPreferences;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREFERENCE_RUNNING:
                if (sharedPreferences.getBoolean(key, false)) {
                    TiltService.startService(SettingsActivity.this);
                } else {
                    TiltService.stopService(SettingsActivity.this);
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
            return;
        }
        boolean running = sharedPreferences.getBoolean(PREFERENCE_RUNNING, false);
        if (running) {
            TiltService.startService(this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public static float getFloatPreference(SharedPreferences sharedPreferences, String key, float defaultValue) {
        try {
            return Float.parseFloat(sharedPreferences.getString(key, Float.toString(defaultValue)));
        } catch (ClassCastException e) {
            sharedPreferences.edit().putFloat(key, defaultValue);
            return defaultValue;
        }
    }
}
