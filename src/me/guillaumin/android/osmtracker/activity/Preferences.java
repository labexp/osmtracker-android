package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

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
	}
	
}
