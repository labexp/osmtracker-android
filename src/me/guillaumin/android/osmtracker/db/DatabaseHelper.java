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
	 * SQL for creating table TRACK
	 */
	private static final String SQL_CREATE_TABLE_TRACK = ""
		+ "create table " + Schema.TBL_TRACK + " ("
		+ Schema.COL_ID + " integer primary key autoincrement,"
		+ Schema.COL_NAME + " text,"
		+ Schema.COL_START_DATE + " long not null,"
		+ Schema.COL_DIR + " text,"
		+ Schema.COL_ACTIVE + " integer not null default 0" + ")";
			
	
	/**
	 * Database name.
	 */
	private static final String DB_NAME = OSMTracker.class.getSimpleName();

	/**
	 * Database version.
	 */
	private static final int DB_VERSION = 9;

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("drop table if exists " + Schema.TBL_TRACKPOINT);
		db.execSQL(SQL_CREATE_TABLE_TRACKPOINT);
		db.execSQL("drop table if exists " + Schema.TBL_WAYPOINT);
		db.execSQL(SQL_CREATE_TABLE_WAYPOINT);
		db.execSQL("drop table if exists " + Schema.TBL_TRACK);
		db.execSQL(SQL_CREATE_TABLE_TRACK);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
	}

}