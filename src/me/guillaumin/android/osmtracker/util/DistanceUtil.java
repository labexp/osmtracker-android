package me.guillaumin.android.osmtracker.util;

public class DistanceUtil {

	private static final double DEG2RAD = Math.PI / 180.0;
	// private static final double RAD2DEG = 180.0 / Math.PI;
	private static final double RADIUS_EARTH_METERS = 6371.01; 
	
	/**
	 * Computes the distance with Spherical Law of Cosines
	 * 	http://www.movable-type.co.uk/scripts/latlong.html
	 * @param lat1 
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return The distance (in meters) between point1 and point2
	 */
	public static double getDistance(final double lat1, final double lon1, final double lat2, final double lon2) {
	  double theta = lon1 - lon2;
	  double a = Math.sin(DEG2RAD * lat1) * Math.sin(DEG2RAD * lat2);
	  double b = Math.cos(DEG2RAD * lat1) * Math.cos(DEG2RAD * lat2) * Math.cos(DEG2RAD * theta);
	  
	  return Math.acos(a + b) * RADIUS_EARTH_METERS;
	}

}
