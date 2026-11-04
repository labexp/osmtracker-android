package net.osmtracker.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.TrackContentProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class TextNoteDialogTest {

	@Test
	public void restoreBeforeShowKeepsTrackId() {
		Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
		long trackId = 42;

		// Given a dialog whose state was saved before it was ever shown, e.g. the activity was
		// recreated twice in a row:
		TextNoteDialog oldDialog = new TextNoteDialog(activity, trackId);
		Bundle state = oldDialog.onSaveInstanceState();
		TextNoteDialog dialog = new TextNoteDialog(activity, trackId);
		dialog.onRestoreInstanceState(state);

		// When showing the restored dialog:
		dialog.show();

		// Then the waypoint is created in the right track, not in track 0:
		List<Intent> intents = Shadows.shadowOf(activity.getApplication()).getBroadcastIntents();
		Intent trackWp = null;
		for (Intent intent : intents) {
			if (OSMTracker.INTENT_TRACK_WP.equals(intent.getAction())) {
				trackWp = intent;
			}
		}
		assertEquals(trackId, trackWp.getLongExtra(TrackContentProvider.Schema.COL_TRACK_ID, 0));
	}
}
