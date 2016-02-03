package me.guillaumin.android.osmtracker.db.model;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle; 

/**
 * Represents statistics, such as  total length and maximum speed, for a Track
 *
 * @author Arseniy Lartsev
 *
 */
public class TrackStatistics {
	private long trackId;
	private long pointCount;
	private float length;
	private float maxSpeed;
	private double lastLatitude;
	private double lastLongitude;
	private long lastTime;
	private int lastId;
	private float timeMoving;
	private ContentResolver contentResolver;
	
	private static double square(double x) {
		return x*x;
	}

	/**
	 * Compute the approximate distance in meters between two locations, with 0.5% precision
	 */
	public static float getDistance(double lat1, double long1, double lat2, double long2) {
		final double R = 6370e3; // Earth radius
		final double rad_per_deg = Math.PI/180; // radians per degree
		
		lat1 *= rad_per_deg;
		long1 *= rad_per_deg;
		lat2 *= rad_per_deg;
		long2 *= rad_per_deg;
		
		double dLat = lat2-lat1;
		double dLon = long2-long1;

		double A = square(Math.sin(0.5*dLat)) + Math.cos(lat1) * Math.cos(lat2) * square(Math.sin(0.5*dLon));
		
		return (float)(2 * R * Math.asin(Math.sqrt(A)));
	}

	public TrackStatistics () {
		trackId = -1;
		length = 0;
		maxSpeed = 0;
		pointCount = 0;
		timeMoving = 0;
	}

	/**
	 * build a track statistics object with the given cursor
	 *
	 * @param trackId id of the track that will be built
	 * @param cr the content resolver to use
	 */
	public TrackStatistics (final long trackId, ContentResolver cr) {
		this.trackId = trackId;
		contentResolver = cr;
		length = 0;
		maxSpeed = 0;
		pointCount = 0;
		timeMoving = 0;
		
		update();
	}

	private static final String BUNDLE_POINTCOUNT = "TrackStatistics.pointCount";
	private static final String BUNDLE_LENGTH = "TrackStatistics.length";
	private static final String BUNDLE_MAXSPEED = "TrackStatistics.maxSpeed";
	private static final String BUNDLE_LASTLATITUDE = "TrackStatistics.lastLatitude";
	private static final String BUNDLE_LASTLONGITUDE = "TrackStatistics.lastLongitude";
	private static final String BUNDLE_LASTTIME = "TrackStatistics.lastTime";
	private static final String BUNDLE_LASTID = "TrackStatistics.lastId";
	private static final String BUNDLE_TIMEMOVING = "TrackStatistics.timeMoving";

	/**
	 * Encode the statistical data as Bundle 
	 */
	public Bundle getData() {
		Bundle data = new Bundle();
		data.putLong(BUNDLE_POINTCOUNT, pointCount);
		data.putFloat(BUNDLE_LENGTH, length);
		data.putFloat(BUNDLE_MAXSPEED, maxSpeed);
		data.putDouble(BUNDLE_LASTLATITUDE, lastLatitude);
		data.putDouble(BUNDLE_LASTLONGITUDE, lastLongitude);
		data.putLong(BUNDLE_LASTTIME, lastTime);
		data.putInt(BUNDLE_LASTID, lastId);
		data.putFloat(BUNDLE_TIMEMOVING, timeMoving);

		return data;
	}

	/**
	 * build a track statistics object with the given cursor and pre-existing statistical data
	 *
	 * @param trackId id of the track that will be built
	 * @param cr the content resolver to use
	 * @param data previously existing statistical data; the bundle isn't required to actually
	 *             contain that data   
	 */
	public TrackStatistics (final long trackId, ContentResolver cr, Bundle data) {
		this.trackId = trackId;
		contentResolver = cr;
		
		// If the Bundle doesn't contain the required data, everything will be zero, which is fine
		pointCount = data.getLong(BUNDLE_POINTCOUNT);
		length = data.getFloat(BUNDLE_LENGTH);
		maxSpeed = data.getFloat(BUNDLE_MAXSPEED);
		lastLatitude = data.getDouble(BUNDLE_LASTLATITUDE);
		lastLongitude = data.getDouble(BUNDLE_LASTLONGITUDE);
		lastTime = data.getLong(BUNDLE_LASTTIME);
		lastId = data.getInt(BUNDLE_LASTID);
		timeMoving = data.getFloat(BUNDLE_TIMEMOVING);

		update();
	}

	private void addPoint(double latitude, double longitude, float accuracy, float speed, long time, int point_id) {
		if (pointCount > 0) {
			// The "distance and time only counts when the speed is non-zero" principle has been borrowed from osmand
			if ((speed > 0) && (time != 0) && (lastTime != 0)) {
				length += getDistance(lastLatitude, lastLongitude, latitude, longitude);
				timeMoving += time - lastTime;
			}
		}
		pointCount += 1;

		if(speed > maxSpeed)
			maxSpeed = speed;
		lastLatitude = latitude;
		lastLongitude = longitude;
		lastTime = time;
		lastId = point_id;
	}

	/**
	 * Update the statistics by reading new points from the database
	 */
	public void update() {
		String selection = null;
		if (pointCount > 0)
			selection = Schema.COL_ID + " > " + String.valueOf(lastId);
		Cursor cursor = contentResolver.query(TrackContentProvider.trackPointsUri(trackId), null, 
				selection, null, null);
		if(! cursor.moveToFirst()) {
			cursor.close();
			return;
		}

		while (! cursor.isAfterLast()) {
			double latitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LATITUDE));
			double longitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LONGITUDE));
			float speed = cursor.getFloat(cursor.getColumnIndex(Schema.COL_SPEED));
			float accuracy = cursor.getFloat(cursor.getColumnIndex(Schema.COL_ACCURACY));
			long time = cursor.getLong(cursor.getColumnIndex(Schema.COL_TIMESTAMP));
			int id = cursor.getInt(cursor.getColumnIndex(Schema.COL_ID));

			addPoint(latitude, longitude, accuracy, speed, time, id);
			cursor.moveToNext();
		}

		cursor.close();
	}

	/**
	 * Get track length, in meters
	 */
	public float totalLength() {
		return length;
	}

	/**
	 * Get maximum speed, in meters per second
	 */
	public float maximumSpeed() {
		return maxSpeed;
	}

	/**
	 * Get aveare speed, in meters per second
	 */
	public float averageSpeed() {
		return 1000*length/timeMoving;
	}

}
