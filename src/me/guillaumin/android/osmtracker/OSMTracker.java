package me.guillaumin.android.osmtracker;


/**
 * Constants & app-wide variables.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class OSMTracker {

	/**
	 * Stores settings keys and default values.
	 * See preferences.xml for layout, strings-preferences.xml for text.
	 */
	public static final class Preferences {
		// Property names
		public final static String KEY_STORAGE_DIR = "logging.storage.dir";
		public final static String KEY_VOICEREC_DURATION = "voicerec.duration";
		public final static String KEY_UI_THEME = "ui.theme";
		public final static String KEY_GPS_OSSETTINGS = "gps.ossettings";
		public final static String KEY_GPS_CHECKSTARTUP = "gps.checkstartup";
		public final static String KEY_GPS_IGNORE_CLOCK = "gps.ignoreclock";
		public final static String KEY_OUTPUT_FILENAME = "gpx.filename";
		public final static String KEY_OUTPUT_ACCURACY = "gpx.accuracy";
		public final static String KEY_OUTPUT_GPX_HDOP_APPROXIMATION = "gpx.hdop.approximation";
		public final static String KEY_UI_BUTTONS_LAYOUT = "ui.buttons.layout";
		public final static String KEY_UI_DISPLAYTRACK_OSM = "ui.displaytrack.osm";

		// Default values
		public final static String VAL_STORAGE_DIR = "/osmtracker";
		public final static String VAL_VOICEREC_DURATION = "2";
		public final static String VAL_UI_THEME = "@android:style/Theme";
		public final static boolean VAL_GPS_CHECKSTARTUP = true;
		public final static boolean VAL_GPS_IGNORE_CLOCK = false;
		
		public final static String VAL_OUTPUT_FILENAME_NAME = "name";
		public final static String VAL_OUTPUT_FILENAME_NAME_DATE = "name_date";
		public final static String VAL_OUTPUT_FILENAME_DATE = "date";
		public final static String VAL_OUTPUT_FILENAME = VAL_OUTPUT_FILENAME_NAME_DATE;

		public final static String VAL_OUTPUT_ACCURACY_NONE = "none";
		public final static String VAL_OUTPUT_ACCURACY_WPT_NAME = "wpt_name";
		public final static String VAL_OUTPUT_ACCURACY_WPT_CMT = "wpt_cmt";
		public final static String VAL_OUTPUT_ACCURACY = VAL_OUTPUT_ACCURACY_NONE;
		
		public final static boolean VAL_OUTPUT_GPX_HDOP_APPROXIMATION = false;
		public final static String VAL_UI_BUTTONS_LAYOUT = "default";
		
		public final static boolean VAL_UI_DISPLAYTRACK_OSM = false;
	};
	
	/**
	 * The full Package name of OSMTracker returned by calling
	 * OSMTracker.class.getPackage().getName()
	 */
	public final static String PACKAGE_NAME = OSMTracker.class.getPackage().getName();

	/**
	 * Intent for tracking a waypoint
	 */
	public final static String INTENT_TRACK_WP = OSMTracker.PACKAGE_NAME + ".intent.TRACK_WP";

	/**
	 * Intent for updating a previously tracked waypoint
	 */
	public final static String INTENT_UPDATE_WP = OSMTracker.PACKAGE_NAME + ".intent.UPDATE_WP";
	
	/**
	 * Intent for deleting a previously tracked waypoint
	 */
	public final static String INTENT_DELETE_WP = OSMTracker.PACKAGE_NAME + ".intent.DELETE_WP";
	
	/**
	 * Intent to start tracking
	 */
	public final static String INTENT_START_TRACKING = OSMTracker.PACKAGE_NAME + ".intent.START_TRACKING";

	/**
	 * Intent to stop tracking
	 */
	public final static String INTENT_STOP_TRACKING = OSMTracker.PACKAGE_NAME + ".intent.STOP_TRACKING";

	/**
	 * Intent fired when the service go to background and should start to notify user. 
	 */
	public final static String INTENT_START_NOTIFY_BACKGROUND = OSMTracker.PACKAGE_NAME + ".intent.START_NOTIFY_BACKGROUND";
	
	/**
	 * Intent fired when the UI got back and the service should stop notifying the user of background job.
	 */
	public final static String INTENT_STOP_NOTIFY_BACKGROUND = OSMTracker.PACKAGE_NAME + ".intent.STOP_NOTIFY_BACKGROUND";
	
	/**
	 * Intent fired when the notification are cleared by the user.
	 */
	public final static String INTENT_NOTIFICATION_CLEARED = OSMTracker.PACKAGE_NAME + ".intent.NOTIFICATION_CLEARED";
	
	/**
	 * Key for extra data "waypoint name" in Intent
	 */
	public final static String INTENT_KEY_NAME = "name";

	/**
	 * Key for extra data "link" in Intent
	 */
	public final static String INTENT_KEY_LINK = "link";
	
	/**
	 * Key for extra data "uuid" in Intent
	 */
	public final static String INTENT_KEY_UUID = "uuid";
	
	/**
	 * Approximation factor for calculating Horizontal Dilution of Precision
	 * from location.getAccuracy(). location.getAccuracy() returns an accuracy measured
	 * in meters, and HDOP is obtained by dividing accuracy by this factor.
	 * The value is totally false (!), but is still useful for certain use case like
	 * track display in JOSM.
	 * See: http://code.google.com/p/osmtracker-android/issues/detail?id=15 
	 */
	public final static int HDOP_APPROXIMATION_FACTOR = 4;
	
}