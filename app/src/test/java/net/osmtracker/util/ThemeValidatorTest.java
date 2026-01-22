package net.osmtracker.util;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ThemeValidatorTest {

	private SharedPreferences realPrefs;
	private Resources mockRes;

	@Before
	public void setUp() {
		Context context = ApplicationProvider.getApplicationContext();
		realPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		// Ensure a clean state for every test
		realPrefs.edit().clear().commit();

		mockRes = mock(Resources.class);
		String[] themes = {
				"net.osmtracker:style/DefaultTheme",
				"net.osmtracker:style/DarkTheme",
				"net.osmtracker:style/LightTheme",
				"net.osmtracker:style/HighContrast"
		};
		when(mockRes.getStringArray(R.array.prefs_theme_values)).thenReturn(themes);
	}


    @Test
    public void validateDefaultTheme(){
		// Set a valid theme in preferences
		realPrefs.edit().putString(
				OSMTracker.Preferences.KEY_UI_THEME,
				"net.osmtracker:style/DefaultTheme")
				.commit();

        String result =ThemeValidator.getValidTheme(realPrefs, mockRes);
        String expected = "net.osmtracker:style/DefaultTheme";
		assertEquals(expected, result);
    }

    /*Use a theme that is not included on the theme values array and also
     *  verify methods of the mocked editor so that the preferences are saved.*/
    @Test
    public void validateWrongTheme(){
		// Set an invalid theme in preferences
		realPrefs.edit().putString(
				OSMTracker.Preferences.KEY_UI_THEME,
				"net.osmtracker:style/YellowTheme")
				.commit();

		// The validator should detect "YellowTheme" is missing from the Resources array
		// and reset it to the default.
		String result = ThemeValidator.getValidTheme(realPrefs, mockRes);
		String expected = "net.osmtracker:style/DefaultTheme";

		assertEquals("Should fallback to DefaultTheme", expected, result);

		// Verify that the preference was actually updated/repaired in the storage
		assertEquals("Preference should be repaired in storage",
				expected, realPrefs.getString(OSMTracker.Preferences.KEY_UI_THEME, null));
	}
}
