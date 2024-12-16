package net.osmtracker.util;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import java.util.Arrays;

/**
 * <p>Validates the theme from the preferences
 * and update it to the default value if necessary.</p>
 * 
 * <p>That's required since the theme references have
 * changed to support Android 3+. Users that have upgrade
 * from previous version were still referencing the old
 * theme, causing the menu to disappear.</p>
 *
 */
public class ThemeValidator {

	/**
	 * Return a valid theme, possibly fixing the preference value if needed
	 * @param prefs
	 * @param res
	 * @return
	 */
	public static String getValidTheme(SharedPreferences prefs, Resources res) {
		String theme = prefs.getString(
				OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
		
		if (! Arrays.asList(res.getStringArray(R.array.prefs_theme_values)).contains(theme)) {
			theme = OSMTracker.Preferences.VAL_UI_THEME;
			Editor e = prefs.edit();
			e.putString(OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
			e.apply();
		}
		
		return theme;
	}
	
}
