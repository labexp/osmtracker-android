package me.guillaumin.android.osmtracker.gpx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;

/**
 * Writes a GPX file.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class ExportTrackTask  extends AsyncTask<Void, Integer, Boolean> {

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
	 * {@link Context} to get resources
	 */
	private Context context;
	
	/**
	 * Track ID to export
	 */
	private long trackId;
	
	/**
	 * Dialog to display while exporting
	 */
	private ProgressDialog dialog;

	/**
	 * Message in case of an error
	 */
	private String errorMsg = null;
	
	public ExportTrackTask(Context context, long trackId) {
		this.context = context;
		this.trackId = trackId;
	}

	
	@Override
	protected void onPreExecute() {
		// Display dialog
		dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setIndeterminate(true);
		dialog.setTitle(
				context.getResources().getString(R.string.trackmgr_exporting)
				.replace("{0}", Long.toString(trackId)));
		dialog.setCancelable(false);
		dialog.show();
	}
	
	
	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			exportTrackAsGpx(trackId);
		} catch (ExportTrackException ete) {
			errorMsg = ete.getMessage();
			return false;
		}
		return true;
	}
	
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		dialog.setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		dialog.dismiss();
		if (!success) {
			new AlertDialog.Builder(context)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(context.getResources()
						.getString(R.string.trackmgr_export_error)
						.replace("{0}", errorMsg))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
				})
				.show();
		}
	}

	private void exportTrackAsGpx(long trackId) throws ExportTrackException {
		File sdRoot = Environment.getExternalStorageDirectory();
		if (sdRoot.canWrite()) {
			Cursor c = context.getContentResolver()
					.query(ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId), null, null,
							null, null);

			c.moveToFirst();
			File trackDir = new File(c.getString(c.getColumnIndex(Schema.COL_DIR)));
			long startDate = c.getLong(c.getColumnIndex(Schema.COL_START_DATE));
			c.close();

			if (trackDir != null) {

				File trackFile = new File(trackDir, DataHelper.FILENAME_FORMATTER.format(new Date(startDate))
						+ DataHelper.EXTENSION_GPX);

				Cursor cTrackPoints = context.getContentResolver().query(TrackContentProvider.trackPointsUri(trackId), null,
						null, null, Schema.COL_TIMESTAMP + " asc");
				Cursor cWayPoints = context.getContentResolver().query(TrackContentProvider.waypointsUri(trackId), null, null,
						null, Schema.COL_TIMESTAMP + " asc");

				dialog.setIndeterminate(false);
				dialog.setProgress(0);
				dialog.setMax(cTrackPoints.getCount() + cWayPoints.getCount());
				
				try {
					writeGpxFile(cTrackPoints, cWayPoints, trackFile);
					DataHelper.setTrackExportDate(trackId, System.currentTimeMillis(), context.getContentResolver());
				} catch (IOException ioe) {
					throw new ExportTrackException(ioe.getMessage());
				} finally {
					cTrackPoints.close();
					cWayPoints.close();
				}
			}
		} else {
			throw new ExportTrackException(context.getResources().getString(R.string.error_externalstorage_not_writable));
		}
	}
	
	/**
	 * Writes the GPX file
	 * @param cTrackPoints Cursor to track points.
	 * @param cWayPoints Cursor to way points.
	 * @param target Target GPX file
	 * @throws IOException 
	 */
	private void writeGpxFile(Cursor cTrackPoints, Cursor cWayPoints, File target) throws IOException {
		
		String accuracyOutput = PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_OUTPUT_ACCURACY,
				OSMTracker.Preferences.VAL_OUTPUT_ACCURACY);
		boolean fillHDOP = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION,
				OSMTracker.Preferences.VAL_OUTPUT_GPX_HDOP_APPROXIMATION);
		
		FileWriter fw = new FileWriter(target);
		
		fw.write(XML_HEADER + "\n");
		fw.write(TAG_GPX + "\n");
		
		writeTrackPoints(context.getResources().getString(R.string.gpx_track_name), fw, cTrackPoints, fillHDOP);
		fw.flush();
		writeWayPoints(fw, cWayPoints, accuracyOutput, fillHDOP);
		
		fw.write("</gpx>");
		
		fw.close();
	}
	
	/**
	 * Iterates on track points and write them.
	 * @param trackName Name of the track (metadata).
	 * @param fw Writer to the target file.
	 * @param c Cursor to track points.
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @throws IOException
	 */
	private void writeTrackPoints(String trackName, FileWriter fw, Cursor c, boolean fillHDOP) throws IOException {
		fw.write("\t" + "<trk>" + "\n");
		fw.write("\t\t" + "<name>" + CDATA_START + trackName + CDATA_END + "</name>" + "\n");
		if (fillHDOP) {
			fw.write("\t\t" + "<cmt>"
					+ CDATA_START
					+ context.getResources().getString(R.string.gpx_hdop_approximation_cmt)
					+ CDATA_END
					+ "</cmt>" + "\n");
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
	        fw.flush();
	        dialog.incrementProgressBy(1);
		}
		
		fw.write("\t\t" + "</trkseg>" + "\n");
		fw.write("\t" + "</trk>" + "\n");
	}
	
	/**
	 * Iterates on way points and write them.
	 * @param fw Writer to the target file.
	 * @param c Cursor to way points.
	 * @param accuracyInfo Constant describing how to include (or not) accuracy info for way points.
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @throws IOException
	 */
	private void writeWayPoints(FileWriter fw, Cursor c, String accuracyInfo, boolean fillHDOP) throws IOException {
		// Label for meter unit
		String meterUnit = context.getResources().getString(R.string.various_unit_meters);
		// Word "accuracy"
		String accuracy = context.getResources().getString(R.string.various_accuracy);
		
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
		    	} else {
		    		// Unknown value for accuracy info, shouldn't occur but who knows ?
		    		// See issue #68. Output at least the name just in case.
		    		out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
		    	}
		    } else {
		    	// No accuracy info requested, or available
		    	out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
		    }
			
		    String link = c.getString(c.getColumnIndex(Schema.COL_LINK));
		    if (link != null) {
		       	out.append("\t\t" + "<link href=\"" + URLEncoder.encode(link) + "\">" + "\n");
		       	out.append("\t\t\t" + "<text>" + link +"</text>\n");
		       	out.append("\t\t" + "</link>" + "\n");
		    }
		    
		    if (! c.isNull(c.getColumnIndex(Schema.COL_NBSATELLITES))) {
		    	out.append("\t\t" + "<sat>" + c.getInt(c.getColumnIndex(Schema.COL_NBSATELLITES)) + "</sat>" + "\n");
		    }
		    
		    out.append("\t" + "</wpt>" + "\n");
		    
		    fw.write(out.toString());
		    fw.flush();
		    dialog.incrementProgressBy(1);
		}
	}
}
