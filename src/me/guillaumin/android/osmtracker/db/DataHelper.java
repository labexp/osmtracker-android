package me.guillaumin.android.osmtracker.db;

import java.io.File;
import java.text.SimpleDateFormat;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
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
	public static final String EXTENSION_GPX = ".gpx";

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
	 * Track a point into DB.
	 * 
	 * @param trackId
	 *            Id of the track
	 * @param location
	 *            The Location to track
	 */
	public void track(long trackId, Location location) {
		Log.v(TAG, "Tracking (trackId=" + trackId + ") location: " + location);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_TRACK_ID, trackId);
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

		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		contentResolver.insert(Uri.withAppendedPath(trackUri, Schema.TBL_TRACKPOINT + "s"), values);
	}

	/**
	 * Tracks a way point with link
	 * 
	 * @param trackId
	 *            Id of the track
	 * @param location
	 *            Location of waypoint
	 * @param nbSatellites
	 *            Number of satellites used for the location
	 * @param name
	 *            Name of waypoint
	 * @param link
	 *            Link of waypoint
	 * @param uuid 
	 * 			  Unique id of the waypoint
	 */
	public void wayPoint(long trackId, Location location, int nbSatellites, String name, String link, String uuid) {
		Log.v(TAG, "Tracking waypoint '" + name + "', track=" + trackId + ", uuid=" + uuid + ", link='" + link + "', location=" + location);

		// location should not be null, but sometime is.
		// TODO investigate this issue.
		if (location != null) {
			ContentValues values = new ContentValues();
			values.put(Schema.COL_TRACK_ID, trackId);
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
				values.put(Schema.COL_LINK, renameFile(trackId, link, FILENAME_FORMATTER.format(location.getTime())));
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_IGNORE_CLOCK, OSMTracker.Preferences.VAL_GPS_IGNORE_CLOCK)) {
				// Use OS clock
				values.put(Schema.COL_TIMESTAMP, System.currentTimeMillis());
			} else {
				// Use GPS clock
				values.put(Schema.COL_TIMESTAMP, location.getTime());
			}

			Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
			contentResolver.insert(Uri.withAppendedPath(trackUri, Schema.TBL_WAYPOINT + "s"), values);
		}
	}
	
	/**
	 * Updates a waypoint
	 * 
	 * @param trackId
	 *            Id of the track
	 * @param uuid
	 *            Unique ID of the target waypoint
	 * @param name
	 *            New name
	 * @param link
	 *            New link
	 */
	public void updateWayPoint(long trackId, String uuid, String name, String link) {
		Log.v(TAG, "Updating waypoint with uuid '" + uuid + "'. New values: name='" + name + "', link='" + link + "'");
		if (uuid != null) {
			ContentValues values = new ContentValues();
			if (name != null) {
				values.put(Schema.COL_NAME, name);
			}
			if (link != null) {
				values.put(Schema.COL_LINK, link);
			}
			
			Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
			contentResolver.update(Uri.withAppendedPath(trackUri, Schema.TBL_WAYPOINT + "s"), values,
					"uuid = ?", new String[] { uuid });
		}
	}
	
	/**
	 * Stop tracking by making the track inactive
	 * @param trackId Id of the track
	 */
	public void stopTracking(long trackId) {
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_INACTIVE);
		contentResolver.update(trackUri, values, null, null);
	}

	/**
	 * Find the active track ID, if any.
	 * @param cr  {@link ContentResolver} for query
	 * @return  the active track ID, or -1
	 */
	public static long getActiveTrackId(ContentResolver cr) {
		long currentTrackId = -1;
		Cursor ca = cr.query(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE, null, null, null, null);
		if (ca.moveToFirst()) {
			currentTrackId = ca.getLong(ca.getColumnIndex(Schema.COL_ID));
		}
		ca.close();
		return currentTrackId;
	}

	/**
	 * Change the name of this track.
	 * @param trackId Id of the track
	 * @param name  New name of track, or null to clear it
	 * @param cr  Database connection for query
	 */
	public static void setTrackName(long trackId, String name, ContentResolver cr) {
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_NAME, name);
		cr.update(trackUri, values, null, null);		
	}

	/**
	 * Mark the export date/time of this track.
	 * @param trackId Id of the track
	 * @param exportTime Time of export, from {@link System#currentTimeMillis()}
	 * @param cr {@link ContentResolver} for query
	 */
	public static void setTrackExportDate(long trackId, long exportTime, ContentResolver cr) {
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();
		values.put(Schema.COL_EXPORT_DATE, exportTime);
		cr.update(trackUri, values, null, null);		
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
	private String renameFile(Long trackId, String from, String to) {
		File trackDir = DataHelper.getTrackDir(contentResolver, trackId);
		
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

	/**
	 * @param cr Content Resoliver to use
	 * @param trackId Track id
	 * @return A File to the track directory for the target track id.
	 */
	public static File getTrackDir(ContentResolver cr, long trackId) {
		Cursor c = cr.query(
			ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
			null, null, null, null);
	
		c.moveToFirst();
		File trackDir = new File(c.getString(c.getColumnIndex(Schema.COL_DIR)));
		c.close();
		
		return trackDir;
	}	
}
