package org.osmtracker.gpx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.osmtracker.OSMTracker;
import org.osmtracker.R;
import org.osmtracker.db.DataHelper;
import org.osmtracker.db.TrackContentProvider;
import org.osmtracker.exception.ExportTrackException;
import org.osmtracker.util.FileSystemUtils;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Base class to writes a GPX file and export
 * track media (Photos, Sounds)
 * 
 * @author Nicolas Guillaumin
 *
 */
public abstract class ExportTrackTask  extends AsyncTask<Void, Long, Boolean> {

	private static final String TAG = ExportTrackTask.class.getSimpleName();

	/**
	 * Characters to replace in track filename, for use by {@link #buildGPXFilename(Cursor)}. <BR>
	 * The characters are: (space) ' " / \ * ? ~ @ &lt; &gt; <BR>
	 * In addition, ':' will be replaced by ';', before calling this pattern.
	 */
	private final static Pattern FILENAME_CHARS_BLACKLIST_PATTERN =
		Pattern.compile("[ '\"/\\\\*?~@<>]");  // must double-escape \

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
		+ " creator=\"OSMTracker for Android™ - https://github.com/nguillaumin/osmtracker-android\""
		+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
		+ " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd \">";
	
	/**
	 * Date format for a point timestamp.
	 */
	private SimpleDateFormat pointDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	/**
	 * {@link Context} to get resources
	 */
	protected Context context;
	
	/**
	 * Track IDs to export
	 */
	protected long[] trackIds;
	
	/**
	 * Dialog to display while exporting
	 */
	protected ProgressDialog dialog;

	/**
	 * Message in case of an error
	 */
	private String errorMsg = null;

	/**
	 * @param startDate
	 * @return The directory in which the track file should be created
	 * @throws ExportTrackException
	 */
	protected abstract File getExportDirectory(Date startDate) throws ExportTrackException;
	
	/**
	 * Whereas to export the media files or not
	 * @return
	 */
	protected abstract boolean exportMediaFiles();
	
	/**
	 * Whereas to update the track export date in the database at the end or not
	 * @return
	 */
	protected abstract boolean updateExportDate();

