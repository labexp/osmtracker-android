package net.osmtracker.activity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.stringContainsInOrder;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;


@RunWith(AndroidJUnit4.class)
public class PreferencesTest {

	private Context context;
	private ActivityScenario<Preferences> activity;

	@Before
	public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		// Reset preferences to default before each test to ensure a clean state
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().clear().commit();

		// Launch the activity
		activity = ActivityScenario.launch(Preferences.class);
	}

	@After
	public void tearDown() {
		activity.close();
	}

	/**
	 * Test that the Storage Directory preference logic works to rejects empty input.
	 */
	@Test
	public void testStorageDirectoryValidatesNonEmpty() {
		String keyTitle = context.getString(R.string.prefs_storage_dir);
		String defaultValue = OSMTracker.Preferences.VAL_STORAGE_DIR;

		// Looks for storage directory preference
		scrollToAndClick(keyTitle);

		// Try to save an empty value
		onView(withId(android.R.id.edit)).perform(clearText());
		onView(withText(android.R.string.ok)).perform(click());

		// Open the preference to verify the value in the list remains the default (unchanged)
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.check(matches(hasDescendant(withText(defaultValue))));
	}

	/**
	 * Test that the Storage Directory preference logic works to automatically append a leading
	 * slash separator if missing.
	 */
	@Test
	public void testStorageDirectoryValidatesAppendLeadingSlash() {
		String keyTitle = context.getString(R.string.prefs_storage_dir);
		String expected = File.separator + "my_folder";


		// Looks for storage directory preference
		scrollToAndClick(keyTitle);

		// Try to type a value without a slash
		onView(withId(android.R.id.edit)).perform(clearText());
		onView(withId(android.R.id.edit))
				.perform(typeText("my_folder"));
		onView(withText(android.R.string.ok)).perform(click());

		// Open the preference to verify the value in the list is the expected
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.check(matches(hasDescendant(withText(expected))));
	}

	/**
	 * Test Numeric Input logic (GPS Logging Interval): update summary with suffix.
	 */
	@Test
	public void testNumericInputLogic() {
		String title = context.getString(R.string.prefs_gps_logging_interval);
		String suffix = context.getString(R.string.prefs_gps_logging_interval_seconds);

		scrollToAndClick(title);

		// Enter a valid number
		onView(withId(android.R.id.edit))
				.perform(clearText(), typeText("30"));
		onView(withText(android.R.string.ok)).perform(click());

		// Verify summary format: "30 seconds. <Static Summary>"
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.check(matches(hasDescendant(withText(stringContainsInOrder(Arrays.asList("30",
						suffix))))));
	}

	/**
	 * Test that the Reset button in numeric preferences restores the default value.
	 */
	@Test
	public void testResetButtonResetsValue() {
		String title = context.getString(R.string.prefs_gps_logging_interval);
		String suffix = context.getString(R.string.prefs_gps_logging_interval_seconds);
		String defaultValue = OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL;

		scrollToAndClick(title);

		// Set a custom value "50"
		onView(withId(android.R.id.edit)).perform(clearText(), typeText("50"));
		onView(withText(android.R.string.ok)).perform(click());

		// Verify custom value is set
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.check(matches(hasDescendant(withText(stringContainsInOrder(Arrays.asList("50",
						suffix))))));

		// Reopen dialog
		scrollToAndClick(title);

		// Click the Reset button (Neutral button)
		onView(withText(R.string.prefs_reset_default_value)).perform(click());

		// Verify value is back to default "0"
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.check(matches(hasDescendant(withText(stringContainsInOrder(Arrays.asList(
						defaultValue,
						suffix))))));
	}

	/**
	 * Test ListPreference custom summary logic (Screen Orientation)
	 * Should show "Selected Value. \n ..." (don't check for the 2nd line of the summary)
	 */
	@Test
	public void testListPreferenceCustomSummary() {
		String title = context.getString(R.string.prefs_ui_orientation);

		scrollToAndClick(title);

		// Select 1st option from array resource entries
		String[] entries = context.getResources()
				.getStringArray(R.array.prefs_ui_orientation_options_keys);
		onView(withText(entries[0])).perform(click());

		// Verify the two-line summary exists
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class)).check(matches(hasDescendant(
				withText(stringContainsInOrder(Arrays.asList(entries[0], ".\n"))))));
	}

	/**
	 * Test Clear OAuth Data logic.
	 */
	@Test
	public void testClearOAuthData() {
		String title = context.getString(R.string.prefs_osm_clear_oauth_data);

		// Inject a fake token to enable the button
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
				.edit();
		editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, "fake_token");
		editor.commit();

		// Relaunch to refresh UI state
		ActivityScenario.launch(Preferences.class);


		scrollToAndClick(title);

		// Click OK on Confirmation Dialog
		onView(withText(R.string.prefs_osm_clear_oauth_data_dialog)).check(matches(isDisplayed()));
		onView(withText(android.R.string.ok)).perform(click());

		// Verify token is gone in prefs
		assert(!PreferenceManager.getDefaultSharedPreferences(context)
				.contains(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN));
	}

	// --- Helper Methods ---

	/**
	 * Helper to scroll to a preference in the RecyclerView and click it.
	 */
	private void scrollToAndClick(String text) {
		onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
				.perform(RecyclerViewActions.actionOnItem(
						hasDescendant(withText(text)),
						click()));
	}

}
