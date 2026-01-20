package net.osmtracker.db;

import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class DataHelperNoteTest {

	private DataHelper dataHelper;

	@Before
	public void setup() {
		// Initialize DataHelper with the Robolectric application context
		dataHelper = new DataHelper(ApplicationProvider.getApplicationContext());
	}

	@Test
	public void testDeleteNote_RemovesFromDatabase() {
		String noteUUID = UUID.randomUUID().toString();
		long trackId = 1L;

		// 1. Insert a note
		Location loc = new Location("gps");
		loc.setLatitude(1.23);
		loc.setLongitude(4.56);
		loc.setTime(System.currentTimeMillis());

		// Initialize extras to avoid NullPointerException if logic accesses location extras
		loc.setExtras(new Bundle());

		dataHelper.trackNote(trackId, loc, "Note to delete", noteUUID);

		// 2. Verify it exists before deletion
		Assert.assertTrue("Note should exist after insertion", noteExists(trackId));

		// 3. Delete note
		dataHelper.deleteNote(noteUUID);

		// 4. Verify it is gone
		Assert.assertFalse("Note should have been deleted from DB", noteExists(trackId));
	}

	private boolean noteExists(long TrackId) {
		// Query using the content resolver provided by the Robolectric environment
		Cursor c = ApplicationProvider.getApplicationContext().getContentResolver().query(
				TrackContentProvider.notesUri(TrackId),
				null,null,null,null);

		boolean exists = (c != null && c.getCount() > 0);
		if (c != null) {
			c.close();
		}
		return exists;
	}
}