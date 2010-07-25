package me.guillaumin.android.osmtracker.db;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.gpx.GPXFileWriter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Data helper for dialoging with content resolver and filesystem.
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
	public static final String EXTENSION_3GPP = ".3gpp";

	/**
	 * JPG file extension
	 */
	public static final String EXTENSION_JPG = ".jpg";

	/**
	 * Number of tries to rename a media file for the current track if there are
	 * already a media file of this name.
	 */
	private static final int MAX_RENAME_ATTEMPTS = 20;

	/**
	 * Current File for recording a still picture. Behaves as a dirty 1 level
	 * stack.
	 */
	private File currentImageFile;

	/**
	 * Formatter for various files (GPX, media)
	 */
	public static final SimpleDateFormat FILENAME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	/**
	 * Context
	 */
	private Context context;

	/**
	 * ContentResolver to interact with content provider
	 */
	private ContentResolver contentResolver;

	/**
	 * Directory for storing track.
	 */
	private File trackDir;

	/**
	 * Constructor.
	 * 
	 * @param c
	 *            Application context.
	 */
	public DataHelper(Context c) {
		context = c;
		contentResolver = c.getContentResolver();
	}

	/**
	 * Create a new track: Erase all db data, and creates a track dir.
	 * 
	 * @throws IOException
	 */
	public void createNewTrack() throws IOException {

		// Delete all previous trackpoint & waypoint
		deleteAllData();

		// Create directory for track
		File sdRoot = Environment.getExternalStorageDirectory();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String storageDir = prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR,
				OSMTracker.Preferences.VAL_STORAGE_DIR);
		if (sdRoot.canWrite()) {
			// Create base OSMTracker directory on SD Card
			File osmTrackerDir = new File(sdRoot + storageDir);
			if (!osmTrackerDir.exists()) {
				osmTrackerDir.mkdir();
			}

			// Create track directory
			trackDir = new File(osmTrackerDir + File.separator + FILENAME_FORMATTER.format(new Date()));
			trackDir.mkdir();
			
			// Insert current trackdir in DB for uses by other components
			ContentValues values = new ContentValues();
			values.put(Schema.COL_KEY, Schema.KEY_CONFIG_TRACKDIR);
			values.put(Schema.COL_VALUE, trackDir.getAbsolutePath());
			contentResolver.insert(TrackContentProvider.CONTENT_URI_CONFIG, values);
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
		if (location.hasAltitude()) {
			values.put(Schema.COL_ELEVATION, location.getAltitude());
		}
		if (location.hasAccuracy()) {
			values.put(Schema.COL_ACCURACY, location.getAccuracy());
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_IGNORE_CLOCK, OSMTracker.Preferences.VAL_GPS_IGNORE_CLOCK)) {
			// Use OS clock
			values.put(Schema.COL_TIMESTAMP, System.currentTimeMillis());
		} else {
			// Use GPS clock
			values.put(Schema.COL_TIMESTAMP, location.getTime());
		}

		contentResolver.insert(TrackContentProvider.CONTENT_URI_TRACKPOINT, values);
	}

	/**
	 * Tracks a way point with link
	 * 
	 * @param location
	 *            Location of waypoint
	 * @param nbSatellites
	 *            Number of satellites used for the location
	 * @param name
	 *            Name of waypoint
	 * @param link
	 *            Link of waypoint
	 */
	public void wayPoint(Location location, int nbSatellites, String name, String link, String uuid) {
		Log.v(TAG, "Tracking waypoint '" + name + "', uuid=" + uuid + ", link='" + link + "', location=" + location);

		// location should not be null, but sometime is.
		// TODO investigate this issue.
		if (location != null) {
			ContentValues values = new ContentValues();
			values.put(Schema.COL_LATITUDE, location.getLatitude());
			values.put(Schema.COL_LONGITUDE, location.getLongitude());
			values.put(Schema.COL_NAME, name);
			values.put(Schema.COL_NBSATELLITES, nbSatellites);

			if (uuid != null) {
				values.put(Schema.COL_UUID, uuid);
			}
			
			if (location.hasAltitude()) {
				values.put(Schema.COL_ELEVATION, location.getAltitude());
			}
			if (location.hasAccuracy()) {
				values.put(Schema.COL_ACCURACY, location.getAccuracy());
			}
			if (link != null) {
				// Rename file to match location timestamp
				values.put(Schema.COL_LINK, renameFile(link, FILENAME_FORMATTER.format(location.getTime())));
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_IGNORE_CLOCK, OSMTracker.Preferences.VAL_GPS_IGNORE_CLOCK)) {
				// Use OS clock
				values.put(Schema.COL_TIMESTAMP, System.currentTimeMillis());
			} else {
				// Use GPS clock
				values.put(Schema.COL_TIMESTAMP, location.getTime());
			}


			contentResolver.insert(TrackContentProvider.CONTENT_URI_WAYPOINT, values);
		}
	}
	
	/**
	 * Updates a waypoint
	 * 
	 * @param uuid
	 *            Unique ID of the target waypoint
	 * @param name
	 *            New name
	 * @param link
	 *            New link
	 */
	public void updateWayPoint(String uuid, String name, String link) {
		Log.v(TAG, "Updating waypoint with uuid '" + uuid + "'. New values: name='" + name + "', link='" + link + "'");
		if (uuid != null) {
			ContentValues values = new ContentValues();
			if (name != null) {
				values.put(Schema.COL_NAME, name);
			}
			if (link != null) {
				values.put(Schema.COL_LINK, link);
			}
			contentResolver
					.update(TrackContentProvider.CONTENT_URI_WAYPOINT, values, "uuid = ?", new String[] { uuid });
		}
	}

	/**
	 * Exports current database to a GPX file.
	 */
	public void exportTrackAsGpx() {

		if (trackDir != null) {

			File trackFile = new File(trackDir, FILENAME_FORMATTER.format(new Date()) + EXTENSION_GPX);

			Cursor cTrackPoints = contentResolver.query(TrackContentProvider.CONTENT_URI_TRACKPOINT, null, null, null,
					Schema.COL_TIMESTAMP + " asc");
			Cursor cWayPoints = contentResolver.query(TrackContentProvider.CONTENT_URI_WAYPOINT, null, null, null,
					Schema.COL_TIMESTAMP + " asc");

			try {
				GPXFileWriter.writeGpxFile(context.getResources(), cTrackPoints, cWayPoints, trackFile,
						PreferenceManager.getDefaultSharedPreferences(context));
			} catch (IOException ioe) {
				Log.e(TAG, "Unable to export track: " + ioe.getMessage());
			}

			cTrackPoints.close();
			cWayPoints.close();
			
			deleteAllData();

		}
	}

	/**
	 * Getter for trackDir
	 * 
	 * @return the tracking directory on external storage.
	 */
	public File getTrackDir() {
		return trackDir;
	}

	/**
	 * @return A new File to record an audio way point, inside the track
	 *         directory.
	 */
	public File getNewAudioFile() {
		return new File(trackDir + File.separator + FILENAME_FORMATTER.format(new Date()) + EXTENSION_3GPP);
	}

	/**
	 * @return A new File to record a still image, inside the track directory.
	 */
	public File pushImageFile() {
		currentImageFile = new File(trackDir + File.separator + FILENAME_FORMATTER.format(new Date()) + EXTENSION_JPG);
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
	 * Delete all data in ContentProvider
	 */
	private void deleteAllData() {
		contentResolver.delete(TrackContentProvider.CONTENT_URI_TRACKPOINT, null, null);
		contentResolver.delete(TrackContentProvider.CONTENT_URI_WAYPOINT, null, null);
		contentResolver.delete(TrackContentProvider.CONTENT_URI_CONFIG, null, null);
	}
	
	/**
	 * Renames a file inside track directory, keeping the extension
	 * 
	 * @param from
	 *            File to rename (Ex: "abc.png")
	 * @param to
	 *            Filename to use for new name (Ex: "def")
	 * @return Renamed filename (Ex: "def.png")
	 */
	private String renameFile(String from, String to) {
		String ext = from.substring(from.lastIndexOf(".") + 1, from.length());
		File origin = new File(trackDir + File.separator + from);
		File target = new File(trackDir + File.separator + to + "." + ext);
		// Check & manages if there is already a file with this name
		for (int i = 0; i < MAX_RENAME_ATTEMPTS && target.exists(); i++) {
			target = new File(trackDir + File.separator + to + i + "." + ext);
		}
		origin.renameTo(target);
		return target.getName();

	}

}
