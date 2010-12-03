package me.guillaumin.android.osmtracker.activity;

import java.io.File;
import java.io.FilenameFilter;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;

/**
 * Manages preferences screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class Preferences extends PreferenceActivity {

	@SuppressWarnings("unused")
	private static final String TAG = Preferences.class.getSimpleName();
	
	/**
	 * Directory containing user layouts, relative to storage dir.
	 */
	public static final String LAYOUTS_SUBDIR = "layouts";
	
	/**
	 * File extension for layout files
	 */
	private static final String LAYOUT_FILE_EXTENSION = ".xml";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Set summary of some preferences to their actual values
		// and register a change listener to set again the summary in case of change
		
		// External storage directory
		EditTextPreference storageDirPref = (EditTextPreference) findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
		storageDirPref.setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(OSMTracker.Preferences.KEY_STORAGE_DIR, OSMTracker.Preferences.VAL_STORAGE_DIR));
		storageDirPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the directory value
				preference.setSummary((String) newValue);
				
				// Re-populate layout list preference
				populateLayoutPreference((String) newValue); 
				
				// Set layout to default layout
				((ListPreference) findPreference(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT)).setValue(OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
				return true;
			}
		});
		populateLayoutPreference(storageDirPref.getText());

		// Voice record duration
		Preference pref = findPreference(OSMTracker.Preferences.KEY_VOICEREC_DURATION);
		pref.setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(OSMTracker.Preferences.KEY_VOICEREC_DURATION, OSMTracker.Preferences.VAL_VOICEREC_DURATION) + " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the number of seconds, following by "seconds"
				preference.setSummary(newValue+ " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
				return true;
			}
		});

		pref = findPreference(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL);
		pref.setSummary(
				PreferenceManager.getDefaultSharedPreferences(this).getString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)
				+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
				+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the interval and "seconds"
				preference.setSummary(newValue
						+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
						+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
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

	/**
	 * Populates the user layout list preference.
	 * @param storageDir Where to find layout files
	 */
	private void populateLayoutPreference(String storageDir) {
		// Populate layout lists reading available layouts from external storage
		ListPreference lf = (ListPreference) findPreference(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT);
		String[] entries;
		String[] values;
		
		// Check for presence of layout directory
		File layoutsDir = new File(Environment.getExternalStorageDirectory().getPath() + storageDir + File.separator + LAYOUTS_SUBDIR + File.separator);
		if (layoutsDir.exists() && layoutsDir.canRead()) {
			// List each layout file
			String[] layoutFiles = layoutsDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(LAYOUT_FILE_EXTENSION);
				}
			});
			// Create array of values for each layout file + the default one
			entries = new String[layoutFiles.length+1];
			values = new String[layoutFiles.length+1];
			entries[0] = getResources().getString(R.string.prefs_ui_buttons_layout_defaut);
			values[0] = OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT;
			for (int i=0; i<layoutFiles.length; i++) {
				entries[i+1] = layoutFiles[i];
				values[i+1] = layoutFiles[i];
			}
		} else {
			// No layout found, populate values with just the default entry.
			entries = new String[] {getResources().getString(R.string.prefs_ui_buttons_layout_defaut)};
			values = new String[] {OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT};
		}
		lf.setEntries(entries);
		lf.setEntryValues(values);
	}
	
	@Override
	protected void onResume() {
		// Tell service to stop notifying user of background activity
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
