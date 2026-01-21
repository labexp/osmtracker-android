package net.osmtracker.activity;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class OpenStreetMapNotesUploadTest {

	private Intent intent;

	@Before
	public void setUp() {
		// Prepare a valid intent with extras
		intent = new Intent(ApplicationProvider.getApplicationContext(),
				OpenStreetMapNotesUpload.class);
		intent.putExtra("noteId", 123L);
		intent.putExtra("noteContent", "Test Note Content");
		intent.putExtra("latitude", 45.0);
		intent.putExtra("longitude", 9.0);
		intent.putExtra("version", "3.0.1");
		intent.putExtra("appName", "OSMTrackerTest");

		// Reset preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
				ApplicationProvider.getApplicationContext());
		prefs.edit().clear().apply();
	}


	/**
	 * Verification of UI Binding and Intent Data Extraction
	 */
	@Test
	public void onCreate_populatesViewsCorrectly() {
		// Launch Activity
		OpenStreetMapNotesUpload activity = Robolectric.buildActivity(
				OpenStreetMapNotesUpload.class, intent)
				.create()
				.start()
				.resume()
				.get();

		TextView noteContentView = activity.findViewById(R.id.wplist_item_name);
		TextView noteFooterView = activity.findViewById(R.id.osm_note_footer);

		// Verify content extracted from intent
		assertEquals("Test Note Content", noteContentView.getText().toString());

		//check footer is correctly constructed according to strings.xml
		// Verify footer constructed with intent extras
		String expectedFooter = activity.getString(
				R.string.osm_note_footer,"OSMTrackerTest", "3.0.1");
		assertEquals(expectedFooter, noteFooterView.getText().toString());
	}


	/**
	 * Flow Control - Existing token should skip Auth and trigger task directly
	 */
	@Test
	public void startUpload_withExistingToken_skipsAuthFlow() {
		// Inject a fake token into SharedPreferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
				ApplicationProvider.getApplicationContext());
		prefs.edit().putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN,
				"fake_token").commit();

		OpenStreetMapNotesUpload activity = Robolectric.buildActivity(
				OpenStreetMapNotesUpload.class, intent)
				.create()
				.start()
				.resume()
				.get();

		// Trigger OK button
		activity.findViewById(R.id.osm_note_upload_button_ok).performClick();

		// Verify that NO new activity was started
		// because it bypassed auth and went to background task
		ShadowActivity shadowActivity = shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();

		Assert.assertNull("Should not start Auth browser if token is already present",
				startedIntent);
	}

}
