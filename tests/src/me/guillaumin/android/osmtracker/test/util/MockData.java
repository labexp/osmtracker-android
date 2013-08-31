package me.guillaumin.android.osmtracker.test.util;

import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.net.Uri;

public class MockData {

	private static Object[][] mockTrackPoints = new Object[][] {
			{12.34, 56.78, 0.42f, 4321.7d, 45.8f },
			{21.57, 12.6,  0.24f, 12.1d,	12.6f }  
	};
	
	private static Object[][] mockWayPoints = new Object[][] {
			{34.12, 18.45, 0.25f, 5812.2d, 284.5f, 2, "wp1", "http://link1.com", "uuid1"},
			{43.76, 31.89, 0.61f, 75.4d,	127.4f, 6, "wp2", "http://link2.com", "uuid2"}
	};

	public static long mockTrack(Context context) {
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
		
		
		ContentResolver cr = context.getContentResolver();
		
		// Create new track
		ContentValues values = new ContentValues();
		values.put(Schema.COL_NAME, "");
		values.put(Schema.COL_START_DATE, c.getTime().getTime());
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
		values.put(Schema.COL_NAME, "gpx-test");
		Uri trackUri = cr.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);

		DataHelper helper = new DataHelper(context);
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

		return trackId;
	}
	
	public static long mockBigTrack(Context context, int numWayPoints, int numTrackPoints) {
		ContentResolver cr = context.getContentResolver();
		
		// Create new track
		ContentValues values = new ContentValues();
		values.put(Schema.COL_NAME, "");
		values.put(Schema.COL_START_DATE, System.currentTimeMillis());
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
		values.put(Schema.COL_NAME, "gpx-big-test");
		Uri trackUri = cr.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);
		
		Random r = new Random();

		DataHelper helper = new DataHelper(context);
		for (int i=0; i<numWayPoints; i++) {
			Location l = new Location("test");
			l.setLatitude((r.nextDouble() * 180) -90);
			l.setLongitude((r.nextDouble() * 360) - 180);
			l.setAccuracy(r.nextFloat());
			l.setAltitude((r.nextDouble() * 2000) - 1000);
			l.setSpeed(r.nextFloat() * 200);
			l.setTime(System.currentTimeMillis());
			helper.track(trackId, l);
		}
		
		for (int i=0; i<numTrackPoints; i++) {
			Location l = new Location("test");
			l.setLatitude((r.nextDouble() * 180) -90);
			l.setLongitude((r.nextDouble() * 360) - 180);
			l.setAccuracy(r.nextFloat());
			l.setAltitude((r.nextDouble() * 2000) - 1000);
			l.setSpeed(r.nextFloat() * 200);
			l.setTime(System.currentTimeMillis());

			helper.wayPoint(trackId, l,
					r.nextInt() * 10,
					"wayPoint #"+i,
					"http://link.com/"+i,
					UUID.randomUUID().toString());
		}

		return trackId;

	}
}
