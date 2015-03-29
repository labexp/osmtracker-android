package me.guillaumin.android.osmtracker.util;

import org.osmdroid.api.IGeoPoint;

public class DistanceUtil {

	private static final double DEG2RAD = Math.PI / 180.0;
	// private static final double RAD2DEG = 180.0 / Math.PI;
	private static final int RADIUS_EARTH_METERS = 6378137;
	
	/**
	 * Computes the distance with Spherical Law of Cosines
	 * 	http://www.movable-type.co.uk/scripts/latlong.html
	 * @param lat1 
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return The distance (in meters) between point1 and point2
	 */
	public static float getDistance(final float lat1, final float lon1, final float lat2, final float lon2) {
		final double theta = lon1 - lon2;
		System.out.println("theta = " + theta);
		final double v =  (Math.sin(DEG2RAD * lat1) * Math.sin(DEG2RAD * lat2))
						+ (Math.cos(DEG2RAD * lat1) * Math.cos(DEG2RAD * lat2) * Math.cos(DEG2RAD * theta));
		
		// Due to Float/Double approximations sometimes the value v is greater than 1, thus out of Math.acos() domain
		// @see: http://www.mathworks.it/it/help/matlab/ref/acos.html
		return (v > 1) ? 0.0f : (float) (Math.acos(v) * RADIUS_EARTH_METERS);
	}
	
	/**
	 * Computes the distance with Spherical Law of Cosines
	 * 	http://www.movable-type.co.uk/scripts/latlong.html
	 * @param point1 
	 * @param point2
	 * @return The distance (in meters) between point1 and point2
	 */
	public static float getDistance(final IGeoPoint point1, final IGeoPoint point2) {
	  final float lat1 = (float) (point1.getLatitudeE6() / 1E6);
	  final float lon1 = (float) (point1.getLongitudeE6() / 1E6);
	  final float lat2 = (float) (point2.getLatitudeE6() / 1E6);
	  final float lon2 = (float) (point2.getLongitudeE6() / 1E6);
	  
	  return getDistance(lat1, lon1, lat2, lon2);	
	}
}
