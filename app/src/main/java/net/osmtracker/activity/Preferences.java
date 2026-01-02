package net.osmtracker.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
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

		private static final String EXTRA_DEFAULT_VALUE = "DEFAULT_VALUE";

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);
			SharedPreferences prefs =
					PreferenceManager.getDefaultSharedPreferences(requireContext());

			// General settings
			setupVoiceRecDuration();
			setupOSMAuthClearData(prefs);

			// GPS Settings
			//Open Android GPS Settings screen
			setupPreferenceNavigation(
					OSMTracker.Preferences.KEY_GPS_OSSETTINGS,
					new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			//GPSLogging Interval
			setupEditTextNum(
					OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL,
					getString(R.string.prefs_gps_logging_interval_seconds),
					getString(R.string.prefs_gps_logging_interval_summary),
					getString(R.string.prefs_gps_logging_interval_empty),
					OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL
			);
			//GPS Logging Min Distance
			setupEditTextNum(
					OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE,
					getString(R.string.prefs_gps_logging_min_distance_meters),
					getString(R.string.prefs_gps_logging_min_distance_summary),
					getString(R.string.prefs_gps_logging_min_distance_empty),
					OSMTracker.Preferences.VAL_GPS_LOGGING_MIN_DISTANCE
			);


			// GPX Settings
			setupStorageDirectory();
			//Filename
			setupListPreference(
					OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
					getString(R.string.prefs_output_filename_summary)
			);
			//Accuracy
			setupListPreference(
					OSMTracker.Preferences.KEY_OUTPUT_ACCURACY,
					getString(R.string.prefs_output_accuracy_summary)
			);
			//Compas Heading
			setupListPreference(
					OSMTracker.Preferences.KEY_OUTPUT_COMPASS,
					getString(R.string.prefs_compass_heading_summary)
			);

			// User Interface Settings
			// Camera
			setupListPreference(
					OSMTracker.Preferences.KEY_UI_PICTURE_SOURCE,
					getString(R.string.prefs_ui_picture_source_summary)
			);
			// App Theme
			setupListPreference(
					OSMTracker.Preferences.KEY_UI_THEME,
					getString(R.string.prefs_theme_summary)
			);
			//Explicit execution of buttons presets window
			setupPreferenceNavigation(
					OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
					new Intent(requireContext(), ButtonsPresets.class));
			//Map tile provider
			setupListPreference(
					OSMTracker.Preferences.KEY_UI_MAP_TILE,
					getString(R.string.prefs_map_tile_summary)
			);
			// Screen Orientation
			setupListPreference(
					OSMTracker.Preferences.KEY_UI_ORIENTATION,
					getString(R.string.prefs_ui_orientation_summary)
			);

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
		 * Setup a preference that launches an activity via Intent
		 * @param preferenceKey The preference key
		 * @param intent The intent to launch
		 */
		private void setupPreferenceNavigation(String preferenceKey, Intent intent) {
			Preference preference = findPreference(preferenceKey);

			if (preference == null) return;

			preference.setOnPreferenceClickListener(p -> {
				startActivity(intent);
				return true;
			});
		}

		/**
		 *
		 * @param preferenceKey   from OSMTracker.Preferences
		 * @param valueSuffix     appended to the end of the value, shown in the summary
		 * @param summary         static summary to be appended to the end of the summary
		 * @param validationError in case of empty value
		 * @param defaultValue	  value to be used for the reset button
		 */
		private void setupEditTextNum(String preferenceKey, String valueSuffix, String summary,
									  String validationError, String defaultValue) {
			EditTextPreference numInputPref = findPreference(preferenceKey);
			if (numInputPref == null) return;

			// Store default value in Extras so it can be retrieved by the Reset Dialog
			numInputPref.getExtras().putString(EXTRA_DEFAULT_VALUE, defaultValue);

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

		@SuppressWarnings("deprecation") // Required to link the dialog to the fragment
		@Override
		public void onDisplayPreferenceDialog(Preference preference) {

			// Retrieve the default value defined in extras.
			// If null, it means this preference doesn't support the reset feature.
			// Fallback to the default dialog behavior.
			String defaultValue = preference.getExtras().getString(EXTRA_DEFAULT_VALUE);
			if (defaultValue == null) {
				super.onDisplayPreferenceDialog(preference);
				return;
			}

			// Create the standard dialog fragment
			final EditTextPreferenceDialogFragmentCompat dialogFragment =
					EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
			dialogFragment.setTargetFragment(this, 0);
			dialogFragment.show(
					getParentFragmentManager(),
					"androidx.preference.PreferenceFragment.DIALOG");

			// Inject the button after the dialog is shown
			getParentFragmentManager().registerFragmentLifecycleCallbacks(
					new FragmentManager.FragmentLifecycleCallbacks() {
				@Override
				public void onFragmentStarted(
						@androidx.annotation.NonNull FragmentManager fm,
						@androidx.annotation.NonNull androidx.fragment.app.Fragment f) {
					if (f == dialogFragment) {
						android.app.Dialog dialog = dialogFragment.getDialog();
						if (dialog instanceof androidx.appcompat.app.AlertDialog alertDialog) {

							// Configure the Neutral Button for reset default value
							Button btnReset = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
							btnReset.setText(R.string.prefs_reset_default_value);
							btnReset.setVisibility(android.view.View.VISIBLE);

							btnReset.setOnClickListener(v -> {
								if (preference instanceof EditTextPreference) {
									((EditTextPreference) preference).setText(defaultValue);
									alertDialog.dismiss();
								}
							});
						}
						// Cleanup
						getParentFragmentManager().unregisterFragmentLifecycleCallbacks(this);
					}
				}
			}, false);
		}


		/**
		 * Setup a ListPreference with a custom two lines summary, displays the selected entry
		 *  on the first line, and the static summary on the second line.
		 *
		 * @param preferenceKey preference identifier
		 * @param staticSummary text to show on the second line
		 */
		private void setupListPreference(String preferenceKey, String staticSummary) {
			ListPreference listPref = findPreference(preferenceKey);

			if (listPref == null) return;

			listPref.setSummaryProvider(preference -> {
				ListPreference lp = (ListPreference) preference;
				CharSequence entry = lp.getEntry();
				// Null check: entry might be null if no value is selected
				String displayValue = Objects.requireNonNull(entry).toString();
				return displayValue + ".\n" + staticSummary;
			});
		}

	}
}