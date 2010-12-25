package me.guillaumin.android.osmtracker.db.model;

import java.text.DateFormat;
import java.util.Date;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.database.Cursor;

/**
 * Represents a Track

 * @author Nicolas Guillaumin
 *
 */
public class Track {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
	
	private String name;
	private int tpCount, wpCount;
	private long trackDate;
	private long trackId;
	
	private Long startDate=null, endDate=null;
	private Float startLat=null, startLong=null, endLat=null, endLong=null;
	
	private boolean extraInformationRead = false;
	
	private ContentResolver cr;
	
	/**
	 * build a track object with the given cursor
	 * 
	 * @param trackId id of the track that will be built
	 * @param tc cursor that is used to build the track
	 * @param cr the content resolver to use
	 * @param withExtraInformation if additional informations (startDate, endDate, first and last track point will be loaded from the database
	 * @return Track
	 */
	public static Track build(final long trackId, Cursor tc, ContentResolver cr, boolean withExtraInformation) {
		Track out = new Track();

		out.trackId = trackId;
		out.cr = cr;
		out.trackDate = tc.getLong(tc.getColumnIndex(Schema.COL_START_DATE));
		out.name = tc.getString(tc.getColumnIndex(Schema.COL_NAME));

		out.tpCount = tc.getInt(tc.getColumnIndex(Schema.COL_TRACKPOINT_COUNT));
		
		out.wpCount = tc.getInt(tc.getColumnIndex(Schema.COL_WAYPOINT_COUNT));
		
		if(withExtraInformation){
			out.readExtraInformation();
		}
		
		return out;		
	}
	
	private void readExtraInformation(){
		if(!extraInformationRead){
			Cursor startCursor = cr.query(TrackContentProvider.trackStartUri(trackId), null, null, null, null);
			if(startCursor.moveToFirst()){
				startDate = startCursor.getLong(startCursor.getColumnIndex(Schema.COL_TIMESTAMP));
				startLat = startCursor.getFloat(startCursor.getColumnIndex(Schema.COL_LATITUDE));
				startLong = startCursor.getFloat(startCursor.getColumnIndex(Schema.COL_LONGITUDE));
			}
			Cursor endCursor = cr.query(TrackContentProvider.trackEndUri(trackId), null, null, null, null);
			if(endCursor.moveToFirst()){
				endDate = endCursor.getLong(endCursor.getColumnIndex(Schema.COL_TIMESTAMP));
				endLat = endCursor.getFloat(endCursor.getColumnIndex(Schema.COL_LATITUDE));
				endLong = endCursor.getFloat(endCursor.getColumnIndex(Schema.COL_LONGITUDE));
			}
			extraInformationRead = true;
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setTpCount(int tpCount) {
		this.tpCount = tpCount;
	}

	public void setWpCount(int wpCount) {
		this.wpCount = wpCount;
	}

	public void setTracktDate(long tracktDate) {
		this.trackDate = tracktDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	public void setStartLat(float startLat) {
		this.startLat = startLat;
	}

	public void setTrackDate(long trackDate) {
		this.trackDate = trackDate;
	}
	
	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}
	
	public void setStartLong(float startLong) {
		this.startLong = startLong;
	}

	public void setEndLat(float endLat) {
		this.endLat = endLat;
	}

	public void setEndLong(float endLong) {
		this.endLong = endLong;
	}

	public Integer getWpCount() {
		return wpCount;
	}
	
	public Integer getTpCount() {
		return tpCount;
	}
	
	public String getName() {
		if (name != null && name.length() > 0) {
			return name;
		} else {
			// Use start date as name
			return DATE_FORMAT.format(new Date(trackDate));
		}
	}
	
	public String getStartDateAsString() {
		readExtraInformation();
		if (startDate != null) {
			return DATE_FORMAT.format(new Date(startDate));
		} else {
			return "";
		}
	}
	
	public String getEndDateAsString() {
		readExtraInformation();
		if (endDate != null) {
			return DATE_FORMAT.format(new Date(endDate));
		} else {
			return "";
		}
	}

	public Float getStartLat() {
		readExtraInformation();
		return startLat;
	}

	public Float getStartLong() {
		readExtraInformation();
		return startLong;
	}

	public Float getEndLat() {
		readExtraInformation();
		return endLat;
	}

	public Float getEndLong() {
		readExtraInformation();
		return endLong;
	}


	
}
