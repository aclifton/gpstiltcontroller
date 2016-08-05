package com.andrewrobertclifton.tiltcontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by user on 8/4/16.
 */
public class SettingsActivity extends PreferenceActivity {
    private static final String PREFERENCE_RUNNING = "running";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean running = sharedPreferences.getBoolean(PREFERENCE_RUNNING, false);
        if (running) {
            TiltService.startService(this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
