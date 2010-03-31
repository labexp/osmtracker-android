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
	private static final String SQL_CREATE_TABLE_TRACKPOINT = "" + "create table trackpoint (" + Schema.COL_ID
			+ " integer primary key autoincrement," + Schema.COL_LATITUDE + " double not null," + Schema.COL_LONGITUDE
			+ " double not null," + Schema.COL_ELEVATION + " double null," + Schema.COL_ACCURACY + " double null,"
			+ Schema.COL_TIMESTAMP + " long not null" + ")";

	/**
	 * SQL for creating table WAYPOINT
	 */
	private static final String SQL_CREATE_TABLE_WAYPOINT = "" + "create table waypoint (" + Schema.COL_ID
			+ " integer primary key autoincrement," + Schema.COL_LATITUDE + " double not null," + Schema.COL_LONGITUDE
			+ " double not null," + Schema.COL_ELEVATION + " double null," + Schema.COL_ACCURACY + " double null,"
			+ Schema.COL_TIMESTAMP + " long not null," + Schema.COL_NAME + " text," + Schema.COL_LINK + " text,"
			+ Schema.COL_NBSATELLITES + " integer not null" + ")";

	/**
	 * Database name.
	 */
	private static final String DB_NAME = OSMTracker.class.getSimpleName();

	/**
	 * Database version.
	 */
	private static final int DB_VERSION = 1;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("drop table if exists " + Schema.TBL_TRACKPOINT);
		db.execSQL(SQL_CREATE_TABLE_TRACKPOINT);
		db.execSQL("drop table if exists " + Schema.TBL_WAYPOINT);
		db.execSQL(SQL_CREATE_TABLE_WAYPOINT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
	}

}