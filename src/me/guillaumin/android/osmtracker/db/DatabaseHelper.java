package me.guillaumin.android.osmtracker.db;

import java.io.File;
import java.io.FilenameFilter;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track.OSMVisibility;
import me.guillaumin.android.osmtracker.util.FileSystemUtils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper for managing database.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getSimpleName();	
	
	/**
	 * SQL for creating table TRACKPOINT
	 */
	private static final String SQL_CREATE_TABLE_TRACKPOINT = ""
		+ "create table " + Schema.TBL_TRACKPOINT + " ("
		+ Schema.COL_ID	+ " integer primary key autoincrement,"
		+ Schema.COL_TRACK_ID + " integer not null,"
		+ Schema.COL_LATITUDE + " double not null,"
		+ Schema.COL_LONGITUDE + " double not null,"
		+ Schema.COL_SPEED + " double null,"
		+ Schema.COL_ELEVATION + " double null,"
		+ Schema.COL_ACCURACY + " double null,"
		+ Schema.COL_TIMESTAMP + " long not null" + ")";

	/**
	 * SQL for creating index TRACKPOINT_idx (track id)
	 * @since 12
	 */
	private static final String SQL_CREATE_IDX_TRACKPOINT_TRACK
		= "create index if not exists "
		+ Schema.TBL_TRACKPOINT
		+ "_idx ON " + Schema.TBL_TRACKPOINT + "(" + Schema.COL_TRACK_ID + ")";

	/**
	 * SQL for creating table WAYPOINT
	 */
	private static final String SQL_CREATE_TABLE_WAYPOINT = ""
		+ "create table " + Schema.TBL_WAYPOINT + " ("
		+ Schema.COL_ID + " integer primary key autoincrement,"
		+ Schema.COL_TRACK_ID + " integer not null,"
		+ Schema.COL_UUID + " text,"
		+ Schema.COL_LATITUDE + " double not null,"
		+ Schema.COL_LONGITUDE + " double not null,"
		+ Schema.COL_ELEVATION + " double null,"
		+ Schema.COL_ACCURACY + " double null,"
		+ Schema.COL_TIMESTAMP + " long not null,"
		+ Schema.COL_NAME + " text,"
		+ Schema.COL_LINK + " text,"
		+ Schema.COL_NBSATELLITES + " integer not null" + ")";

	/**
	 * SQL for creating index WAYPOINT_idx (track id)
	 * @since 12
	 */
	private static final String SQL_CREATE_IDX_WAYPOINT_TRACK
		= "create index if not exists "
		+ Schema.TBL_WAYPOINT
		+ "_idx ON " + Schema.TBL_WAYPOINT + "(" + Schema.COL_TRACK_ID + ")";

	/**
	 * SQL for creating table TRACK
	 * @since 5
	 */
	@SuppressWarnings("deprecation")
	private static final String SQL_CREATE_TABLE_TRACK = ""
		+ "create table " + Schema.TBL_TRACK + " ("
		+ Schema.COL_ID + " integer primary key autoincrement,"
		+ Schema.COL_NAME + " text,"
		+ Schema.COL_DESCRIPTION + " text,"
		+ Schema.COL_TAGS + " text,"
		+ Schema.COL_OSM_VISIBILITY + " text default '"+OSMVisibility.Private+"',"
		+ Schema.COL_START_DATE + " long not null,"
		+ Schema.COL_DIR + " text," // unused since DB_VERSION 13, since SQLite doesn't support to remove a column it will stay for now
		+ Schema.COL_ACTIVE + " integer not null default 0,"
		+ Schema.COL_EXPORT_DATE + " long,"  // null indicates not yet exported
		+ Schema.COL_OSM_UPLOAD_DATE + " long" // null indicates not yet uploaded
		+ ")";

	/**
	 * Database name.
	 */
	private static final String DB_NAME = OSMTracker.class.getSimpleName();

	/**
	 * Database version.
	 * If you change the version, be sure that {@link #onUpgrade(SQLiteDatabase, int, int)} can handle it.
	 * Only required for versions after v0.5.0 as before the DB was fully erased and recreated from scratch
	 * for each new track.
	 *<pre>
	 *  v1: (r117)  v0.4.0, v0.4.1
	 *  v2: add TBL_CONFIG; that table's been dropped since then (r163)  v0.4.2
	 *  v3: add TBL_WAYPOINT.COL_UUID  (r187)  v0.4.3
	 *  v5: add TBL_TRACK; TRACKPOINT, WAYPOINT +COL_TRACK_ID  (r198)
	 *  v7: add TBL_TRACK.COL_DIR; drop TBL_CONFIG  (r201)
	 *  v9: add TBL_TRACK.COL_ACTIVE  (r206)
	 * v12: add TBL_TRACK.COL_EXPORT_DATE, IDX_TRACKPOINT_TRACK, IDX_WAYPOINT_TRACK (r207) v0.5.0
	 * v13: TBL_TRACK.COL_DIR is now deprecated (rxxx) v0.5.3 TODO: fill in correct revision and version
	 * v14: add TBL_TRACK.COL_OSM_UPLOAD_DATE, TBL_TRACK.COL_DESCRIPTION,
	 * 			TBL_TRACK.COL_TAGS and TBL_TRACK.COL_OSM_VISIBILITY for OSM upload - v0.6.0 
	 * v15: add TBL_TRACKPOINT.COL_SPPED
	 *</pre>
	 */
	private static final int DB_VERSION = 15;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("drop table if exists " + Schema.TBL_TRACKPOINT);
		db.execSQL(SQL_CREATE_TABLE_TRACKPOINT);
		db.execSQL(SQL_CREATE_IDX_TRACKPOINT_TRACK);
		db.execSQL("drop table if exists " + Schema.TBL_WAYPOINT);
		db.execSQL(SQL_CREATE_TABLE_WAYPOINT);
		db.execSQL(SQL_CREATE_IDX_WAYPOINT_TRACK);
		db.execSQL("drop table if exists " + Schema.TBL_TRACK);
		db.execSQL(SQL_CREATE_TABLE_TRACK);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch(oldVersion){
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
		case 11: //pre v0.5.0 (completely create a new database)
			onCreate(db);
			break;
		case 12:
			manageNewStoragePath(db);
		case 13:
			// Create 'osm_upload_date', 'description', 'tags' and 'visibility'
			db.execSQL("alter table " + Schema.TBL_TRACK + " add column " + Schema.COL_OSM_UPLOAD_DATE+ " long");
			db.execSQL("alter table " + Schema.TBL_TRACK + " add column " + Schema.COL_DESCRIPTION + " text");
			db.execSQL("alter table " + Schema.TBL_TRACK + " add column " + Schema.COL_TAGS + " text");
			db.execSQL("alter table " + Schema.TBL_TRACK + " add column " + Schema.COL_OSM_VISIBILITY
					+ " text default '"+OSMVisibility.Private+"'");
		case 14:
			db.execSQL("alter table " + Schema.TBL_TRACKPOINT + " add column " + Schema.COL_SPEED + " double null");
		}
		
	}

	/**
	 * copies files from the tracks to our new storage directory and removes the path reference in COL_DIR
	 * @param db the database to work on 
	 */
	@SuppressWarnings("deprecation")
	private void manageNewStoragePath(SQLiteDatabase db){
		Log.d(TAG,"manageNewStoragePath");
		
		// we'll need this FilenameFitler to clean up our track directory
		FilenameFilter gpxFilenameFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.toLowerCase().endsWith(".gpx"))
					return true;
				return false;
			}
		};
		
		// query all tracks
		String[] columns = new String[]{Schema.COL_ID, Schema.COL_DIR};
		Cursor cursor = db.query(Schema.TBL_TRACK, columns, null, null, null, null, null);
		
		// if we have a valid cursor and can write to the sdcard, we'll go on and try to copy the files
		if(cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "manageNewStoragePath (found " + cursor.getCount() + " tracks to be processed)");
			do{
				long trackId = cursor.getLong(cursor.getColumnIndex(Schema.COL_ID));
				Log.d(TAG,"manageNewStoragePath (" + trackId + ")");
				String oldDirName = cursor.getString(cursor.getColumnIndex(Schema.COL_DIR));
				File newDir = DataHelper.getTrackDirectory(trackId);
				File oldDir = new File(oldDirName);
				if(oldDir.exists() && oldDir.canRead()){
					
					// if our new directory doesn't exist, we'll create it
					if(!newDir.exists())
						newDir.mkdirs();
					
					if(newDir.exists() && newDir.canWrite()){
						Log.d(TAG,"manageNewStoragePath (" + trackId + "): copy directory");
						// we'll first copy all files to our new storage area... we'll clean up later
						FileSystemUtils.copyDirectoryContents(newDir, oldDir);
						
						// cleaning up new storage area
						// find gpx files we accidentally copied to our new storage area and delete them 
						for(File gpxFile:newDir.listFiles(gpxFilenameFilter)){
							Log.d(TAG,"manageNewStoragePath (" + trackId + "): deleting gpx file ["+gpxFile+"]");
							gpxFile.delete();
						}
					}else{
						Log.e(TAG, "manageNewStoragePath (" + trackId + "): directory ["+newDir+"] is not writable or could not be created");
					}
					
				}
			}while(cursor.moveToNext());
			
			cursor.close();
		}
		
		ContentValues vals = new ContentValues();
		vals.putNull(Schema.COL_DIR);
		db.update(Schema.TBL_TRACK, vals, null, null);
	}
	
}