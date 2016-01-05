package me.guillaumin.android.osmtracker.db.model;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

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
	private float timeMoving;

	/**
	 * Copypasta from osmand
	 */
	private static void computeDistanceAndBearing(double lat1, double lon1,
												  double lat2, double lon2, float[] results) {
		// Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
		// using the "Inverse Formula" (section 4)
		int MAXITERS = 20;
		// Convert lat/long to radians
		lat1 *= Math.PI / 180.0;
		lat2 *= Math.PI / 180.0;
		lon1 *= Math.PI / 180.0;
		lon2 *= Math.PI / 180.0;

		double a = 6378137.0; // WGS84 major axis
		double b = 6356752.3142; // WGS84 semi-major axis
		double f = (a - b) / a;
		double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

		double L = lon2 - lon1;
		double A = 0.0;
		double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
		double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

		double cosU1 = Math.cos(U1);
		double cosU2 = Math.cos(U2);
		double sinU1 = Math.sin(U1);
		double sinU2 = Math.sin(U2);
		double cosU1cosU2 = cosU1 * cosU2;
		double sinU1sinU2 = sinU1 * sinU2;

		double sigma = 0.0;
		double deltaSigma = 0.0;
		double cosSqAlpha = 0.0;
		double cos2SM = 0.0;
		double cosSigma = 0.0;
		double sinSigma = 0.0;
		double cosLambda = 0.0;
		double sinLambda = 0.0;

		double lambda = L; // initial guess
		for (int iter = 0; iter < MAXITERS; iter++) {
			double lambdaOrig = lambda;
			cosLambda = Math.cos(lambda);
			sinLambda = Math.sin(lambda);
			double t1 = cosU2 * sinLambda;
			double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
			double sinSqSigma = t1 * t1 + t2 * t2; // (14)
			sinSigma = Math.sqrt(sinSqSigma);
			cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
			sigma = Math.atan2(sinSigma, cosSigma); // (16)
			double sinAlpha = (sinSigma == 0) ? 0.0 :
					cosU1cosU2 * sinLambda / sinSigma; // (17)
			cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
			cos2SM = (cosSqAlpha == 0) ? 0.0 :
					cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

			double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
			A = 1 + (uSquared / 16384.0) * // (3)
					(4096.0 + uSquared *
							(-768 + uSquared * (320.0 - 175.0 * uSquared)));
			double B = (uSquared / 1024.0) * // (4)
					(256.0 + uSquared *
							(-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
			double C = (f / 16.0) *
					cosSqAlpha *
					(4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
			double cos2SMSq = cos2SM * cos2SM;
			deltaSigma = B * sinSigma * // (6)
					(cos2SM + (B / 4.0) *
							(cosSigma * (-1.0 + 2.0 * cos2SMSq) -
									(B / 6.0) * cos2SM *
											(-3.0 + 4.0 * sinSigma * sinSigma) *
											(-3.0 + 4.0 * cos2SMSq)));

			lambda = L +
					(1.0 - C) * f * sinAlpha *
							(sigma + C * sinSigma *
									(cos2SM + C * cosSigma *
											(-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

			double delta = (lambda - lambdaOrig) / lambda;
			if (Math.abs(delta) < 1.0e-12) {
				break;
			}
		}

		float distance = (float) (b * A * (sigma - deltaSigma));
		results[0] = distance;
		if (results.length > 1) {
			float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
					cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
			initialBearing *= 180.0 / Math.PI;
			results[1] = initialBearing;
			if (results.length > 2) {
				float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
						-sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
				finalBearing *= 180.0 / Math.PI;
				results[2] = finalBearing;
			}
		}
	}

	/**
	 * Copypasta from osmand
	 * 
	 * Computes the approximate distance in meters between two
	 * locations, and optionally the initial and final bearings of the
	 * shortest path between them.  Distance and bearing are defined using the
	 * WGS84 ellipsoid.
	 *
	 * <p> The computed distance is stored in results[0].  If results has length
	 * 2 or greater, the initial bearing is stored in results[1]. If results has
	 * length 3 or greater, the final bearing is stored in results[2].
	 *
	 * @param startLatitude the starting latitude
	 * @param startLongitude the starting longitude
	 * @param endLatitude the ending latitude
	 * @param endLongitude the ending longitude
	 * @param results an array of floats to hold the results
	 *
	 * @throws IllegalArgumentException if results is null or has length < 1
	 */
	public static void distanceBetween(double startLatitude, double startLongitude,
									   double endLatitude, double endLongitude, float[] results) {
		if (results == null || results.length < 1) {
			throw new IllegalArgumentException("results is null or has length < 1");
		}
		computeDistanceAndBearing(startLatitude, startLongitude,
				endLatitude, endLongitude, results);
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
		length = 0;
		maxSpeed = 0;
		pointCount = 0;
		timeMoving = 0;

		Cursor cursor = cr.query(TrackContentProvider.trackPointsUri(trackId), null, null, null, null);
		if(! cursor.moveToFirst())
			return;

		while (! cursor.isAfterLast()) {
			double latitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LATITUDE));
			double longitude = cursor.getDouble(cursor.getColumnIndex(Schema.COL_LONGITUDE));
			float speed = cursor.getFloat(cursor.getColumnIndex(Schema.COL_SPEED));
			float accuracy = cursor.getFloat(cursor.getColumnIndex(Schema.COL_ACCURACY));
			long time = cursor.getLong(cursor.getColumnIndex(Schema.COL_TIMESTAMP));

			update(latitude, longitude, accuracy, speed, time);
			cursor.moveToNext();
		}

		cursor.close();
	}

	/**
	 * Update the statistics upon adding a new point to the track
	 */
	public void update(Location trackPoint){
		update(trackPoint.getLatitude(), trackPoint.getLongitude(), trackPoint.getAccuracy(),
				trackPoint.getSpeed(), trackPoint.getTime());
	}

	private void update(double latitude, double longitude, float accuracy, float speed, long time) {
		if (pointCount > 0) {
			float[] distance = new float[1];
			distanceBetween(lastLatitude, lastLongitude, latitude, longitude, distance);
			
			// The "distance and time only counts at non-zero speed" principle is borrowed from osmand
			if ((speed > 0) && (time != 0) && (lastTime != 0)) {
				length += distance[0];
				timeMoving += time - lastTime;
			}
		}
		pointCount += 1;

		if(speed > maxSpeed)
			maxSpeed = speed;
		lastLatitude = latitude;
		lastLongitude = longitude;
		lastTime = time;
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
