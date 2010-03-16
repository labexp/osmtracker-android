package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;

/**
 * Main activity. Not used for the moment.
 * 
 * @author Nicolas Guillaumn
 * 
 */
public class OSMTracker extends Activity {

	/**
	 * Stores settings keys and default values.
	 */
	public static final class Preferences {
		/**
		 * Property names
		 */
		public final static String KEY_STORAGE_DIR = "logging.storage.dir";
		public final static String KEY_VOICEREC_DURATION = "voicerec.duration";
		public final static String KEY_UI_LEGACYBACK = "ui.legacyback";
		public final static String KEY_UI_THEME = "ui.theme";
		public final static String KEY_GPS_OSSETTINGS = "gps.ossettings";
		public final static String KEY_GPS_CHECKSTARTUP = "gps.checkstartup";

		/**
		 * Default values
		 */
		public final static String VAL_STORAGE_DIR = "/osmtracker";
		public final static String VAL_VOICEREC_DURATION = "2";
		public final static boolean VAL_UI_LEGACYBACK = false;
		public final static String VAL_UI_THEME = "@android:style/Theme";
		public final static boolean VAL_GPS_CHECKSTARTUP = true;
	};

	/**
	 * Intent for tracking a waypoint
	 */
	public final static String INTENT_TRACK_WP = "me.guillaumin.android.osmtracker.intent.TRACK_WP";

	/**
	 * Intent to start tracking
	 */
	public final static String INTENT_START_TRACKING = "me.guillaumin.android.osmtracker.intent.START_TRACKING";

	/**
	 * Intent to stop tracking
	 */
	public final static String INTENT_STOP_TRACKING = "me.guillaumin.android.osmtracker.intent.STOP_TRACKING";

	/**
	 * Intent fired when the service go to background and should start to notify user. 
	 */
	public final static String INTENT_START_NOTIFY_BACKGROUND = "me.guillaumin.android.osmtracker.intent.START_NOTIFY_BACKGROUND";
	
	/**
	 * Intent fired when the UI got back and the service should stop notifying the user of background job.
	 */
	public final static String INTENT_STOP_NOTIFY_BACKGROUND = "me.guillaumin.android.osmtracker.intent.STOP_NOTIFY_BACKGROUND";
	
	/**
	 * Intent fired when the notification are cleared by the user.
	 */
	public final static String INTENT_NOTIFICATION_CLEARED = "me.guillaumin.android.osmtracker.intent.NOTIFICATION_CLEARED";
	
	/**
	 * Key for extra data "waypoint name" in Intent
	 */
	public final static String INTENT_KEY_NAME = "name";

	/**
	 * Key for extra data "link" in Intent
	 */
	public final static String INTENT_KEY_LINK = "link";

}