	public ExportTrackTask(Context context, long... trackIds) {
		this.context = context;
		this.trackIds = trackIds;
		pointDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	protected void onPreExecute() {
		// Display dialog
		dialog = new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.setMessage(context.getResources().getString(R.string.trackmgr_exporting_prepare));
		dialog.show();
	}
	
	
	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			for (int i=0; i<trackIds.length; i++) {
				exportTrackAsGpx(trackIds[i]);
			}
		} catch (ExportTrackException ete) {
			errorMsg = ete.getMessage();
			return false;
		}
		return true;
	}
	public void parentNotVisible(){
		if(getStatus() == Status.FINISHED || getStatus() == Status.RUNNING) {
			dialog.dismiss();
		}
	}
	@Override
	protected void onProgressUpdate(Long... values) {
		if (values.length == 1) {
			// Standard progress update
			dialog.incrementProgressBy(values[0].intValue());
		} else if (values.length == 3) {
			// To initialise the dialog, 3 values are passed to onProgressUpdate()
			// trackId, number of track points, number of waypoints
			if(dialog.isShowing()) {
				dialog.dismiss();
			}
			
			dialog = new ProgressDialog(context);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setCancelable(false);
			dialog.setProgress(0);
			dialog.setMax(values[1].intValue() + values[2].intValue());
			dialog.setTitle(
					context.getResources().getString(R.string.trackmgr_exporting)
					.replace("{0}", Long.toString(values[0])));
			dialog.show();

		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if(dialog.isShowing()) {
			dialog.dismiss();
		}
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
						if(((AlertDialog)dialog).isShowing()){
							dialog.dismiss();
						}
					}
				})
				.show();
		}
	}

	private void exportTrackAsGpx(long trackId) throws ExportTrackException {
		File sdRoot = Environment.getExternalStorageDirectory();
		
		if (sdRoot.canWrite()) {
			ContentResolver cr = context.getContentResolver();
			
			Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(
					TrackContentProvider.CONTENT_URI_TRACK, trackId), null, null,
					null, null);

			// Get the startDate of this track
			// TODO: Maybe we should be pulling the track name instead?
			// We'd need to consider the possibility that two tracks were given the same name
			// We could possibly disambiguate by including the track ID in the Folder Name
			// to avoid overwriting another track on one hand or needlessly creating additional
			// directories to avoid overwriting.
			Date startDate = new Date();
			if (null != c && 1 <= c.getCount()) {
				c.moveToFirst();
				long startDateInMilliseconds = c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_START_DATE));
				startDate.setTime(startDateInMilliseconds);
			}

			File trackGPXExportDirectory = getExportDirectory(startDate);
			String filenameBase = buildGPXFilename(c);
			c.close();
			
			File trackFile = new File(trackGPXExportDirectory, filenameBase);

			
			Cursor cTrackPoints = cr.query(TrackContentProvider.trackPointsUri(trackId), null,
					null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");
			Cursor cWayPoints = cr.query(TrackContentProvider.waypointsUri(trackId), null, null,
					null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");

			if (null != cTrackPoints && null != cWayPoints) {
				publishProgress(new Long[] { trackId, (long) cTrackPoints.getCount(), (long) cWayPoints.getCount() });
				
				try {
					writeGpxFile(cTrackPoints, cWayPoints, trackFile);
					if (exportMediaFiles()) {
						copyWaypointFiles(trackId, trackGPXExportDirectory);
					}
					if (updateExportDate()) {
						DataHelper.setTrackExportDate(trackId, System.currentTimeMillis(), cr);
					}
				} catch (IOException ioe) {
					throw new ExportTrackException(ioe.getMessage());
				} finally {
					cTrackPoints.close();
					cWayPoints.close();
				}

				// Force rescan of directory
				ArrayList<String> files = new ArrayList<String>();
				for (File file: trackGPXExportDirectory.listFiles()) {
					files.add(file.getAbsolutePath());
				}
				MediaScannerConnection.scanFile(context, files.toArray(new String[0]), null, null);

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
		String compassOutput = PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_OUTPUT_COMPASS,
				OSMTracker.Preferences.VAL_OUTPUT_COMPASS);
		
		Log.v(TAG, "write preferences: compass:" + compassOutput);
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(target));
			
			writer.write(XML_HEADER + "\n");
			writer.write(TAG_GPX + "\n");
			
			writeWayPoints(writer, cWayPoints, accuracyOutput, fillHDOP, compassOutput);
			writeTrackPoints(context.getResources().getString(R.string.gpx_track_name), writer, cTrackPoints, fillHDOP, compassOutput);
			
			writer.write("</gpx>");
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
	 * Iterates on track points and write them.
	 * @param trackName Name of the track (metadata).
	 * @param fw Writer to the target file.
	 * @param c Cursor to track points.
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @param compass Indicates if and how to write compass heading to the GPX ('none', 'comment', 'extension')
	 * @throws IOException
	 */
	private void writeTrackPoints(String trackName, Writer fw, Cursor c, boolean fillHDOP, String compass) throws IOException {
		// Update dialog every 1%
		int dialogUpdateThreshold = c.getCount() / 100;
		if (dialogUpdateThreshold == 0) {
			dialogUpdateThreshold++;
		}
		
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
		
		int i=0;
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext(),i++) {
			StringBuffer out = new StringBuffer();
			out.append("\t\t\t" + "<trkpt lat=\"" 
					+ c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE)) + "\" "
					+ "lon=\"" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE)) + "\">" + "\n");
			if (! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION))) {
				out.append("\t\t\t\t" + "<ele>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION)) + "</ele>" + "\n");
			}
			out.append("\t\t\t\t" + "<time>" + pointDateFormatter.format(new Date(c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_TIMESTAMP)))) + "</time>" + "\n");
			
			if(fillHDOP && ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY))) {
				out.append("\t\t\t\t" + "<hdop>" + (c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) / OSMTracker.HDOP_APPROXIMATION_FACTOR) + "</hdop>" + "\n");
			}
			if(OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass) && !c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
				out.append("\t\t\t\t" + "<cmt>"+CDATA_START+"compass: " + 
							c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))+
							"\n\t\t\t\t\tcompAccuracy: " + 
							c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY))+
				            CDATA_END+"</cmt>"+"\n");
			}
			
			String buff = "";
			if(! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_SPEED))) {
				buff += "\t\t\t\t\t" + "<speed>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_SPEED)) + "</speed>" + "\n";
			}
			if(OSMTracker.Preferences.VAL_OUTPUT_COMPASS_EXTENSION.equals(compass) && !c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
				buff += "\t\t\t\t\t" + "<compass>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) + "</compass>" + "\n";
				buff += "\t\t\t\t\t" + "<compass_accuracy>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) + "</compass_accuracy>" + "\n";
			}
			if(! buff.equals("")) {
				out.append("\t\t\t\t" + "<extensions>\n");
				out.append(buff);
				out.append("\t\t\t\t" + "</extensions>\n");
			}

			out.append("\t\t\t" + "</trkpt>" + "\n");
			fw.write(out.toString());

			if (i % dialogUpdateThreshold == 0) {
				publishProgress((long) dialogUpdateThreshold);
			}
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
	 * @param compass Indicates if and how to write compass heading to the GPX ('none', 'comment', 'extension')
	 * @throws IOException
	 */
	private void writeWayPoints(Writer fw, Cursor c, String accuracyInfo, boolean fillHDOP, String compass) throws IOException {

		// Update dialog every 1%
		int dialogUpdateThreshold = c.getCount() / 100;
		if (dialogUpdateThreshold == 0) {
			dialogUpdateThreshold++;
		}
		
		// Label for meter unit
		String meterUnit = context.getResources().getString(R.string.various_unit_meters);
		// Word "accuracy"
		String accuracy = context.getResources().getString(R.string.various_accuracy);
		
		int i=0;
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext(), i++) {
			StringBuffer out = new StringBuffer();
			out.append("\t" + "<wpt lat=\""
					+ c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE)) + "\" "
					+ "lon=\"" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE)) + "\">" + "\n");
			if (! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION))) {
				out.append("\t\t" + "<ele>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION)) + "</ele>" + "\n");
			}
			out.append("\t\t" + "<time>" + pointDateFormatter.format(new Date(c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_TIMESTAMP)))) + "</time>" + "\n");

			String name = c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
			
			if (! OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_NONE.equals(accuracyInfo) && ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY))) {
				// Outputs accuracy info for way point
				if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_NAME.equals(accuracyInfo)) {
					// Output accuracy with name
					out.append("\t\t" + "<name>"
							+ CDATA_START 
							+ name
							+ " (" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) + meterUnit + ")"
							+ CDATA_END
							+ "</name>" + "\n");
					if (OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass) &&
							! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
						out.append("\t\t"+ "<cmt>" + CDATA_START + "compass: " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) +
								"\n\t\t\tcompass accuracy: " + c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) + CDATA_END + "</cmt>\n");
					}
				} else if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_CMT.equals(accuracyInfo)) {
					// Output accuracy in separate tag
					out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
					if (OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass) &&
							! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
						out.append("\t\t" + "<cmt>" + CDATA_START + accuracy + ": " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) + meterUnit +
								"\n\t\t\t compass heading: " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) +
								"deg\n\t\t\t compass accuracy: " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) +CDATA_END + "</cmt>" + "\n");
					} else {
						out.append("\t\t" + "<cmt>" + CDATA_START + accuracy + ": " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) + meterUnit + CDATA_END + "</cmt>" + "\n");
					}
				} else {
					// Unknown value for accuracy info, shouldn't occur but who knows ?
					// See issue #68. Output at least the name just in case.
					out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
				}
			} else {
				// No accuracy info requested, or available
				out.append("\t\t" + "<name>" + CDATA_START + name + CDATA_END + "</name>" + "\n");
				if (OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass) &&
						! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
					out.append("\t\t"+ "<cmt>" + CDATA_START + "compass: " + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) +
							"\n\t\t\tcompass accuracy: " + c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) + CDATA_END + "</cmt>\n");
				}
			}
			
			String link = c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_LINK));
			if (link != null) {
					out.append("\t\t" + "<link href=\"" + URLEncoder.encode(link) + "\">" + "\n");
					out.append("\t\t\t" + "<text>" + link +"</text>\n");
					out.append("\t\t" + "</link>" + "\n");
			}
			
			if (! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_NBSATELLITES))) {
				out.append("\t\t" + "<sat>" + c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_NBSATELLITES)) + "</sat>" + "\n");
			}

			if(fillHDOP && ! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY))) {
				out.append("\t\t" + "<hdop>" + (c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)) / OSMTracker.HDOP_APPROXIMATION_FACTOR) + "</hdop>" + "\n");
			}

			if (OSMTracker.Preferences.VAL_OUTPUT_COMPASS_EXTENSION.equals(compass) &&
					! c.isNull(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
				out.append("\t\t<extensions>\n");
				out.append("\t\t\t"+ "<compass>" + c.getDouble(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)) + "</compass>\n");
				out.append("\t\t\t" + "<compass_accuracy>" + c.getInt(c.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)) + "</compass_accuracy>" + "\n");
				out.append("\t\t</extensions>\n");				
			}
			
			out.append("\t" + "</wpt>" + "\n");
			
			fw.write(out.toString());

			if (i % dialogUpdateThreshold == 0) {
				publishProgress((long) dialogUpdateThreshold);
			}
		}
	}

	/**
	 * Copy all files from the OSMTracker external storage location to gpxOutputDirectory
	 * @param gpxOutputDirectory The directory to which the track is being exported
	 */
	private void copyWaypointFiles(long trackId, File gpxOutputDirectory) {
		// Get the new location where files related to these waypoints are/should be stored		
		File trackDir = DataHelper.getTrackDirectory(trackId);

		if(trackDir != null){
			Log.v(TAG, "Copying files from the standard TrackDir ["+trackDir+"] to the export directory ["+gpxOutputDirectory+"]");
			FileSystemUtils.copyDirectoryContents(gpxOutputDirectory, trackDir);
		}
		
	}

	/**
	 * Build GPX filename from track info, based on preferences.
	 * The filename will have the start date, and/or the track name if available.
	 * If no name is available, fall back to the start date and time.
	 * Track name characters will be sanitized using {@link #FILENAME_CHARS_BLACKLIST_PATTERN}.
	 * @param c  Track info: {@link TrackContentProvider.Schema#COL_NAME}, {@link TrackContentProvider.Schema#COL_START_DATE}
	 * @return  GPX filename, not including the path
	 */
	protected String buildGPXFilename(Cursor c) {
		// Build GPX filename from track info & preferences
		final String filenameOutput = PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
				OSMTracker.Preferences.VAL_OUTPUT_FILENAME);
		StringBuffer filenameBase = new StringBuffer();
		final int colName = c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_NAME);
		if ((! c.isNull(colName))
			&& (! filenameOutput.equals(OSMTracker.Preferences.VAL_OUTPUT_FILENAME_DATE)))
		{
			final String tname_raw =
				c.getString(colName).trim().replace(':', ';');
			final String sanitized =
				FILENAME_CHARS_BLACKLIST_PATTERN.matcher(tname_raw).replaceAll("_");
			filenameBase.append(sanitized);
		}
		if ((filenameBase.length() == 0)
			|| ! filenameOutput.equals(OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME))
		{
			final long startDate = c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_START_DATE));
			if (filenameBase.length() > 0)
				filenameBase.append('_');
			filenameBase.append(DataHelper.FILENAME_FORMATTER.format(new Date(startDate)));
		}
		filenameBase.append(DataHelper.EXTENSION_GPX);
		return filenameBase.toString();
	}

}
