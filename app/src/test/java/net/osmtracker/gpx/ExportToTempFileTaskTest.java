package net.osmtracker.gpx;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.database.MatrixCursor;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.TrackContentProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ContentProviderController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, qualifiers = "es")
public class ExportToTempFileTaskTest {

	private Context context;
	private final long testTrackId = 123L;

	private ContentProviderController<TrackContentProvider> providerController;


	/**
	 * Concrete implementation of the abstract ExportToTempFileTask for testing.
	 */
	private static class TestTempExportTask extends ExportToTempFileTask {
		public TestTempExportTask(Context context, long trackId) {
			super(context, trackId);
		}
		@Override
		protected void executionCompleted() {
			// No-op for testing
		}
	}

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		providerController = Robolectric.buildContentProvider(TrackContentProvider.class)
				.create(TrackContentProvider.AUTHORITY);

		// Set up locale and preferences to to match gpx-test.gpx file
		Locale esLocale = new Locale("es");
		Locale.setDefault(esLocale);
		context.getResources().getConfiguration().setLocale(esLocale);
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION, true)
				.putString(OSMTracker.Preferences.KEY_OUTPUT_COMPASS, "extension")
				.apply();

		// Initialize the DB record so the Task constructor doesn't fail
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_ID, testTrackId);
		values.put(TrackContentProvider.Schema.COL_NAME, "2020-12-30_17-20-17");
		values.put(TrackContentProvider.Schema.COL_START_DATE, 990055225000L);
		values.put(TrackContentProvider.Schema.COL_TAGS, "osmtracker");
		context.getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
	}

	@After
	public void tearDown() {
		// Shut down the provider explicitly to close database
		if (providerController != null) {
			providerController.shutdown();
		}
		ShadowContentResolver.reset();
	}

	@Test
	public void testExportMatchesGpxResource() throws Exception {
		try (
				MatrixCursor pointCursor = createPointCursor();
				MatrixCursor wptCursor = createWptCursor()
		) {
			TestTempExportTask task = new TestTempExportTask(context, testTrackId);
			File outputFile = task.getTmpFile();

			// Load gpx-test.gpx file
			InputStream is = getClass().getClassLoader().getResourceAsStream("gpx/gpx-test.gpx");
			if (is == null) throw new RuntimeException("Resource gpx-test.gpx not found");
			String expectedXml = IOUtils.toString(is, StandardCharsets.UTF_8.name());

			// Execute GPX writing logic
			task.writeGpxFile("2020-12-30_17-20-17","osmtracker",null,
					pointCursor,wptCursor,outputFile);
			String actualXml = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8.name());

			assertEquals(expectedXml, actualXml);
		}
	}

	// Helper method to create the point cursor
	private MatrixCursor createPointCursor() {
		String[] ptColumns = new String[] {
				TrackContentProvider.Schema.COL_LATITUDE,
				TrackContentProvider.Schema.COL_LONGITUDE,
				TrackContentProvider.Schema.COL_ELEVATION,
				TrackContentProvider.Schema.COL_TIMESTAMP,
				TrackContentProvider.Schema.COL_SEG_ID,
				TrackContentProvider.Schema.COL_ACCURACY,
				TrackContentProvider.Schema.COL_SPEED,
				TrackContentProvider.Schema.COL_COMPASS,
				TrackContentProvider.Schema.COL_COMPASS_ACCURACY,
				TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE,
		};

		MatrixCursor pointCursor = new MatrixCursor(ptColumns);
		pointCursor.addRow(new Object[]{10.0375690436648, -84.2122886824885, 1015.13159253262,
				990055225000L, 0, 35.635440826416, 0.361938893795013, 204.690002441406, 2.0, null});
		pointCursor.addRow(new Object[]{10.0377106969496, -84.2123268390527, 1007.3209409276,
				990055226000L, 1, 52.0706405639648, 0.271533757448196, 204.360000610352, 2.0, null});
		return pointCursor;
	}

	// Helper method to create the waypoint cursor
	private MatrixCursor createWptCursor() {
		String[] wptColumns = new String[]{
				TrackContentProvider.Schema.COL_LATITUDE,
				TrackContentProvider.Schema.COL_LONGITUDE,
				TrackContentProvider.Schema.COL_ELEVATION,
				TrackContentProvider.Schema.COL_TIMESTAMP,
				TrackContentProvider.Schema.COL_NAME,
				TrackContentProvider.Schema.COL_ACCURACY,
				TrackContentProvider.Schema.COL_COMPASS,
				TrackContentProvider.Schema.COL_COMPASS_ACCURACY,
				TrackContentProvider.Schema.COL_LINK,
				TrackContentProvider.Schema.COL_NBSATELLITES,
				TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE,
		};

		MatrixCursor wptCursor = new MatrixCursor(wptColumns);
		wptCursor.addRow(new Object[]{10.0375634848231, -84.2123549868801, 1029.89940680377,
				990055228000L, "Punto de prueba", 24.0, 204.029998779297, 2.0, null, 0, null});
		return wptCursor;
	}

}
