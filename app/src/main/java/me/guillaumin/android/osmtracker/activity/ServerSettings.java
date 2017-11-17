package me.guillaumin.android.osmtracker.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import me.guillaumin.android.osmtracker.R;

/**
 * Created by emmanuel on 10/11/17.
 */

public class ServerSettings extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.server_settings);
    }
}
