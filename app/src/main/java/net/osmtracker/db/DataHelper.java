package net.osmtracker.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	 * GPX Files MIME standard for sharing
	 */
	public static final String MIME_TYPE_GPX = "application/gpx+xml";

	/**
	 * APP sign plus FileProvider = authority
	 */
	public static final String FILE_PROVIDER_AUTHORITY = "net.osmtracker.fileprovider";

	/**
	 * Number of tries to rename a media file for the current track if there are
	 * already a media file of this name.
	 */
	private static final int MAX_RENAME_ATTEMPTS = 20;
	
	/**
	 * valid range for azimuth angles.
	 */
	public static final float AZIMUTH_MIN = 0;
	public static final float AZIMUTH_MAX = 360;
	public static final float AZIMUTH_INVALID = -1;

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
	 *				Application context.
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
	 * @param azimuth
	 * 			  azimuth angle in degrees (0-360deg) of the track point. if it is outside the given range it will be set null.
	 * @param accuracy
	 * 			  accuracy of the compass reading (as SensorManager.SENSOR_STATUS_ACCURACY*),
	 * 			  ignored if azimuth is invalid.
	 * @param pressure
	 *            atmospheric pressure
	 */
	public void track(long trackId, Location location, float azimuth, int accuracy, float pressure) {
		Log.v(TAG, "Tracking (trackId=" + trackId + ") location: " + location + " azimuth: " + azimuth + ", accuracy: " + accuracy);
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
		values.put(TrackContentProvider.Schema.COL_LATITUDE, location.getLatitude());
		values.put(TrackContentProvider.Schema.COL_LONGITUDE, location.getLongitude());
		if (location.hasAltitude()) {
			values.put(TrackContentProvider.Schema.COL_ELEVATION, location.getAltitude());
		}
		if (location.hasAccuracy()) {
			values.put(TrackContentProvider.Schema.COL_ACCURACY, location.getAccuracy());
		}
		if (location.hasSpeed()) {
			values.put(TrackContentProvider.Schema.COL_SPEED, location.getSpeed());
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_IGNORE_CLOCK, OSMTracker.Preferences.VAL_GPS_IGNORE_CLOCK)) {
			// Use OS clock
			values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
		} else {
			// Use GPS clock
			values.put(TrackContentProvider.Schema.COL_TIMESTAMP, location.getTime());
		}

		if (azimuth >= AZIMUTH_MIN && azimuth < AZIMUTH_MAX) {
			values.put(TrackContentProvider.Schema.COL_COMPASS, azimuth);
			values.put(TrackContentProvider.Schema.COL_COMPASS_ACCURACY, accuracy);
		}

		if (pressure != 0) {
			values.put(TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE, pressure);
		}

		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		contentResolver.insert(Uri.withAppendedPath(trackUri, TrackContentProvider.Schema.TBL_TRACKPOINT + "s"), values);
	}

	/**
	 * Tracks a way point with link
	 * 
	 * @param trackId
	 *				Id of the track
	 * @param location
	 *				Location of waypoint
	 * @param name
	 *				Name of waypoint
	 * @param link
	 *				Link of waypoint
	 * @param uuid 
	 * 			    Unique id of the waypoint
	 * @param azimuth
	 * 			    azimuth angle in degrees (0-360deg) of the way point. if it is outside the given range it will be set null.
	 * @param accuracy
	 * 			  accuracy of the compass reading (as SensorManager.SENSOR_STATUS_ACCURACY*),
	 * 			  ignored if azimuth is invalid.
	 */
	public void wayPoint(long trackId, Location location, String name, String link, String uuid, float azimuth, int accuracy, float pressure) {
		Log.d(TAG, "Tracking waypoint '" + name + "', track=" + trackId + ", uuid=" + uuid
				+ ", nbSatellites=" + location.getExtras().getInt("satellites")
				+ ", link='"+ link + "', location=" + location + ", azimuth=" + azimuth
				+ ", accuracy=" + accuracy);

		// location should not be null, but sometime is.
		// TODO investigate this issue.
		if (location != null) {
			ContentValues values = new ContentValues();
			values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
			values.put(TrackContentProvider.Schema.COL_LATITUDE, location.getLatitude());
			values.put(TrackContentProvider.Schema.COL_LONGITUDE, location.getLongitude());
			values.put(TrackContentProvider.Schema.COL_NAME, name);
			values.put(TrackContentProvider.Schema.COL_NBSATELLITES, location.getExtras().getInt("satellites"));

			if (uuid != null) {
				values.put(TrackContentProvider.Schema.COL_UUID, uuid);
			}
			if (location.hasAltitude()) {
				values.put(TrackContentProvider.Schema.COL_ELEVATION, location.getAltitude());
			}
			if (location.hasAccuracy()) {
				values.put(TrackContentProvider.Schema.COL_ACCURACY, location.getAccuracy());
			}
			if (link != null) {
				// Rename file to match location timestamp
				values.put(TrackContentProvider.Schema.COL_LINK, renameFile(trackId, link, FILENAME_FORMATTER.format(location.getTime())));
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_IGNORE_CLOCK, OSMTracker.Preferences.VAL_GPS_IGNORE_CLOCK)) {
				// Use OS clock
				values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
			} else {
				// Use GPS clock
				values.put(TrackContentProvider.Schema.COL_TIMESTAMP, location.getTime());
			}
			
			//add compass if valid
			if (azimuth >= AZIMUTH_MIN && azimuth < AZIMUTH_MAX) {
				values.put(TrackContentProvider.Schema.COL_COMPASS, azimuth);
				values.put(TrackContentProvider.Schema.COL_COMPASS_ACCURACY, accuracy);
			}

			if (pressure != 0) {
				values.put(TrackContentProvider.Schema.COL_ATMOSPHERIC_PRESSURE, pressure);
			}

			Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
			contentResolver.insert(Uri.withAppendedPath(trackUri, TrackContentProvider.Schema.TBL_WAYPOINT + "s"), values);
		}
	}
	
	/**
	 * Updates a waypoint
	 * 
	 * @param trackId
	 *				Id of the track
	 * @param uuid
	 *				Unique ID of the target waypoint
	 * @param name
	 *				New name
	 * @param link
	 *				New link
	 */
	public void updateWayPoint(long trackId, String uuid, String name, String link) {
		Log.v(TAG, "Updating waypoint with uuid '" + uuid + "'. New values: name='" + name + "', link='" + link + "'");
		if (uuid != null) {
			ContentValues values = new ContentValues();
			if (name != null) {
				values.put(TrackContentProvider.Schema.COL_NAME, name);
			}
			if (link != null) {
				values.put(TrackContentProvider.Schema.COL_LINK, link);
			}
			
			Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
			contentResolver.update(Uri.withAppendedPath(trackUri, TrackContentProvider.Schema.TBL_WAYPOINT + "s"), values,
					"uuid = ?", new String[] { uuid });
		}
	}
	
	/**
	 * Deletes a waypoint
	 * 
	 * @param uuid
	 *				Unique ID of the target waypoint
	 */
	public void deleteWayPoint(String uuid) {
		Log.v(TAG, "Deleting waypoint with uuid '" + uuid);
		if (uuid != null) {
			contentResolver.delete(Uri.withAppendedPath(TrackContentProvider.CONTENT_URI_WAYPOINT_UUID, uuid), null, null);
		}
	}
	
	
	/**
	 * Stop tracking by making the track inactive
	 * @param trackId Id of the track
	 */
	public void stopTracking(long trackId) {
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_INACTIVE);
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
			currentTrackId = ca.getLong(ca.getColumnIndex(TrackContentProvider.Schema.COL_ID));
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
		values.put(TrackContentProvider.Schema.COL_NAME, name);
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
		values.put(TrackContentProvider.Schema.COL_EXPORT_DATE, exportTime);
		cr.update(trackUri, values, null, null);
	}
	
	public static void setTrackUploadDate(long trackId, long uploadTime, ContentResolver cr) {
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE, uploadTime);
		cr.update(trackUri, values, null, null);		
	}

	/**
	 * Renames a file inside track directory, keeping the extension
	 * 
	 * @param from
	 *				File to rename (Ex: "abc.png")
	 * @param to
	 *				Filename to use for new name (Ex: "def")
	 * @return Renamed filename (Ex: "def.png")
	 */
	private String renameFile(Long trackId, String from, String to) {
		// If all goes terribly wrong and we can't rename the file,
		// we will return the original file name we were given
		String _return = from;
		
		File trackDir = getTrackDirectory(trackId, context);
		
		String ext = from.substring(from.lastIndexOf(".") + 1, from.length());
		File origin = new File(trackDir + File.separator + from);
		
		// No point in trying to rename the file unless it exist
		if (origin.exists()) {
			File target = new File(trackDir + File.separator + to + "." + ext);
			// Check & manages if there is already a file with this name
			for (int i = 0; i < MAX_RENAME_ATTEMPTS && target.exists(); i++) {
				target = new File(trackDir + File.separator + to + i + "." + ext);
			}
		
			origin.renameTo(target);
			_return = target.getName(); 
		}
		
		return _return;
	}

	/**
	 * @param cr Content Resolver to use
	 * @param trackId Track id
	 * @return A File to the track directory for the target track id.
	 */
	public static File getTrackDirFromDB(ContentResolver cr, long trackId) {
		File trackDir = null;
		Cursor c = cr.query(
			ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
			null, null, null, null);
	
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			@SuppressWarnings("deprecation")
			String trackPath = c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_DIR));
			if (trackPath != null) {
				trackDir = new File(trackPath);
			}
		}
		if (c != null && !c.isClosed()) {
			c.close();
			c = null;
		}
		
		return trackDir;
	}
	
	/**
	 * Generate a string of the directory path to external storage for the track id provided 
	 * @param trackId Track id
	 * @param context
	 * @return A the path where this track should store its files
	 */
	public static File getTrackDirectory(long trackId, Context context) {
		File _return = null;
		
		String trackStorageDirectory = context.getExternalFilesDir(null)
		+ OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator + "track" + trackId;
		
		_return = new File(trackStorageDirectory);		
		return _return;
	}

	/* method not in use. TODO: delete code.
	public static File getGPXTrackFile(long trackId, ContentResolver contentResolver, Context context) {

		String trackName = getTrackNameInDB(trackId, contentResolver);

		File sdRoot = Environment.getExternalStorageDirectory();

		// The location where the user has specified gpx files and associated content to be written
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String userGPXExportDirectoryName = prefs.getString(
				OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);

		// Build storage track path for file creation
		String completeGPXTrackPath = sdRoot + userGPXExportDirectoryName.trim() +
				File.separator + trackName.trim()  + File.separator +
				trackName.trim() + DataHelper.EXTENSION_GPX;

		return new File(completeGPXTrackPath);
	}
	*/

	public static String getTrackNameInDB(long trackId, ContentResolver contentResolver) {
		String trackName = "";
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		Cursor cursor = contentResolver.query(trackUri, null, null,
				null, null);
		if(cursor != null && cursor.moveToFirst()) {
			trackName = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
			cursor.close();
		}

		return trackName;
	}

	/**
	 *
	 * @param startDate
	 * @return
	 */
	public Track getTrackByStartDate(Date startDate) {
		// Get the name of the track with the received start date
		String selection = TrackContentProvider.Schema.COL_START_DATE + " = ?";
		String[] args = {String.valueOf(startDate.getTime())};
		Cursor cursor = context.getContentResolver().query(
				TrackContentProvider.CONTENT_URI_TRACK, null, selection, args,
				null);
		Track track = null;
		if(cursor != null && cursor.moveToFirst()){
			//This is due the build method. (TODO: a constructor with c as param needed in Track)
			long trackId = cursor.getLong(
					cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID));
			track = Track.build(trackId, cursor, contentResolver, true);
		}
		return track;
	}

	//TODO: Fix this method. I suspect the query is not OK.
	// What happens if trackId is not valid?
	public Track getTrackById(long trackId) {
		Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(
				TrackContentProvider.CONTENT_URI_TRACK, trackId), null, null,
				null, null);
		Log.d(TAG, "Count of elements in cursor:" + c.getCount());

		c.moveToFirst();
		Track track = Track.build(trackId, c, contentResolver, true);
		c.close();
		return track;

	}

	public List<Integer> getWayPointIdsOfTrack(long trackId) {
		List<Integer> out = new ArrayList<Integer>();
		// constant for the column track Id
		String[] mProjection = { TrackContentProvider.Schema.COL_ID };

		Cursor cWayPoints = contentResolver.query( TrackContentProvider.waypointsUri(trackId),
				mProjection, null, null,
				TrackContentProvider.Schema.COL_TIMESTAMP + " asc");

		Log.d(TAG, "Count of elements in cursor:" + cWayPoints.getCount());
		for(cWayPoints.moveToFirst(); !cWayPoints.isAfterLast(); cWayPoints.moveToNext()) {
			out.add(cWayPoints.getInt(
					cWayPoints.getColumnIndex(TrackContentProvider.Schema.COL_ID)));
		}
		cWayPoints.close();

		Log.d(TAG, "Count of elements in returned list:" + out.size());

		return out;
	}

	public WayPoint getWayPointById(Integer wayPointId) {
		WayPoint wpt = null;

		Cursor cWayPoint = contentResolver.query(
				TrackContentProvider.waypointUri(wayPointId),
				null, null, null, null);
		Log.d(TAG, "Count of elements in cursor (expected 1): "
				+ cWayPoint.getCount());

		cWayPoint.moveToFirst();
		wpt = new WayPoint(cWayPoint);
		return wpt;
	}

	public List<Integer> getTrackPointIdsOfTrack(long trackId) {
		List<Integer> out = new ArrayList<Integer>();
		// constant for the column track Id
		String[] mProjection = { TrackContentProvider.Schema.COL_ID };

		Cursor cTrackPoints = contentResolver.query( TrackContentProvider.trackPointsUri(trackId),
				mProjection, null, null,
				TrackContentProvider.Schema.COL_TIMESTAMP + " asc");

		Log.d(TAG, "Count of elements in cTrackPoints:" + cTrackPoints.getCount());
		for(cTrackPoints.moveToFirst(); !cTrackPoints.isAfterLast(); cTrackPoints.moveToNext()) {
			out.add(cTrackPoints.getInt(
					cTrackPoints.getColumnIndex(TrackContentProvider.Schema.COL_ID)));
		}
		cTrackPoints.close();
		Log.d(TAG, "Count of elements in returned list:" + out.size());

		return out;
	}

	public TrackPoint getTrackPointById(Integer trackPointId) {
		TrackPoint trkpt = null;

		Cursor cTrackPoint = contentResolver.query(
				TrackContentProvider.trackpointUri(trackPointId),
				null, null, null, null);
		Log.d(TAG, "Count of elements in cursor (expected 1): "
				+ cTrackPoint.getCount());

		cTrackPoint.moveToFirst();
		trkpt = new TrackPoint(cTrackPoint);
		return trkpt;
	}

}
