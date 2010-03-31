package me.guillaumin.android.osmtracker.gpx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;

/**
 * Writes a GPX file.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPXFileWriter {

	/**
	 * XML header.
	 */
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
	
	private static final String CDATA_START = "<![CDATA[";
	private static final String CDATA_END = "]]>";
	
	/**
	 * GPX opening tag
	 */
	private static final String TAG_GPX = "<gpx"
		+ " xmlns=\"http://www.topografix.com/GPX/1/1\""
		+ " version=\"1.1\""
		+ " creator=\"osmtracker-android\""		// TODO: Get name in resources ?
		+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
		+ " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd \">";
	
	/**
	 * Date format for a point timestamp.
	 */
	private static SimpleDateFormat POINT_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	static {
		POINT_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Writes the GPX file
	 * @param resources Access to application resources
	 * @param cTrackPoints Cursor to track points.
	 * @param cWayPoints Cursor to way points.
	 * @param target Target GPX file
	 * @param preferences App preferences to access output settings
	 * @throws IOException 
	 */
	public static void writeGpxFile(Resources resources, Cursor cTrackPoints, Cursor cWayPoints, File target, SharedPreferences preferences) throws IOException {
		
		String accuracyOutput = preferences.getString(
				OSMTracker.Preferences.KEY_OUTPUT_ACCURACY,
				OSMTracker.Preferences.VAL_OUTPUT_ACCURACY);
		boolean fillHDOP = preferences.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION, OSMTracker.Preferences.VAL_OUTPUT_GPX_HDOP_APPROXIMATION);
		
		FileWriter fw = new FileWriter(target);
		
		fw.write(XML_HEADER + "\n");
		fw.write(TAG_GPX + "\n");
		
		writeTrackPoints(resources.getString(R.string.gpx_track_name), fw, cTrackPoints, resources, fillHDOP);
		writeWayPoints(fw, cWayPoints, accuracyOutput, resources, fillHDOP);
		
		fw.write("</gpx>");
		
		fw.close();
	}
	
	/**
	 * Iterates on track points and write them.
	 * @param trackName Name of the track (metadata).
	 * @param fw Writer to the target file.
	 * @param c Cursor to track points.
	 * @param resourcse To access string resources
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @throws IOException
	 */
	public static void writeTrackPoints(String trackName, FileWriter fw, Cursor c, Resources resources, boolean fillHDOP) throws IOException {
		fw.write("\t" + "<trk>" + "\n");
		fw.write("\t\t" + "<name>" + CDATA_START + trackName + CDATA_END + "</name>" + "\n");
		if (fillHDOP) {
			fw.write("\t\t" + "<cmt>" + CDATA_START + resources.getString(R.string.gpx_hdop_approximation_cmt) + CDATA_END + "</cmt>" + "\n");
		}
		
		fw.write("\t\t" + "<trkseg>" + "\n");
		
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			StringBuffer out = new StringBuffer();
			out.append("\t\t\t" + "<trkpt lat=\"" 
					+ c.getDouble(c.getColumnIndex(Schema.COL_LATITUDE)) + "\" "
					+ "lon=\"" + c.getDouble(c.getColumnIndex(Schema.COL_LONGITUDE)) + "\">" + "\n");
	        if (! c.isNull(c.getColumnIndex(Schema.COL_ELEVATION))) {
	        	out.append("\t\t\t\t" + "<ele>" + c.getDouble(c.getColumnIndex(Schema.COL_ELEVATION)) + "</ele>" + "\n");
	        }
	        out.append("\t\t\t\t" + "<time>" + POINT_DATE_FORMATTER.format(new Date(c.getLong(c.getColumnIndex(Schema.COL_TIMESTAMP)))) + "</time>" + "\n");
	        
	        if(fillHDOP && ! c.isNull(c.getColumnIndex(Schema.COL_ACCURACY))) {
	        	out.append("\t\t\t\t" + "<hdop>" + (c.getDouble(c.getColumnIndex(Schema.COL_ACCURACY)) / OSMTracker.HDOP_APPROXIMATION_FACTOR) + "</hdop>" + "\n");
	        }
	       
	        out.append("\t\t\t" + "</trkpt>" + "\n");
	        fw.write(out.toString());
		}
		
		fw.write("\t\t" + "</trkseg>" + "\n");
		fw.write("\t" + "</trk>" + "\n");
	}
	
	/**
	 * Iterates on way points and write them.
	 * @param fw Writer to the target file.
	 * @param c Cursor to way points.
	 * @param accuracyOutput Constant describing how to include (or not) accuracy info for way points.
	 * @param resourcse To access string resources
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @throws IOException
	 */
	public static void writeWayPoints(FileWriter fw, Cursor c, String accuracyInfo, Resources resources, boolean fillHDOP) throws IOException {
		// Label for meter unit
		String meterUnit = resources.getString(R.string.various_unit_meters);
		// Word "accuracy"
		String accuracy = resources.getString(R.string.various_accuracy);
		
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			StringBuffer out = new StringBuffer();
			out.append("\t" + "<wpt lat=\""
					+ c.getDouble(c.getColumnIndex(Schema.COL_LATITUDE)) + "\" "
					+ "lon=\"" + c.getDouble(c.getColumnIndex(Schema.COL_LONGITUDE)) + "\">" + "\n");
	        if (! c.isNull(c.getColumnIndex(Schema.COL_ELEVATION))) {
	        	out.append("\t\t" + "<ele>" + c.getDouble(c.getColumnIndex(Schema.COL_ELEVATION)) + "</ele>" + "\n");
	        }
		    out.append("\t\t" + "<time>" + POINT_DATE_FORMATTER.format(new Date(c.getLong(c.getColumnIndex(Schema.COL_TIMESTAMP)))) + "</time>" + "\n");

		    if(fillHDOP && ! c.isNull(c.getColumnIndex(Schema.COL_ACCURACY))) {
	        	out.append("\t\t" + "<hdop>" + (c.getDouble(c.getColumnIndex(Schema.COL_ACCURACY)) / OSMTracker.HDOP_APPROXIMATION_FACTOR) + "</hdop>" + "\n");
	        }
		    
		    String name = c.getString(c.getColumnIndex(Schema.COL_NAME));
		    
		    if (! OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_NONE.equals(accuracyInfo) && ! c.isNull(c.getColumnIndex(Schema.COL_ACCURACY))) {
		    	// Outputs accuracy info for way point
		    	if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_NAME.equals(accuracyInfo)) {
		    		// Output accuracy with name
		    		out.append("\t\t" + "<name>"
		    				+ CDATA_START 
		    				+ name
		    				+ " (" + c.getDouble(c.getColumnIndex(Schema.COL_ACCURACY)) + meterUnit + ")"
		    				+ CDATA_END
		    				+ "</name>" + "\n");
		    	} else if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_CMT.equals(accuracyInfo)) {
		    		// Output accuracy in separate tag
		    		out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
		    		out.append("\t\t" + "<cmt>" + CDATA_START + accuracy + ": " + c.getDouble(c.getColumnIndex(Schema.COL_ACCURACY)) + meterUnit + CDATA_END + "</cmt>" + "\n");
		    	}
		    } else {
		    	// No accuracy info requested, or available
		    	out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
		    }
			
		    String link = c.getString(c.getColumnIndex(Schema.COL_LINK));
		    if (link != null) {
		       	out.append("\t\t" + "<link>" + link + "</link>" + "\n");
		    }
		    
		    if (! c.isNull(c.getColumnIndex(Schema.COL_NBSATELLITES))) {
		    	out.append("\t\t" + "<sat>" + c.getInt(c.getColumnIndex(Schema.COL_NBSATELLITES)) + "</sat>" + "\n");
		    }
		    
		    out.append("\t" + "</wpt>" + "\n");
		    
		    fw.write(out.toString());
		}
	}
}
