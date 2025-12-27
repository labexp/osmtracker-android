package net.osmtracker.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import java.io.File;

/**
 * Manages preferences screen
 */
public class Preferences extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
			Preference preference;

			// Voice record duration: set a custom SummaryProvider
			preference = findPreference(OSMTracker.Preferences.KEY_VOICEREC_DURATION);
			preference.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
				@Override
				public CharSequence provideSummary(@NonNull ListPreference preference) {
					String currentValue = preference.getEntry().toString();
					// Return your combined string
					return currentValue+ " "
							+ getResources().getString(R.string.prefs_voicerec_duration_seconds);
				}
			});

			// Clear OSM data: Disable if there's no OSM data stored
			preference = findPreference(OSMTracker.Preferences.KEY_OSM_OAUTH_CLEAR_DATA);
			preference.setEnabled(prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN));
			// Set a Click Listener to show the confirmation dialog
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(@NonNull Preference preference) {
					new androidx.appcompat.app.AlertDialog.Builder(requireContext())
							.setTitle(preference.getTitle())
							.setMessage(R.string.prefs_osm_clear_oauth_data_dialog)
							.setIcon(preference.getIcon())
							.setPositiveButton(android.R.string.ok, (dialog, which) -> {
								// User clicked OK: Clear the data
								SharedPreferences.Editor editor = prefs.edit();
								editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN);
								editor.apply();

								// Disable the button now that data is gone
								preference.setEnabled(false);
							})
							.setNegativeButton(android.R.string.cancel, null)
							.show();
					return true;
				}
			});


			// GPS Settings

			preference = findPreference(OSMTracker.Preferences.KEY_GPS_OSSETTINGS);
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(@NonNull Preference preference) {
					startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					return true;
				}
			});

			// Update GPS logging interval summary to the current value
			preference = findPreference(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL);
			preference.setSummary(
					prefs.getString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)
							+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
							+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
			preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
					String val = (String) newValue;
					// don't allow the logging_min_distance to be empty
					if (TextUtils.isEmpty(val)) {
						Toast.makeText(requireContext(), R.string.prefs_gps_logging_interval_empty, Toast.LENGTH_SHORT).show();
						return false; // Don't save
					} else {
						// Set summary with the interval and "seconds"
						preference.setSummary(newValue
								+ " " + getResources().getString(R.string.prefs_gps_logging_interval_seconds)
								+ ". " + getResources().getString(R.string.prefs_gps_logging_interval_summary));
						return true;
					}
				}
			});

			// Update GPS min. distance summary to the current value
			preference = findPreference(OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE);
			preference.setSummary(
					prefs.getString(OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, OSMTracker.Preferences.VAL_GPS_LOGGING_MIN_DISTANCE)
							+ " " + getResources().getString(R.string.prefs_gps_logging_min_distance_meters)
							+ ". " + getResources().getString(R.string.prefs_gps_logging_min_distance_summary));
			preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
					// Set summary with the interval and "seconds"
					preference.setSummary(newValue
							+ " " + getResources().getString(R.string.prefs_gps_logging_min_distance_meters)
							+ ". " + getResources().getString(R.string.prefs_gps_logging_min_distance_summary));
					return true;
				}
			});



			//GPX Settings

			// External storage directory
			EditTextPreference storageDirPref = (EditTextPreference) findPreference(OSMTracker.Preferences.KEY_STORAGE_DIR);
			storageDirPref.setSummary(prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR, OSMTracker.Preferences.VAL_STORAGE_DIR));
			storageDirPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
					// Ensure there is always a leading slash
					if (! ((String) newValue).startsWith(File.separator)) {
						newValue = File.separator + (String) newValue;
					}

					// Set summary with the directory value
					preference.setSummary((String) newValue);

					return true;
				}});

			// Explicit execution of buttons presets window
			preference = findPreference(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT);
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				@Override
				public boolean onPreferenceClick(@NonNull Preference preference) {
					startActivity(new Intent(requireContext(), ButtonsPresets.class));
					return true;
				}

			});

			// Button screen orientation option
			preference = findPreference(OSMTracker.Preferences.KEY_UI_ORIENTATION);
			ListPreference orientationListPreference = (ListPreference) preference;
			String displayValueKey = prefs.getString(OSMTracker.Preferences.KEY_UI_ORIENTATION, OSMTracker.Preferences.VAL_UI_ORIENTATION);
			int displayValueIndex = orientationListPreference.findIndexOfValue(displayValueKey);
			String displayValue = orientationListPreference.getEntries()[displayValueIndex].toString();
			orientationListPreference.setSummary(displayValue + ".\n"
					+ getResources().getString(R.string.prefs_ui_orientation_summary));
			// Set a listener to update the preference display after a change is made
			preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
					// Set summary with the display text of the item and a description of the preference
					ListPreference orientationListPreference = (ListPreference) preference;
					// Pull the display string from the list preference rather than simply using the key value
					int newValueIndex = orientationListPreference.findIndexOfValue((String)newValue);
					String newPreferenceDisplayValue = orientationListPreference.getEntries()[newValueIndex].toString();

					preference.setSummary(newPreferenceDisplayValue
							+ ".\n" + getResources().getString(R.string.prefs_ui_orientation_summary));
					return true;
			}});

		}
	}
}