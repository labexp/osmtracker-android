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
	
	private Long startDate=null, endDate=null;
	private Float startLat=null, startLong=null, endLat=null, endLong=null;
	
	public static Track build(final long trackId, Cursor tc, ContentResolver cr) {
		Track out = new Track();
		
		out.setTrackDate(tc.getLong(tc.getColumnIndex(Schema.COL_START_DATE)));
		out.setName(tc.getString(tc.getColumnIndex(Schema.COL_NAME)));

		// Track points
		Cursor tpCursor = cr.query(
				TrackContentProvider.trackPointsUri(trackId),
				null, null, null, Schema.COL_ID);
		out.setTpCount(tpCursor.getCount());
		if (tpCursor.moveToFirst()) {
			// start
			out.setStartLat(tpCursor.getFloat(tpCursor.getColumnIndex(Schema.COL_LATITUDE)));
			out.setStartLong(tpCursor.getFloat(tpCursor.getColumnIndex(Schema.COL_LONGITUDE)));
			out.setStartDate(tpCursor.getLong(tpCursor.getColumnIndex(Schema.COL_TIMESTAMP)));
			
			tpCursor.moveToLast();
			out.setEndLat(tpCursor.getFloat(tpCursor.getColumnIndex(Schema.COL_LATITUDE)));
			out.setEndLong(tpCursor.getFloat(tpCursor.getColumnIndex(Schema.COL_LONGITUDE)));
			out.setEndDate(tpCursor.getLong(tpCursor.getColumnIndex(Schema.COL_TIMESTAMP)));
		}
		tpCursor.close();
		
		// Way points
		Cursor wpCursor = cr.query(
				TrackContentProvider.waypointsUri(trackId),
				null, null,	null, null);
		out.setWpCount(wpCursor.getCount());
		wpCursor.close();
		
		return out;		
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
		if (startDate != null) {
			return DATE_FORMAT.format(new Date(startDate));
		} else {
			return "";
		}
	}
	
	public String getEndDateAsString() {
		if (endDate != null) {
			return DATE_FORMAT.format(new Date(endDate));
		} else {
			return "";
		}
	}

	public Float getStartLat() {
		return startLat;
	}

	public Float getStartLong() {
		return startLong;
	}

	public Float getEndLat() {
		return endLat;
	}

	public Float getEndLong() {
		return endLong;
	}


	
}
