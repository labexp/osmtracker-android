package me.guillaumin.android.osmtracker.db;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
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

	private static final String TAG = DataHelper.class.getSimpleName();
	
	/**
	 * GPX file extension.
	 */
	private static final String EXTENSION_GPX = ".gpx";
	
	/**
	 * 3GPP extension
	 */
	private static final String EXTENSION_3GPP = ".3gpp";

	/**
	 * JPG file extension
	 */
	private static final String EXTENSION_JPG = ".jpg";
	
	/**
	 * Database name.
	 */
	private static final String DB_NAME = OSMTracker.class.getSimpleName();
	
	/**
	 * Current File for recording a still picture.
	 * Behaves as a dirty 1 level stack.
	 */
	private File currentImageFile;

	/**
	 * SQL for creating table TRACKPOINT
	 */
	private static final String SQL_CREATE_TABLE_TRACKPOINT = ""
			+ "create table trackpoint ("
			+ Schema.COL_ID + " integer primary key autoincrement,"
			+ Schema.COL_LATITUDE + " double not null,"
			+ Schema.COL_LONGITUDE + " double not null,"
			+ Schema.COL_ELEVATION + " double null,"
			+ Schema.COL_ACCURACY + " double null,"
			+ Schema.COL_TIMESTAMP + " long not null" + ")";

	/**
	 * SQL for creating table WAYPOINT
	 */
	private static final String SQL_CREATE_TABLE_WAYPOINT = ""
			+ "create table waypoint ("
			+ Schema.COL_ID + " integer primary key autoincrement,"
			+ Schema.COL_LATITUDE + " double not null,"
			+ Schema.COL_LONGITUDE + " double not null,"
			+ Schema.COL_ELEVATION + " double null,"
			+ Schema.COL_ACCURACY + " double null,"
			+ Schema.COL_TIMESTAMP + " long not null,"
			+ Schema.COL_NAME + " text," 
			+ Schema.COL_LINK + " text" + ")";

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

	/**
	 * Constructor.
	 * @param c Application context.
	 */
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
		String storageDir = prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR,
				OSMTracker.Preferences.VAL_STORAGE_DIR);
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
		Log.v(TAG, "Tracking location: " + location);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_LATITUDE, location.getLatitude());
		values.put(Schema.COL_LONGITUDE, location.getLongitude());
		values.put(Schema.COL_TIMESTAMP, location.getTime());
		if (location.hasAltitude()) {
			values.put(Schema.COL_ELEVATION, location.getAltitude());
		}
		if (location.hasAccuracy()) {
			values.put(Schema.COL_ACCURACY, location.getAccuracy());
		}

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
		
		// location should not be null, but sometime is.
		// TODO investigate this issue.
		if (location != null ) {
			ContentValues values = new ContentValues();
			values.put(Schema.COL_LATITUDE, location.getLatitude());
			values.put(Schema.COL_LONGITUDE, location.getLongitude());
			values.put(Schema.COL_TIMESTAMP, location.getTime());
			if (location.hasAltitude()) {
				values.put(Schema.COL_ELEVATION, location.getAltitude());
			}
			if (location.hasAccuracy()) {
				values.put(Schema.COL_ACCURACY, location.getAccuracy());
			}
			values.put(Schema.COL_NAME, name);
			if (link != null ) {
				values.put(Schema.COL_LINK, link);
			}
			
			database.insert(Schema.TBL_WAYPOINT, null, values);
		}
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
	 * @return A {@link Cursor} to the waypoints in db or null if db is closed
	 */
	public Cursor getWaypointsCursor() {
		if (database != null && database.isOpen() ) {
			// Query for way points
			Cursor cWayPoints = database.query(Schema.TBL_WAYPOINT, new String[] {
					Schema.COL_ID, Schema.COL_LONGITUDE, Schema.COL_LATITUDE, Schema.COL_LINK,
					Schema.COL_ELEVATION, Schema.COL_ACCURACY, Schema. COL_TIMESTAMP, Schema.COL_NAME }, null, null,
					null, null, Schema.COL_TIMESTAMP + " asc");
			cWayPoints.moveToFirst();
			
			return cWayPoints;
		} else {
			return null;
		}
	}
	
	/**
	 * @return A {@link Cursor} to the trackpoints in db, or null if db is closed
	 */
	public Cursor getTrackpointsCursor() {
		if (database != null && database.isOpen() ) {
			// Query for track points
			Cursor cTrackPoints = database.query(Schema.TBL_TRACKPOINT, new String[] {
					Schema.COL_ID, Schema.COL_LONGITUDE, Schema.COL_LATITUDE,
					Schema.COL_ELEVATION, Schema.COL_ACCURACY, Schema.COL_TIMESTAMP }, null, null,
					null, null, Schema.COL_TIMESTAMP + " asc");
			cTrackPoints.moveToFirst();
			
			return cTrackPoints;
		} else {
			return null;
		}
	}
	
	/**
	 * Exports current database to a GPX file.
	 */
	public void exportTrackAsGpx() {

		if (trackDir != null) {
			File trackFile = new File(trackDir, fileNameFormatter
					.format(new Date())
					+ EXTENSION_GPX);

			Cursor cTrackPoints = getTrackpointsCursor();
			Cursor cWayPoints = getWaypointsCursor();
			
			try {
				GPXFileWriter.writeGpxFile(context.getResources().getString(R.string.gpx_track_name), cTrackPoints, cWayPoints, trackFile);
			} catch (IOException ioe) {
				Log.e(TAG, "Unable to export track: " + ioe.getMessage());
			}
			
			cTrackPoints.close();
			cWayPoints.close();
			
			database.close();
			context.deleteDatabase(DB_NAME);
		}
	}


	/**
	 * Getter for trackDir
	 * @return the tracking directory on external storage.
	 */
	public File getTrackDir() {
		return trackDir;
	}
	
	/**
	 * @return A new File to record an audio way point, inside the track directory.
	 */
	public File getNewAudioFile() {
		return new File(trackDir + File.separator + fileNameFormatter.format(new Date()) + EXTENSION_3GPP);
	}
	
	/**
	 * @return A new File to record a still image, inside the track directory.
	 */
	public File pushImageFile() {
		currentImageFile = new File(trackDir + File.separator + fileNameFormatter.format(new Date()) + EXTENSION_JPG);
		return currentImageFile;
	}
	
	/**
	 * @return The current image file, and removes it.
	 */
	public File popImageFile() {
		File imageFile = new File(currentImageFile.getAbsolutePath());
		currentImageFile = null;
		return imageFile;
	}
	
	/**
	 * Represents XML Schema.
	 */
	public static final class Schema {
		public static final String TBL_TRACKPOINT = "trackpoint";
		public static final String TBL_WAYPOINT = "waypoint";
		public static final String COL_ID = "_id";
		public static final String COL_LONGITUDE = "longitude";
		public static final String COL_LATITUDE = "latitude";
		public static final String COL_ELEVATION = "elevation";
		public static final String COL_ACCURACY = "accuracy";
		public static final String COL_TIMESTAMP = "point_timestamp";
		public static final String COL_NAME = "name";
		public static final String COL_LINK = "link";
	}
	

}
