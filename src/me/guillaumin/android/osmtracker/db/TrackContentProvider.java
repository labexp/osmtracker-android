package me.guillaumin.android.osmtracker.db;

import java.nio.channels.UnsupportedAddressTypeException;

import me.guillaumin.android.osmtracker.OSMTracker;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Content provider for track data, using Android
 * {@link ContentProvider} mechanism.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackContentProvider extends ContentProvider {

	private static final String TAG = TrackContentProvider.class.getSimpleName();

	/**
	 * Authority for Uris
	 */
	public static final String AUTHORITY = OSMTracker.class.getPackage().getName() + ".provider";

	/**
	 * Uri for track
	 */
	public static final Uri CONTENT_URI_TRACK = Uri.parse("content://" + AUTHORITY + "/" + Schema.TBL_TRACK);

	/**
	 * Uri Matcher
	 */
	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		uriMatcher.addURI(AUTHORITY, Schema.TBL_TRACK, Schema.URI_CODE_TRACK);
		uriMatcher.addURI(AUTHORITY, Schema.TBL_TRACK + "/#", Schema.URI_CODE_TRACK_ID);
		uriMatcher.addURI(AUTHORITY, Schema.TBL_TRACK + "/#/" + Schema.TBL_WAYPOINT + "s", Schema.URI_CODE_TRACK_WAYPOINTS);
		uriMatcher.addURI(AUTHORITY, Schema.TBL_TRACK + "/#/" + Schema.TBL_TRACKPOINT + "s", Schema.URI_CODE_TRACK_TRACKPOINTS);
	}
	
	/**
	 * @param trackId target track id
	 * @return Uri for the waypoints of the track 
	 */
	public static final Uri waypointsUri(long trackId) {
		return Uri.withAppendedPath(
				ContentUris.withAppendedId(CONTENT_URI_TRACK, trackId),
				Schema.TBL_WAYPOINT + "s" );
	}
	
	/**
	 * @param trackId target track id
	 * @return Uri for the trackpoints of the track 
	 */
	public static final Uri trackPointsUri(long trackId) {
		return Uri.withAppendedPath(
				ContentUris.withAppendedId(CONTENT_URI_TRACK, trackId),
				Schema.TBL_TRACKPOINT + "s" );		
	}

	/**
	 * Database Helper
	 */
	private DatabaseHelper dbHelper;

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.v(TAG, "delete(), uri=" + uri);

		int count;
		// Select which data type to delete
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACK:
			count = dbHelper.getWritableDatabase().delete(Schema.TBL_TRACK, selection, selectionArgs);
			break;
		case Schema.URI_CODE_TRACK_ID:
				// the URI matches a specific track, delete all related entities
				String trackId = Long.toString(ContentUris.parseId(uri));
				dbHelper.getWritableDatabase().delete(Schema.TBL_WAYPOINT, Schema.COL_TRACK_ID + " = ?", new String[] {trackId});
				dbHelper.getWritableDatabase().delete(Schema.TBL_TRACKPOINT, Schema.COL_TRACK_ID + " = ?", new String[] {trackId});
				count = dbHelper.getWritableDatabase().delete(Schema.TBL_TRACK, Schema.COL_ID + " = ?", new String[] {trackId});
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		Log.v(TAG, "getType(), uri=" + uri);

		// Select wich type to return
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACK_TRACKPOINTS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_TRACKPOINT;
		case Schema.URI_CODE_TRACK_WAYPOINTS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_WAYPOINT;
		case Schema.URI_CODE_TRACK:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_TRACK;
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.v(TAG, "insert(), uri=" + uri + ", values=" + values.toString());

		// Select which data type to insert
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACK_TRACKPOINTS:
			// Check that mandatory columns are present.
			if (values.containsKey(Schema.COL_TRACK_ID) && values.containsKey(Schema.COL_LONGITUDE)
					&& values.containsKey(Schema.COL_LATITUDE) && values.containsKey(Schema.COL_TIMESTAMP)) {

				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_TRACKPOINT, null, values);
				if (rowId > 0) {
					Uri trackpointUri = ContentUris.withAppendedId(uri, rowId);
					getContext().getContentResolver().notifyChange(trackpointUri, null);
					return trackpointUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_LONGITUDE + ", "
						+ Schema.COL_LATITUDE + ", " + Schema.COL_TIMESTAMP);
			}
			break;
		case Schema.URI_CODE_TRACK_WAYPOINTS:
			// Check that mandatory columns are present.
			if (values.containsKey(Schema.COL_TRACK_ID) && values.containsKey(Schema.COL_LONGITUDE)
					&& values.containsKey(Schema.COL_LATITUDE) && values.containsKey(Schema.COL_TIMESTAMP) ) {

				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_WAYPOINT, null, values);
				if (rowId > 0) {
					Uri waypointUri = ContentUris.withAppendedId(uri, rowId);
					getContext().getContentResolver().notifyChange(waypointUri, null);
					return waypointUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_LONGITUDE + ", "
						+ Schema.COL_LATITUDE + ", " + Schema.COL_TIMESTAMP);
			}
			break;
		case Schema.URI_CODE_TRACK:
			if (values.containsKey(Schema.COL_START_DATE)) {
				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_TRACK, null, values);
				if (rowId > 0) {
					Uri trackUri = ContentUris.withAppendedId(CONTENT_URI_TRACK, rowId);
					getContext().getContentResolver().notifyChange(trackUri, null);
					return trackUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_START_DATE);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selectionIn, String[] selectionArgsIn, String sortOrder) {
		Log.v(TAG, "query(), uri=" + uri);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String selection = selectionIn;
		String[] selectionArgs = selectionArgsIn;
		
		// Select which datatype was requested
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACK_TRACKPOINTS:
			if (selectionIn != null || selectionArgsIn != null) {
				// Any selection/selectionArgs will be ignored
				throw new UnsupportedOperationException();
			}
			String trackId = uri.getPathSegments().get(1);
			qb.setTables(Schema.TBL_TRACKPOINT);
			selection = Schema.COL_TRACK_ID + " = ?";
			selectionArgs = new String[] {trackId};
			break;
		case Schema.URI_CODE_TRACK_WAYPOINTS:
			if (selectionIn != null || selectionArgsIn != null) {
				// Any selection/selectionArgs will be ignored
				throw new UnsupportedOperationException();
			}
			trackId = uri.getPathSegments().get(1);
			qb.setTables(Schema.TBL_WAYPOINT);
			selection = Schema.COL_TRACK_ID + " = ?";
			selectionArgs = new String[] {trackId};
			break;
		case Schema.URI_CODE_TRACK:
			qb.setTables(Schema.TBL_TRACK);
			break;
		case Schema.URI_CODE_TRACK_ID:
			if (selectionIn != null || selectionArgsIn != null) {
				// Any selection/selectionArgs will be ignored
				throw new UnsupportedOperationException();
			}
			trackId = uri.getLastPathSegment();
			qb.setTables(Schema.TBL_TRACK);
			selection = Schema.COL_ID + " = ?";
			selectionArgs = new String[] {trackId};			
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		Cursor c = qb.query(dbHelper.getReadableDatabase(), null, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selectionIn, String[] selectionArgsIn) {
		Log.v(TAG, "update(), uri=" + uri);
		
		String table;
		String selection = selectionIn;
		String[] selectionArgs = selectionArgsIn;
		
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACK_WAYPOINTS:
			if (selectionIn == null || selectionArgsIn != null) {
				// Caller must narrow to a specific waypoint
				throw new IllegalArgumentException();
			}
			table = Schema.TBL_WAYPOINT;
			break;
		case Schema.URI_CODE_TRACK_ID:
			if (selectionIn != null || selectionArgsIn != null) {
				// Any selection/selectionArgs will be ignored
				throw new UnsupportedOperationException();
			}
			table = Schema.TBL_TRACK;
			String trackId = uri.getLastPathSegment();
			selection = Schema.COL_ID + " = ?";
			selectionArgs = new String[] {trackId};			
			break;
		case Schema.URI_CODE_TRACK:
			// Dangerous: Will update all the tracks, but necessary for example
			// to switch all the tracks to inactive
			table = Schema.TBL_TRACK;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		int rows = dbHelper.getWritableDatabase().update(Schema.TBL_TRACK, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rows;

	}

	/**
	 * Represents Data Schema.
	 */
	public static final class Schema {
		public static final String TBL_TRACKPOINT = "trackpoint";
		public static final String TBL_WAYPOINT = "waypoint";
		public static final String TBL_TRACK = "track";
		
		public static final String COL_ID = "_id";
		public static final String COL_TRACK_ID = "track_id";
		public static final String COL_UUID = "uuid";
		public static final String COL_LONGITUDE = "longitude";
		public static final String COL_LATITUDE = "latitude";
		public static final String COL_ELEVATION = "elevation";
		public static final String COL_ACCURACY = "accuracy";
		public static final String COL_NBSATELLITES = "nb_satellites";
		public static final String COL_TIMESTAMP = "point_timestamp";
		public static final String COL_NAME = "name";
		public static final String COL_LINK = "link";
		public static final String COL_START_DATE = "start_date";
		public static final String COL_DIR = "directory";
		public static final String COL_ACTIVE = "active";
		
		// Codes for UriMatcher
		public static final int URI_CODE_TRACK = 3;
		public static final int URI_CODE_TRACK_ID = 4;
		public static final int URI_CODE_TRACK_WAYPOINTS = 5;
		public static final int URI_CODE_TRACK_TRACKPOINTS = 6;
		
		public static final int VAL_TRACK_ACTIVE = 1;
		public static final int VAL_TRACK_INACTIVE = 0;
		
	}

}
