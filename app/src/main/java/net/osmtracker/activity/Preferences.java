package net.osmtracker.activity;

import java.io.File;
import java.io.FilenameFilter;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Manages preferences screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class Preferences extends PreferenceActivity {

	@SuppressWarnings("unused")
	private static final String TAG = Preferences.class.getSimpleName();

	final private int RC_READ_PERMISSION = 1;
	
	/**
	 * Directory containing user layouts, relative to storage dir.
	 */
	public static final String LAYOUTS_SUBDIR = "layouts";
	
	/**
	 * File extension for layout files
	 */
	public static final String LAYOUT_FILE_EXTENSION = ".xml";

	/**
	 * The suffix that must be added to the layout's name for getting its icons directory
	 * Example: water_supply       <- layout name
	 *          water_supply_icons <- icon directory
	 */

	public static final String ICONS_DIR_SUFFIX = "_icons";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Set summary of some preferences to their actual values
		// and register a change listener to set again the summary in case of change
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// External storage directory
		EditTextPreference storageDirPref = (EditTextPreference) findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
		storageDirPref.setSummary(prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR, OSMTracker.Preferences.VAL_STORAGE_DIR));
		storageDirPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Ensure there is always a leading slash
				if (! ((String) newValue).startsWith(File.separator)) {
					newValue = File.separator + (String) newValue;
				}
				
				// Set summary with the directory value
				preference.setSummary((String) newValue);
				
				// Re-populate layout list preference
				populateLayoutPreference((String) newValue);
				
				// Set layout to default layout
				((ListPreference) findPreference(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT)).setValue(OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
				return true;
			}
		});

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.READ_EXTERNAL_STORAGE)) {

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				// TODO: explain why we need permission.
				Log.w(TAG, "we should explain why we need read permission");

			} else {

				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RC_READ_PERMISSION);
			}

		}  else {
			populateLayoutPreference(storageDirPref.getText());
		}


		// Voice record duration
		Preference pref = findPreference(OSMTracker.Preferences.KEY_VOICEREC_DURATION);
		pref.setSummary(prefs.getString(OSMTracker.Preferences.KEY_VOICEREC_DURATION, OSMTracker.Preferences.VAL_VOICEREC_DURATION) + " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the number of seconds, following by "seconds"
				preference.setSummary(newValue+ " " + getResources().getString(R.string.prefs_voicerec_duration_seconds));
				return true;
			}
		});
		
		// Update GPS logging interval summary to the current value
		pref = findPreference(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL);
		pref.setSummary(
				prefs.getString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)
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

		// Button screen orientation option
		pref = findPreference(OSMTracker.Preferences.KEY_UI_ORIENTATION);
		ListPreference orientationListPreference = (ListPreference) pref;
		String displayValueKey = prefs.getString(OSMTracker.Preferences.KEY_UI_ORIENTATION, OSMTracker.Preferences.VAL_UI_ORIENTATION);
		int displayValueIndex = orientationListPreference.findIndexOfValue(displayValueKey);
		String displayValue = orientationListPreference.getEntries()[displayValueIndex].toString();
		orientationListPreference.setSummary(displayValue + ".\n" 
				+ getResources().getString(R.string.prefs_ui_orientation_summary));
		
		// Set a listener to update the preference display after a change is made
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Set summary with the display text of the item and a description of the preference
				ListPreference orientationListPreference = (ListPreference)preference;
				// Pull the display string from the list preference rather than simply using the key value
				int newValueIndex = orientationListPreference.findIndexOfValue((String)newValue);
				String newPreferenceDisplayValue = orientationListPreference.getEntries()[newValueIndex].toString();
				
				preference.setSummary(newPreferenceDisplayValue
						+ ".\n" + getResources().getString(R.string.prefs_ui_orientation_summary));
				return true;
			}
		});

		// Clear OSM data: Disable if there's no OSM data stored
		pref = findPreference(OSMTracker.Preferences.KEY_OSM_OAUTH_CLEAR_DATA);
		if (prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN)
				&& prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET)) {
			pref.setEnabled(true);
		} else {
			pref.setEnabled(false);
		}
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Clear data
				Editor editor = prefs.edit();
				editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN);
				editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET);
				editor.commit();
				
				preference.setEnabled(false);
				return false;
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
		File layoutsDir = new File(Environment.getExternalStorageDirectory(), storageDir + File.separator + LAYOUTS_SUBDIR + File.separator);
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
				entries[i+1] = layoutFiles[i].substring(0, layoutFiles[i].length()-LAYOUT_FILE_EXTENSION.length());
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

	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case RC_READ_PERMISSION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					EditTextPreference storageDirPref = (EditTextPreference) findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
					populateLayoutPreference(storageDirPref.getText());

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "we should explain why we need read permission");
				}
			}
		}
	}
	
}
