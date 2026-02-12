package net.osmtracker.gpx;

import static junit.framework.TestCase.assertEquals;
import static net.osmtracker.db.TrackContentProvider.Schema;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.OSMTracker.Preferences;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.exception.ExportTrackException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;


import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ExportToStorageTaskTest {

	private Context context;
	private DataHelper mockDataHelper;
	private ExportToStorageTask task;
	private SharedPreferences prefs;

	// Standard test date: Jan 2nd, 2000, 03:04:05 UTC
	private static final String DATE_STRING = "2000-01-02_03-04-05";
	private static final String TRACK_NAME = "MyTrack";

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		mockDataHelper = mock(DataHelper.class);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// Reset preferences to prevent test-to-test leakage
		prefs.edit().clear().apply();
		task = new ExportToStorageTask(context, mockDataHelper, 1L);
    }

	// --- Filename Generation Tests ---

	@Test
	public void testBuildGPXFilename_OnlyTrackName() {
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_NAME);
		Assert.assertEquals("MyTrack.gpx",
				executeBuildFilename(TRACK_NAME, createDate()));
	}

	@Test
	public void testBuildGPXFilename_TrackNameAndDate() {
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_NAME_DATE);
		assertEquals("MyTrack_" + DATE_STRING + ".gpx",
				executeBuildFilename(TRACK_NAME, createDate()));
	}

	@Test
	public void testBuildGPXFilename_DateAndTrackName() {
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_DATE_NAME);
		assertEquals(DATE_STRING + "_MyTrack" + ".gpx",
				executeBuildFilename(TRACK_NAME, createDate()));
	}

	@Test
	public void testBuildGPXFilename_OnlyDate() {
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_DATE);
		assertEquals(DATE_STRING + ".gpx",
				executeBuildFilename(TRACK_NAME, createDate()));
	}

	@Test
	public void testBuildGPXFilename_Sanitization() {
		String dirtyName = ":M/y*T@r~a\\c?k:";
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_NAME);
		assertEquals(";M_y_T_r_a_c_k;.gpx",
				executeBuildFilename(dirtyName, createDate()));
	}

	@Test
	public void testBuildGPXFilename_FallbackToDateWhenNameEmpty() {
		String emptyName = "";
		setupFilenamePreference(Preferences.VAL_OUTPUT_FILENAME_NAME);
		// Should fallback to the timestamp if name is missing
		assertEquals(DATE_STRING + ".gpx",
				executeBuildFilename(emptyName, createDate()));
	}

	// --- Export Directory Tests ---

	@Test
	public void testGetExportDirectory_CreatesMissingFolders() throws Exception {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
		prefs.edit().putString(Preferences.KEY_STORAGE_DIR, "NewAppFolder").apply();

		File result = task.getExportDirectory(new Date());

		assertTrue("Folder should be created", result.exists());
		assertTrue("Path should contain custom dir name", result.getAbsolutePath().contains("NewAppFolder"));
	}

	@Test
	public void testGetExportDirectory_ThrowsWhenNotWritable() {
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED_READ_ONLY);
		assertThrows(ExportTrackException.class, () -> task.getBaseExportDirectory());
	}

	@Test
	public void testGetSanitizedTrackName_ReplacesSlashes() {
		Track mockTrack = new Track();
		mockTrack.setName("Category/Sub/Track");
		when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

		String result = task.getSanitizedTrackNameByStartDate(new Date());
		assertEquals("Category_Sub_Track", result);
	}

	// --- Internal Helpers ---

	private void setupFilenamePreference(String format) {
		prefs.edit()
				.putString(Preferences.KEY_OUTPUT_FILENAME, format)
				// Reset label for predictability
				.putString(Preferences.KEY_OUTPUT_FILENAME_LABEL, "")
				.apply();
	}

	private String executeBuildFilename(String name, Date date) {
		return task.buildGPXFilename(createMockCursor(name, date.getTime()), context.getCacheDir());
	}

	private Cursor createMockCursor(String trackName, long trackStartDate) {
		Cursor mockCursor = mock(Cursor.class);
		when(mockCursor.getColumnIndex(Schema.COL_NAME)).thenReturn(1);
		when(mockCursor.getString(1)).thenReturn(trackName);
		when(mockCursor.getColumnIndex(Schema.COL_START_DATE)).thenReturn(2);
		when(mockCursor.getLong(2)).thenReturn(trackStartDate);
		return mockCursor;
	}

	/**
	 * Creates a UTC Date representing 2000-01-02 03:04:05.
	 */
	private static Date createDate() {
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		// Calendar months are 0-based (January is 0), so we subtract 1 from the input
		cal.set(2000, Calendar.JANUARY, 2, 3, 4, 5);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();

	}

}
