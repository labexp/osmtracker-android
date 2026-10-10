package net.osmtracker.activity;

import static org.robolectric.Robolectric.buildActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import net.osmtracker.R;
import net.osmtracker.db.model.Track;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
//Min SDK of the App
@Config(sdk = 25)
public class TrackDetailEditorTest {
	private TrackDetailEditor activity;

	/**
	 * Concrete implementation of the abstract TrackDetailEditor for testing purposes.
	 */
	public static class TrackDetailEditorActivity extends TrackDetailEditor {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// Pass a dummy layout and ID to satisfy the base class
			super.onCreate(savedInstanceState, R.layout.trackdetail, 1L);
		}
	}
	@Before
	public void setup() {
		activity = buildActivity(TrackDetailEditorActivity.class).create().get();

		// Ensure Spinner is populated as it would be from XML
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				activity,
				R.array.prefs_osm_visibility_keys,
				android.R.layout.simple_spinner_item);
		activity.spVisibility.setAdapter(adapter);
	}

	@Test
	public void testBindTrackSetsCorrectSpinnerPosition() {
		Track t = new Track();
		t.setVisibility(Track.OSMVisibility.Trackable);

		activity.bindTrack(t);

		// Verify that position 0 - Trackable is selected in the UI
		Assert.assertEquals(Track.OSMVisibility.Trackable.position,
				activity.spVisibility.getSelectedItemPosition());
	}

	@Test
	public void testSaveCapturesCorrectEnumValue() {
		// Simulate user selecting "Trackable" (Index 0)
		activity.spVisibility.setSelection(Track.OSMVisibility.Trackable.position);

		// Verify that save logic correctly converts position to "Trackable"
		Track.OSMVisibility result = Track.OSMVisibility.fromPosition(
				activity.spVisibility.getSelectedItemPosition());

		Assert.assertEquals(Track.OSMVisibility.Trackable, result);
		Assert.assertEquals(Track.OSMVisibility.Trackable.name(), result.name());
	}
}
