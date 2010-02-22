package me.guillaumin.android.osmtracker.db;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.OSMTracker;
import me.guillaumin.android.osmtracker.gpx.GPXFileWriter;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Data helper for storing track in DB and
 * exporting in GPX.
 * For the moment only 1 database is used, and deleted
 * once done.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class DataHelper {

	private static final String TAG = DataHelper.class.getName();
	
	/**
	 * GPX file extension.
	 */
	private static final String EXTENSION_GPX = ".gpx";
	
	/**
	 * 3GPP extension
	 */
	private static final String EXTENSION_3GPP = ".3gpp";

	/**
	 * Database name.
	 */
	private static final String DB_NAME = OSMTracker.class.getSimpleName();

	/**
	 * SQL for creating table TRACKPOINT
	 */
	private static final String SQL_CREATE_TABLE_TRACKPOINT = ""
			+ "create table trackpoint ("
			+ " id integer primary key autoincrement," + Schema.COL_LATITUDE
			+ " double not null," + Schema.COL_LONGITUDE + " double not null,"
			+ Schema.COL_ELEVATION + " double not null," + Schema.COL_TIMESTAMP
			+ " long not null" + ")";

	/**
	 * SQL for creating table WAYPOINT
	 */
	private static final String SQL_CREATE_TABLE_WAYPOINT = ""
			+ "create table waypoint ("
			+ " id integer primary key autoincrement," + Schema.COL_LATITUDE
			+ " double not null," + Schema.COL_LONGITUDE + " double not null,"
			+ Schema.COL_ELEVATION + " double not null," + Schema.COL_TIMESTAMP
			+ " long not null," + Schema.COL_NAME + " text," + Schema.COL_LINK
			+ " text" + ")";

	private static final SimpleDateFormat fileNameFormatter = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm-ss");

	/**
	 * Context required to interact with DBs.
	 */
	private Context context;

	/**
	 * Database.
	 */
	private SQLiteDatabase database;

	/**
	 * Directory for storing track.
	 */
	private File trackDir;

	public DataHelper(Context c) {
		Log.v(TAG, "Creating a new " + DataHelper.class.getSimpleName());
		context = c;
	}

	/**
	 * Create a new track:<br />
	 * <ul>
	 * 	<li>Creates a DB</li>
	 *  <il>Create a directory for track files</li>
	 * </ul>
	 * @throws IOException
	 */
	public void createNewTrack() throws IOException {
		database = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE,
				null);

		// Creatae database and tables
		database.execSQL("drop table if exists " + Schema.TBL_TRACKPOINT);
		database.execSQL(SQL_CREATE_TABLE_TRACKPOINT);
		database.execSQL("drop table if exists " + Schema.TBL_WAYPOINT);
		database.execSQL(SQL_CREATE_TABLE_WAYPOINT);

		// Create directory for track
		File sdRoot = Environment.getExternalStorageDirectory();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String storageDir = prefs.getString(OSMTracker.PREF_STORAGE_DIR,
				OSMTracker.class.getSimpleName());
		if (sdRoot.canWrite()) {
			// Create base OSMTracker directory on SD Card
			File osmTrackerDir = new File(sdRoot + storageDir);
			if (!osmTrackerDir.exists()) {
				osmTrackerDir.mkdir();
			}

			// Create track directory
			trackDir = new File(osmTrackerDir + File.separator
					+ fileNameFormatter.format(new Date()));
			trackDir.mkdir();
		} else {
			throw new IOException(context.getResources().getString(R.string.error_externalstorage_not_writable));
		}

	}

	/**
	 * Track a point into DB.
	 * 
	 * @param location
	 *            The Location to track
	 */
	public void track(Location location) {
		ContentValues values = new ContentValues();
		values.put(Schema.COL_LATITUDE, location.getLatitude());
		values.put(Schema.COL_LONGITUDE, location.getLongitude());
		values.put(Schema.COL_ELEVATION, location.getAltitude());
		values.put(Schema.COL_TIMESTAMP, location.getTime());

		database.insert(Schema.TBL_TRACKPOINT, null, values);
	}

	/**
	 * Tracks a way point with link
	 * @param location Location of waypoint
	 * @param name Name of waypoint
	 * @param link Link of waypoint
	 */
	public void wayPoint(Location location, String name, String link) {
		Log.v(TAG, "Tracking waypoing '" + name + "', link='" + link + "', location=" + location);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_LATITUDE, location.getLatitude());
		values.put(Schema.COL_LONGITUDE, location.getLongitude());
		values.put(Schema.COL_ELEVATION, location.getAltitude());
		values.put(Schema.COL_TIMESTAMP, location.getTime());
		values.put(Schema.COL_NAME, name);
		if (link != null ) {
			values.put(Schema.COL_LINK, link);
		}
		
		database.insert(Schema.TBL_WAYPOINT, null, values);
	}
	
	/**
	 * Tracks a waypoint.
	 * @param location Location of waypoint
	 * @param name Name of waypoint.
	 */
	public void wayPoint(Location location, String name) {
		wayPoint(location, name, null);
	}
	
	/**
	 * Exports current database to a GPX file.
	 */
	public void exportTrackAsGpx() {
		PreferenceManager.getDefaultSharedPreferences(context);

		if (trackDir != null) {
			File trackFile = new File(trackDir, fileNameFormatter
					.format(new Date())
					+ EXTENSION_GPX);

			// Query for track points
			Cursor cTrackPoints = database.query(Schema.TBL_TRACKPOINT, new String[] {
					Schema.COL_LONGITUDE, Schema.COL_LATITUDE,
					Schema.COL_ELEVATION, Schema.COL_TIMESTAMP }, null, null,
					null, null, Schema.COL_TIMESTAMP + " asc");
			cTrackPoints.moveToFirst();
			
			// Query for way points
			Cursor cWayPoints = database.query(Schema.TBL_WAYPOINT, new String[] {
					Schema.COL_LONGITUDE, Schema.COL_LATITUDE, Schema.COL_LINK,
					Schema.COL_ELEVATION, Schema. COL_TIMESTAMP, Schema.COL_NAME }, null, null,
					null, null, Schema.COL_TIMESTAMP + " asc");
			cWayPoints.moveToFirst();

			try {
				GPXFileWriter.writeGpxFile(context.getResources().getString(R.string.gpx_track_name), cTrackPoints, cWayPoints, trackFile);
			} catch (IOException ioe) {
				Log.e(TAG, "Unable to export track: " + ioe.getMessage());
			}
			
			cTrackPoints.close();
			cWayPoints.close();
			
			database.close();
		}
	}


	public File getTrackDir() {
		return trackDir;
	}
	
	/**
	 * @return A new File to record an audio way point, inside the track directory.
	 */
	public File getNewAudioFile() {
		return new File(trackDir + File.separator + fileNameFormatter.format(new Date()) + EXTENSION_3GPP);
	}
	
	public static final class Schema {
		public static final String TBL_TRACKPOINT = "trackpoint";
		public static final String TBL_WAYPOINT = "waypoint";
		public static final String COL_LONGITUDE = "longitude";
		public static final String COL_LATITUDE = "latitude";
		public static final String COL_ELEVATION = "elevation";
		public static final String COL_TIMESTAMP = "point_timestamp";
		public static final String COL_NAME = "name";
		public static final String COL_LINK = "link";
	}
	

}
