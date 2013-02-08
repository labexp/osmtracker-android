package me.guillaumin.android.osmtracker.db.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Represents a Track

 * @author Nicolas Guillaumin
 *
 */
public class Track {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

	public enum OSMVisibility {
		Private(0, R.string.osm_visibility_private),
		Public(1, R.string.osm_visibility_public),
		Trackable(2, R.string.osm_visibility_trackable),
		Identifiable(3, R.string.osm_visibility_identifiable);
		
		public final int position;
		public final int resId;
		
		private OSMVisibility(int position, int resId) {
			this.position = position;
			this.resId = resId;
		}
		
		public static OSMVisibility fromPosition(int position) {
			for (OSMVisibility v: values()) {
				if (v.position == position) {
					return v;
				}
			}
			
			throw new IllegalArgumentException();
		}
	}
	
	private String name;
	private String description;
	private OSMVisibility visibility;
	private List<String> tags = new ArrayList<String>();
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
	 * @param readPointCounts  if trackpoint/waypoint counts will be needed; may be more work for the database.
	 *          If the track table's tp_count and wp_count fields are null, or the track is active,
	 *          the points are counted with SQL COUNT(*) select statements.  The counts will then be
	 *          saved to tp_count and wp_count unless the track is active.
	 * @param withExtraInformation if additional informations (startDate, endDate, first and last track point will be loaded from the database
	 * @return Track
	 */
	public static Track build(final long trackId, Cursor tc, ContentResolver cr, final boolean readPointCounts, boolean withExtraInformation) {
		Track out = new Track();
 
		out.trackId = trackId;
		out.cr = cr;
		out.trackDate = tc.getLong(tc.getColumnIndex(Schema.COL_START_DATE));
		
		out.name = tc.getString(tc.getColumnIndex(Schema.COL_NAME));
		out.description = tc.getString(tc.getColumnIndex(Schema.COL_DESCRIPTION));
		
		String tags = tc.getString(tc.getColumnIndex(Schema.COL_TAGS));
		if (tags != null && ! "".equals(tags)) {
			out.tags.addAll(Arrays.asList(tags.split(",")));
		}
		
		out.visibility = OSMVisibility.valueOf(tc.getString(tc.getColumnIndex(Schema.COL_OSM_VISIBILITY)));

		if (readPointCounts) {
			final boolean trackInactive = (tc.getInt(tc.getColumnIndex(Schema.COL_ACTIVE)) == Schema.VAL_TRACK_INACTIVE);
			boolean countedAlready = trackInactive;
			if (countedAlready) {
				// try to retrieve from tp_count, wp_count columns
				final int idx_tp = tc.getColumnIndex(Schema.COL_TRACKPOINT_COUNT),
				          idx_wp = tc.getColumnIndex(Schema.COL_WAYPOINT_COUNT);
				if (tc.isNull(idx_tp) || tc.isNull(idx_wp)) {
					countedAlready = false;
				} else {
					out.tpCount = tc.getInt(idx_tp);
					out.wpCount = tc.getInt(idx_wp);
				}
			}

			if (! countedAlready) {
				Cursor pcc = cr.query(TrackContentProvider.trackTrackpointCountUri(trackId), null, null, null, null);
				if (pcc.moveToFirst())
					out.tpCount = pcc.getInt(0);
				else
					out.tpCount = -1;
				pcc.close();

				pcc = cr.query(TrackContentProvider.trackWaypointCountUri(trackId), null, null, null, null);
				if (pcc.moveToFirst())
					out.wpCount = pcc.getInt(0);
				else
					out.wpCount = -1;
				pcc.close();

				if (trackInactive && (out.tpCount != -1) && (out.wpCount != -1)) {
					// remember those counts in track table
					Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
					ContentValues values = new ContentValues();
					values.put(Schema.COL_TRACKPOINT_COUNT, out.tpCount);
					values.put(Schema.COL_WAYPOINT_COUNT, out.wpCount);
					cr.update(trackUri, values, null, null);
				}
			}
		}
		
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
			startCursor.close();
			
			Cursor endCursor = cr.query(TrackContentProvider.trackEndUri(trackId), null, null, null, null);
			if(endCursor.moveToFirst()){
				endDate = endCursor.getLong(endCursor.getColumnIndex(Schema.COL_TIMESTAMP));
				endLat = endCursor.getFloat(endCursor.getColumnIndex(Schema.COL_LATITUDE));
				endLong = endCursor.getFloat(endCursor.getColumnIndex(Schema.COL_LONGITUDE));
			}
			endCursor.close();
			
			extraInformationRead = true;
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setDescription(String description) {
		this.description = description;
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

	public String getDescription() {
		return description;
	}
	
	public List<String> getTags() {
		return tags;
	}
	
	public OSMVisibility getVisibility() {
		return visibility;
	}
	
	public String getCommaSeparatedTags() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<tags.size(); i++) {
			sb.append(tags.get(i));
			if (i+1 < tags.size()) {
				sb.append(",");
			}
		}
		return sb.toString();
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
