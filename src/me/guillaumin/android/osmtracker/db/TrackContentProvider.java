package me.guillaumin.android.osmtracker.db;

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
 * Content provider for track data, using Android {@link ContentProvider}
 * mechanism.
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
	 * Uri for trackpoint
	 */
	public static final Uri CONTENT_URI_TRACKPOINT = Uri.parse("content://" + AUTHORITY + "/" + Schema.TBL_TRACKPOINT);

	/**
	 * Uri for waypoint
	 */
	public static final Uri CONTENT_URI_WAYPOINT = Uri.parse("content://" + AUTHORITY + "/" + Schema.TBL_WAYPOINT);
	
	/**
	 * Uri for config
	 */
	public static final Uri CONTENT_URI_CONFIG = Uri.parse("content://" + AUTHORITY + "/" + Schema.TBL_CONFIG);

	/**
	 * Uri Matcher
	 */
	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		uriMatcher.addURI(AUTHORITY, Schema.TBL_WAYPOINT, Schema.URI_CODE_WAYPOINT);
		uriMatcher.addURI(AUTHORITY, Schema.TBL_TRACKPOINT, Schema.URI_CODE_TRACKPOINT);
		uriMatcher.addURI(AUTHORITY, Schema.TBL_CONFIG, Schema.URI_CODE_CONFIG);
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
		// Select which datatype to delete
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACKPOINT:
			count = dbHelper.getWritableDatabase().delete(Schema.TBL_TRACKPOINT, selection, selectionArgs);
			break;
		case Schema.URI_CODE_WAYPOINT:
			count = dbHelper.getWritableDatabase().delete(Schema.TBL_WAYPOINT, selection, selectionArgs);
			break;
		case Schema.URI_CODE_CONFIG:
			count = dbHelper.getWritableDatabase().delete(Schema.TBL_CONFIG, selection, selectionArgs);
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
		case Schema.URI_CODE_TRACKPOINT:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_TRACKPOINT;
		case Schema.URI_CODE_WAYPOINT:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_WAYPOINT;
		case Schema.URI_CODE_CONFIG:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + OSMTracker.class.getPackage() + "."
					+ Schema.TBL_CONFIG;
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.v(TAG, "insert(), uri=" + uri + ", values=" + values.toString());

		// Select which datatype to insert
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACKPOINT:
			// Check that mandatory columns are present.
			if (values.containsKey(Schema.COL_LONGITUDE) && values.containsKey(Schema.COL_LATITUDE)
					&& values.containsKey(Schema.COL_TIMESTAMP)) {

				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_TRACKPOINT, null, values);
				if (rowId > 0) {
					Uri trackpointUri = ContentUris.withAppendedId(CONTENT_URI_TRACKPOINT, rowId);
					getContext().getContentResolver().notifyChange(trackpointUri, null);
					return trackpointUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_LONGITUDE + ", "
						+ Schema.COL_LATITUDE + ", " + Schema.COL_TIMESTAMP);
			}
			break;
		case Schema.URI_CODE_WAYPOINT:
			// Check that mandatory columns are present.
			if (values.containsKey(Schema.COL_LONGITUDE) && values.containsKey(Schema.COL_LATITUDE)
					&& values.containsKey(Schema.COL_TIMESTAMP) && values.containsKey(Schema.COL_NAME)) {

				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_WAYPOINT, null, values);
				if (rowId > 0) {
					Uri waypointUri = ContentUris.withAppendedId(CONTENT_URI_WAYPOINT, rowId);
					getContext().getContentResolver().notifyChange(waypointUri, null);
					return waypointUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_LONGITUDE + ", "
						+ Schema.COL_LATITUDE + ", " + Schema.COL_TIMESTAMP + ", " + Schema.COL_NAME);
			}
			break;
		case Schema.URI_CODE_CONFIG:
			// Check that mandatory columns are present.
			if (values.containsKey(Schema.COL_KEY)) {
				long rowId = dbHelper.getWritableDatabase().insert(Schema.TBL_CONFIG, null, values);
				if (rowId > 0) {
					Uri configUri = ContentUris.withAppendedId(CONTENT_URI_CONFIG, rowId);
					getContext().getContentResolver().notifyChange(configUri, null);
					return configUri;
				}
			} else {
				throw new IllegalArgumentException("values should provide " + Schema.COL_KEY);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Log.v(TAG, "query(), uri=" + uri);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// Select which datatype was requested
		switch (uriMatcher.match(uri)) {
		case Schema.URI_CODE_TRACKPOINT:
			qb.setTables(Schema.TBL_TRACKPOINT);
			break;
		case Schema.URI_CODE_WAYPOINT:
			qb.setTables(Schema.TBL_WAYPOINT);
			break;
		case Schema.URI_CODE_CONFIG:
			qb.setTables(Schema.TBL_CONFIG);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		Cursor c = qb.query(dbHelper.getReadableDatabase(), null, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new IllegalArgumentException("Update operation is not supported");
	}

	/**
	 * Represents Data Schema.
	 */
	public static final class Schema {
		public static final String TBL_TRACKPOINT = "trackpoint";
		public static final String TBL_WAYPOINT = "waypoint";
		public static final String TBL_CONFIG = "config";
		
		public static final String COL_ID = "_id";
		public static final String COL_LONGITUDE = "longitude";
		public static final String COL_LATITUDE = "latitude";
		public static final String COL_ELEVATION = "elevation";
		public static final String COL_ACCURACY = "accuracy";
		public static final String COL_NBSATELLITES = "nb_satellites";
		public static final String COL_TIMESTAMP = "point_timestamp";
		public static final String COL_NAME = "name";
		public static final String COL_LINK = "link";
		public static final String COL_KEY = "key";
		public static final String COL_VALUE = "value";

		// Codes for UriMatcher
		public static final int URI_CODE_TRACKPOINT = 0;
		public static final int URI_CODE_WAYPOINT = 1;
		public static final int URI_CODE_CONFIG = 2;
		
		/**
		 * Key for config value "track dir"
		 */
		public static final String KEY_CONFIG_TRACKDIR = "trackdir";
	}

}
