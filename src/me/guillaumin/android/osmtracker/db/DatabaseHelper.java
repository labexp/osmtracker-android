package me.guillaumin.android.osmtracker.db;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper for managing database.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	/**
	 * SQL for creating table TRACKPOINT
	 */
	private static final String SQL_CREATE_TABLE_TRACKPOINT = ""
		+ "create table " + Schema.TBL_TRACKPOINT + " ("
		+ Schema.COL_ID	+ " integer primary key autoincrement,"
		+ Schema.COL_TRACK_ID + " integer not null,"
		+ Schema.COL_LATITUDE + " double not null,"
		+ Schema.COL_LONGITUDE + " double not null,"
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
	private static final String SQL_CREATE_TABLE_TRACK = ""
		+ "create table " + Schema.TBL_TRACK + " ("
		+ Schema.COL_ID + " integer primary key autoincrement,"
		+ Schema.COL_NAME + " text,"
		+ Schema.COL_START_DATE + " long not null,"
		+ Schema.COL_DIR + " text,"
		+ Schema.COL_ACTIVE + " integer not null default 0,"
		+ Schema.COL_EXPORT_DATE + " long"  // null indicates not yet exported
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
	 * v12: add TBL_TRACK.COL_EXPORT_DATE, IDX_TRACKPOINT_TRACK, IDX_WAYPOINT_TRACK
	 *</pre>
	 */
	private static final int DB_VERSION = 12;

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
		onCreate(db);
	}

}