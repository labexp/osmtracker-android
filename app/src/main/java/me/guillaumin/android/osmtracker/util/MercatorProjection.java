package me.guillaumin.android.osmtracker.util;


/**
 * Geopoint to 2D projection using Mercator system.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class MercatorProjection {

	/**
	 * Maximum latitude useable with Mercator projection.
	 */
	private static final double MAX_LATITUDE = 85.0511f;

	/**
	 * X & longitude offset in used 2-dim arrays.
	 */
	public static final byte X = 0, LONGITUDE = 0;

	/**
	 * Y & latitude offsets in used 2-dim arrays.
	 */
	public static final byte Y = 1, LATITUDE = 1;

	/**
	 * Width & Height of the projection (pixels)
	 */
	private int width, height;
	
	/**
	 * Scale of the projection
	 */
	private double scale;

	/**
	 * Four corners of the projection, in converted coordinates
	 */
	private double topX, topY, bottomX, bottomY;

	/**
	 * Dimensions of projected space.
	 */
	private double dimX, dimY;

	public MercatorProjection(double minLat, double minLon, double maxLat, double maxLon, int w, int h) {
		width = w;
		height = h;

		// Get data range for X and Y
		double rangeX = Math.abs(convertLongitude(maxLon) - convertLongitude(minLon));
		double rangeY = Math.abs(convertLatitude(maxLat) - convertLatitude(minLat));

		// Determine scale for each axis
		double scaleX = rangeX / width;
		double scaleY = rangeY / height;

		// Determine which scale to use. We take the greater to
		// be able to fit in width AND height
		scale = (scaleX > scaleY) ? scaleX : scaleY;

		// Determine offset for X & Y, to translate
		// lon/lat into screen center
		double offsetX = (width * scale) - rangeX;
		double offsetY = (height * scale) - rangeY;

		// Determine 4 corners of projection
		topX = convertLongitude(minLon) - (offsetX / 2);
		topY = convertLatitude(minLat) - (offsetY / 2);
		bottomX = convertLongitude(maxLon) + (offsetX / 2);
		bottomY = convertLatitude(maxLat) + (offsetY / 2);

		// Calculate projection dimensions
		dimX = bottomX - topX;
		dimY = bottomY - topY;
	}

	/**
	 * Projects lon/lat coordinates into this projection.
	 * 
	 * @param longitude
	 *				Longitude to project
	 * @param latitude
	 *				Latitude to project
	 * @return An array of 2 int projected coordinates (use
	 *			{@link MercatorProjection.X} and {@link MercatorProjection.Y} for
	 *			access.
	 */
	public int[] project(double longitude, double latitude) {
		int[] out = new int[2];

		out[X] = (int) Math.round(((convertLongitude(longitude) - topX) / dimX) * width);
		out[Y] = (int) Math.round(height - (((convertLatitude(latitude) - topY) / dimY) * height));

		return out;
	}

	/**
	 * Convert longitude to X coordinate.
	 * 
	 * @param longitude
	 *				Longitude to convert.
	 * @return Converted X coordinate.
	 */
	private double convertLongitude(double longitude) {
		return longitude;
	}

	/**
	 * Converts latitude to Y coordinate.
	 * 
	 * @param latitude
	 *				Latitude to convert.
	 * @return Converted Y coordinate.
	 */
	private double convertLatitude(double latitude) {
		if (latitude < -MAX_LATITUDE) {
			latitude = -MAX_LATITUDE;
		} else if (latitude > MAX_LATITUDE) {
			latitude = MAX_LATITUDE;
		}

		return Math.log(Math.tan(Math.PI / 4 + (latitude * Math.PI / 180 / 2))) / (Math.PI / 180);
	}

	public double getScale() {
		return scale;
	}
	
	/**
	 * Given a float degree value (latitude or longitude), format it to Degrees/Minutes/Seconds.
	 * @param degrees  The value, such as 43.0438
	 * @param isLatitude  Is this latitude, not longitude?
	 * @return  The Degrees,Minutes,Seconds, such as: 43° 2' 38" N
	 */
	public static String formatDegreesAsDMS(Float degrees, final boolean isLatitude) {		
		if (degrees == null) {
			return "";
		}
		
		final boolean neg;
		if (degrees > 0) {
			neg = false;
		} else {
			neg = true;
			degrees = -degrees;
		}
		StringBuffer dms = new StringBuffer();

		int n = degrees.intValue();
		dms.append(n);
		dms.append("\u00B0 ");

		degrees = (degrees - n) * 60.0f;
		n = degrees.intValue();
		dms.append(n);
		dms.append("' ");

		degrees = (degrees - n) * 60.0f;
		n = degrees.intValue();
		dms.append(n);
		dms.append("\" ");

		if (isLatitude)
			dms.append(neg ? 'S' : 'N');
		else
			dms.append(neg ? 'W' : 'E');

		return dms.toString();
	}
}
