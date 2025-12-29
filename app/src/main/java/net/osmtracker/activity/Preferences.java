package net.osmtracker.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

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
import java.util.Objects;

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
			SharedPreferences prefs =
					PreferenceManager.getDefaultSharedPreferences(requireContext());

			setupVoiceRecDuration();
			setupOSMAuthClearData(prefs);


			// GPS Settings
			setupGPSOSSettings();
			// GPSLogging Interval
			setupEditTextNum(
					OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL,
					getString(R.string.prefs_gps_logging_interval_seconds),
					getString(R.string.prefs_gps_logging_interval_summary),
					getString(R.string.prefs_gps_logging_interval_empty)
			);
			//GPS Logging Min Distance
			setupEditTextNum(
					OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE,
					getString(R.string.prefs_gps_logging_min_distance_meters),
					getString(R.string.prefs_gps_logging_min_distance_summary),
					getString(R.string.prefs_gps_logging_min_distance_empty)
			);


			//GPX Settings
			setupStorageDirectory();
			setupButtonsLayout();
			setupButtonScreenOrientation();

		}

		/**
		 * Explicit execution of buttons presets window
		 */
		private void setupButtonsLayout() {

			Preference UIButtonsLayout = findPreference(
					OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT);
			if (UIButtonsLayout == null) return;
			UIButtonsLayout.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent(requireContext(), ButtonsPresets.class));
				return true;
			});
		}

		/**
		 *
		 */
		private void setupStorageDirectory() {
			// External storage directory
			EditTextPreference storageDirPref = findPreference(
					OSMTracker.Preferences.KEY_STORAGE_DIR);

			if (storageDirPref == null) return;

			// Set summary provider
			storageDirPref.setSummaryProvider(preference -> {
				String val = storageDirPref.getText();
				if (TextUtils.isEmpty(val)) {
					return OSMTracker.Preferences.VAL_STORAGE_DIR;
				}
				return val;
			});

			// Enforce the leading slash
			storageDirPref.setOnPreferenceChangeListener((preference, newValue) -> {
				String val = newValue.toString().trim();
				// Empty
				if (TextUtils.isEmpty(val)) {
					Toast.makeText(requireContext(),
							R.string.prefs_storage_dir_empty,
							Toast.LENGTH_SHORT).show();
					return false;
				}

				// Ensure there is always a leading slash
				if (!val.startsWith(File.separator)) {
					String fixedVal = File.separator + val;
					((EditTextPreference) preference).setText(fixedVal);
					return false; //ignores the user input
				}

				return true;
			});
		}

		/**
		 * Open Android GPS Settings screen
		 */
		private void setupGPSOSSettings() {
			Preference GPSOSSettings = findPreference(OSMTracker.Preferences.KEY_GPS_OSSETTINGS);

			if (GPSOSSettings == null) return;

			GPSOSSettings.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				return true;
			});
		}

		/**
		 * Voice record duration: set a custom SummaryProvider
		 */
		private void setupVoiceRecDuration() {
			Preference voiceRec = findPreference(OSMTracker.Preferences.KEY_VOICEREC_DURATION);

			if (voiceRec == null) return;

			voiceRec.setSummaryProvider(
					(Preference.SummaryProvider<ListPreference>) preference -> {
						// Return your combined string
						return preference.getEntry() + " "
								+ getString(R.string.prefs_voicerec_duration_seconds);
					});
		}

		/**
		 * Clear OSM data: Disable if there's no OSM data stored
		 *
		 * @param prefs SharedPreferences
		 */
		private void setupOSMAuthClearData(SharedPreferences prefs) {

			Preference OSMAuthClearData = findPreference(
					OSMTracker.Preferences.KEY_OSM_OAUTH_CLEAR_DATA);

			if (OSMAuthClearData == null) return;

			String tokenKey = OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN;
			OSMAuthClearData.setEnabled(prefs.contains(tokenKey));
			// Set a Click Listener to show the confirmation dialog
			OSMAuthClearData.setOnPreferenceClickListener(preference -> {
				new androidx.appcompat.app.AlertDialog.Builder(requireContext())
						.setTitle(preference.getTitle())
						.setMessage(R.string.prefs_osm_clear_oauth_data_dialog)
						.setIcon(preference.getIcon())
						.setPositiveButton(android.R.string.ok, (dialog, which) -> {
							// User clicked OK: Clear the data
							prefs.edit().remove(tokenKey).apply();
							// Disable the button now that data is gone
							preference.setEnabled(false);
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
			});

		}

		/**
		 *
		 * @param preferenceKey   from OSMTracker.Preferences
		 * @param valueSuffix     appended to the end of the value, shown in the summary
		 * @param summary         static summary to be appended to the end of the summary
		 * @param validationError in case of empty value
		 */
		private void setupEditTextNum(String preferenceKey, String valueSuffix, String summary,
									  String validationError) {
			EditTextPreference numInputPref = findPreference(preferenceKey);
			if (numInputPref == null) return;

			// Set input type to number and move cursor to the end
			numInputPref.setOnBindEditTextListener(editText -> {
				editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
				editText.setSelection(editText.getText().length());
			});

			// Set summary provider
			numInputPref.setSummaryProvider(preference -> {
				EditTextPreference editTextPreference = (EditTextPreference) preference;
				return editTextPreference.getText() + " " + valueSuffix + ". " + summary;
			});

			numInputPref.setOnPreferenceChangeListener((preference, newValue) -> {
				String val = (String) newValue;
				if (TextUtils.isEmpty(val)) {
					Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show();
					return false;
				}
				return true;
			});
		}

		/**
		 * Button screen orientation option
		 */
		private void setupButtonScreenOrientation() {
			ListPreference uiOrientationPref = findPreference(
					OSMTracker.Preferences.KEY_UI_ORIENTATION);

			if (uiOrientationPref == null) return;

			String summary = getString(R.string.prefs_ui_orientation_summary);

			uiOrientationPref.setSummaryProvider(preference -> {
				ListPreference listPref = (ListPreference) preference;
				CharSequence entry = listPref.getEntry();
				// Null check: entry might be null if no value is selected
				String displayValue = Objects.requireNonNull(entry).toString();
				return displayValue + ".\n" + summary;
			});
		}

	}
}