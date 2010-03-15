package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;

/**
 * Manages preferences screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Set summary of some preferences to their actual values
		// and register a change listener to set again the summary in case of change
		
		// External storage directory
		Preference pref = findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
		pref.setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(OSMTracker.Preferences.KEY_STORAGE_DIR, OSMTracker.Preferences.VAL_STORAGE_DIR));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the directory value
				preference.setSummary((String) newValue);
				return true;
			}
		});

		// Voice record duration
		pref = findPreference(OSMTracker.Preferences.KEY_VOICEREC_DURATION);
		pref.setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(OSMTracker.Preferences.KEY_VOICEREC_DURATION, OSMTracker.Preferences.VAL_VOICEREC_DURATION) + " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the number of seconds, following by "s"
				preference.setSummary(newValue + " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
				return true;
			}
		});
		
		pref = findPreference(OSMTracker.Preferences.KEY_GPS_OSSETTINGS);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				return true;
			}
		});
		
	}
	
	@Override
	protected void onResume() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		super.onPause();
	}
	
}
