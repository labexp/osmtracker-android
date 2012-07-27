package me.guillaumin.android.osmtracker.test.gpx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import junit.framework.Assert;
import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackManager;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.gpx.ExportTrackTask;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

public class ExportTrackTaskTest extends ActivityInstrumentationTestCase2<TrackManager> {

	private long trackId;
	private File trackFile;
	
	private Object[][] mockTrackPoints = new Object[][] {
			{12.34, 56.78, 0.42f, 4321.7d, 45.8f },
			{21.57, 12.6,  0.24f, 12.1d,   12.6f }  
	};
	
	private Object[][] mockWayPoints = new Object[][] {
			{34.12, 18.45, 0.25f, 5812.2d, 284.5f, 2, "wp1", "http://link1.com", "uuid1"},
			{43.76, 31.89, 0.61f, 75.4d,   127.4f, 6, "wp2", "http://link2.com", "uuid2"}
	};

	public ExportTrackTaskTest() {
		super("me.guillaumin.android.osmtracker", TrackManager.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		trackFile = new File(Environment.getExternalStorageDirectory(), "osmtracker/gpx-test.gpx");
		if (trackFile.exists()) {
			Assert.assertTrue(trackFile.delete());
		}
		
		// Use same date for everything so that the generated
		// GPX file will always be the same
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(Calendar.YEAR, 2012);
		c.set(Calendar.MONTH, Calendar.MARCH);
		c.set(Calendar.DAY_OF_MONTH, 12);
		c.set(Calendar.HOUR_OF_DAY, 16);
		c.set(Calendar.MINUTE, 46);
		c.set(Calendar.SECOND, 38);
		
		ContentResolver cr = getActivity().getContentResolver();
		
		// Create new track
		ContentValues values = new ContentValues();
		values.put(Schema.COL_NAME, "");
		values.put(Schema.COL_START_DATE, c.getTime().getTime());
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
		values.put(Schema.COL_NAME, "gpx-test");
		Uri trackUri = cr.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		trackId = ContentUris.parseId(trackUri);

		DataHelper helper = new DataHelper(getActivity());
		for (Object[] mock: mockTrackPoints) {
			Location l = new Location("test");
			l.setLatitude((Double) mock[0]);
			l.setLongitude((Double) mock[1]);
			l.setAccuracy((Float) mock[2]);
			l.setAltitude((Double) mock[3]);
			l.setSpeed((Float) mock[4]);
			l.setTime(c.getTime().getTime());
			helper.track(trackId, l);
		}
		
		for (Object[] mock: mockWayPoints) {
			Location l = new Location("test");
			l.setLatitude((Double) mock[0]);
			l.setLongitude((Double) mock[1]);
			l.setAccuracy((Float) mock[2]);
			l.setAltitude((Double) mock[3]);
			l.setSpeed((Float) mock[4]);
			l.setTime(c.getTime().getTime());

			helper.wayPoint(trackId, l, (Integer) mock[5], (String) mock[6], (String) mock[7], (String) mock[8]);
		}
		
		helper.stopTracking(trackId);
		
		// Ensure easy filename
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Editor e = prefs.edit();
		e.clear();
		e.putString(OSMTracker.Preferences.KEY_OUTPUT_FILENAME, OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME);
		e.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK, false);
		e.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION, true);
		e.commit();
	}
	
	public void test() throws Exception {		
		new ExportTrackTask(getActivity(), trackId).execute().get();
		
		Assert.assertTrue(trackFile.exists());
		Assert.assertEquals(
				readFully(
						getInstrumentation().getContext().getAssets().open("gpx/gpx-test.gpx")),
				readFully(new FileInputStream(trackFile)));
	}

	private static String readFully(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(is));
		
		StringBuilder sb = new StringBuilder();
		String line;
		while( (line=reader.readLine()) != null ) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		reader.close();
		
		return sb.toString();
	}
	
}
