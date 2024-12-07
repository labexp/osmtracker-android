package net.osmtracker.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import net.osmtracker.R;

import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Set the SharedPreferences name
            getPreferenceManager().setSharedPreferencesName(getString(R.string.shared_pref));
            // Load the preferences from an XML resource
			setPreferencesFromResource(R.xml.preferences, rootKey);
			SharedPreferences prefs = getContext().getSharedPreferences(getString(R.string.shared_pref), MODE_PRIVATE);
			Map<String, ?> all = prefs.getAll();
		}
	}
}
