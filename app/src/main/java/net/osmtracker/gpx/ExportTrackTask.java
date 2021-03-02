package net.osmtracker.gpx;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;
import net.osmtracker.exception.ExportTrackException;
import net.osmtracker.util.FileSystemUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static net.osmtracker.db.DataHelper.EXTENSION_GPX;
import static net.osmtracker.util.FileSystemUtils.getUniqueChildNameFor;

/**
 * Base class to writes a GPX file and export
 * track media (Photos, Sounds)
 *
 * @author Nicolas Guillaumin
 *
 */
public abstract class ExportTrackTask extends AsyncTask<Void, Long, Boolean> {

	private static final String TAG = ExportTrackTask.class.getSimpleName();

	/**
	 * Characters to replace in track filename, for use by buildGPXFilename. <BR>
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
			+ " creator=\"OSMTracker for Android™ - https://github.com/labexp/osmtracker-android\""
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
	private long[] trackIds;

	/**
	 * Dialog to display while exporting
	 */
	protected ProgressDialog dialog;

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
			for (long trackId : trackIds) {
				exportTrackAsGpx(trackId);
			}
		} catch (ExportTrackException ete) {
			errorMsg = ete.getMessage();
			return false;
		}
		return true;
	}

	@Override
	protected void onProgressUpdate(Long... values) {
		if (values.length == 1) {
			// Standard progress update
			dialog.incrementProgressBy(values[0].intValue());
		} else if (values.length == 3) {
			// To initialise the dialog, 3 values are passed to onProgressUpdate()
			// trackId, number of track points, number of waypoints
			dialog.dismiss();

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
		}else{
			Toast.makeText(this.context, R.string.various_export_finished, Toast.LENGTH_SHORT).show();
		}
	}

	protected void exportTrackAsGpx(long trackId) throws ExportTrackException {

		String state = Environment.getExternalStorageState();
		File sdRoot = Environment.getExternalStorageDirectory();

		if (ContextCompat.checkSelfPermission(context,
				Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

			if (sdRoot.canWrite()) {

				//refactoring
				DataHelper dh = new DataHelper(context);
				Track track = dh.getTrackById(trackId);

				// Get the startDate of this track
				// TODO: Maybe we should be pulling the track name instead?
				// We'd need to consider the possibility that two tracks were given the same name
				// We could possibly disambiguate by including the track ID in the Folder Name
				// to avoid overwriting another track on one hand or needlessly creating additional
				// directories to avoid overwriting.
				Date startDate = new Date();
				long startDateInMilliseconds = track.getTrackDate();
				startDate.setTime(startDateInMilliseconds);

				File trackGPXExportDirectory = getExportDirectory(startDate);
				String filenameBase = buildGPXFilename(track, trackGPXExportDirectory);
				File trackFile = new File(trackGPXExportDirectory, filenameBase);

				// always try to writGPXFile no matter if there are points
				try {
					writeGpxFile(track, trackFile);

					if (exportMediaFiles()) {
						copyWaypointFiles(trackId, trackGPXExportDirectory);
					}
					if (updateExportDate()) {
						dh.setTrackExportDate(trackId, System.currentTimeMillis());
					}
				} catch (IOException ioe) {
					throw new ExportTrackException(ioe.getMessage());
				}

				// Force rescan of directory
				ArrayList<String> files = new ArrayList<String>();
				for (File file : trackGPXExportDirectory.listFiles()) {
					files.add(file.getAbsolutePath());
				}
				MediaScannerConnection.scanFile(context, files.toArray(new String[0]),
						null, null);
			} else {
				throw new ExportTrackException(context.getResources()
						.getString(R.string.error_externalstorage_not_writable));
			}
		}
	}

	/**
	 * Writes the GPX file
	 * @param target Target GPX file
	 * @throws IOException
	 */
	protected void writeGpxFile(Track track, File target)
			throws IOException {

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

			String metadata = buildMetadataString(track);
			writer.write(metadata);

			writeWayPoints(writer, track.getTrackId(), accuracyOutput, fillHDOP, compassOutput);
			writeTrackPoints(writer, track.getTrackId(), fillHDOP, compassOutput);

			writer.write("</gpx>");

		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Build a string with the metadata of the gpx
	 *
	 * @param track
	 * @return
	 */
	protected String buildMetadataString(Track track) {
		StringBuilder metadata = new StringBuilder();
		metadata.append("\t<metadata>\n");

		// Write the track's name to a tag
		String trackName = track.getName();
		if((trackName != null && !trackName.equals(""))) {
			metadata.append("\t\t<name>" + trackName + "</name>\n");
		}

		for (String tag : track.getTags()) {
			metadata.append("\t\t<keywords>" + tag.trim() + "</keywords>\n");
		}

		String trackDescription = track.getDescription();
		if ((trackDescription != null && !trackDescription.equals(""))){
			metadata.append("\t\t<desc>" + trackDescription + "</desc>\n");
		}

		metadata.append("\t</metadata>\n");
		return metadata.toString();
	}


	/**
	 * Iterates on track points and write them.
	 * @param fw Writer to the target file.
	 * @param trackId
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @param compass Indicates if and how to write compass heading to the GPX ('none', 'comment', 'extension')
	 * @throws IOException
	 */
	private void writeTrackPoints(Writer fw, long trackId, boolean fillHDOP, String compass) throws IOException {
		DataHelper dh = new DataHelper(context);
		List<Integer> trackPointsId = dh.getTrackPointIdsOfTrack(trackId);

		fw.write("\t" + "<trk>" + "\n");
		String GPXTrackName = context.getResources().getString(R.string.gpx_track_name);
		fw.write("\t\t" + "<name>" + CDATA_START + GPXTrackName + CDATA_END + "</name>" + "\n");
		if (fillHDOP) {
			fw.write("\t\t" + "<cmt>"
					+ CDATA_START
					+ context.getResources().getString(R.string.gpx_hdop_approximation_cmt)
					+ CDATA_END
					+ "</cmt>" + "\n");
		}
		fw.write("\t\t" + "<trkseg>" + "\n");

		TrackPoint trkpt;
		String trkptString;
		for (Integer trackPointId : trackPointsId) {
			trkpt = dh.getTrackPointById(trackPointId);
			trkptString = buildTrackPointString(trkpt, fillHDOP, compass);
			fw.write(trkptString);

		}

		fw.write("\t\t" + "</trkseg>" + "\n");
		fw.write("\t" + "</trk>" + "\n");
	}

	/**
	 *
	 * @param trkpt
	 * @param fillHDOP
	 * @param compass
	 * @return
	 */
	protected String buildTrackPointString(TrackPoint trkpt, boolean fillHDOP, String compass) {
		StringBuilder out = new StringBuilder();
		out.append("\t\t\t" + "<trkpt lat=\"" + trkpt.getLatitude() + "\" "
				+ "lon=\"" + trkpt.getLongitude() + "\">" + "\n");
		if (trkpt.getElevation() != null) {
			out.append("\t\t\t\t" + "<ele>" + trkpt.getElevation() + "</ele>" + "\n");
		}
		out.append("\t\t\t\t" + "<time>"
				+ pointDateFormatter.format(new Date(trkpt.getPointTimestamp()))
				 + "</time>" + "\n");

		if(fillHDOP && trkpt.getAccuracy() != null) {
			out.append("\t\t\t\t" + "<hdop>"
					+ (trkpt.getAccuracy() / OSMTracker.HDOP_APPROXIMATION_FACTOR)
					+ "</hdop>" + "\n");
		}
		if(OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass)
				&& trkpt.getCompassHeading() != null) {
			out.append("\t\t\t\t" + "<cmt>"+CDATA_START+"compass: " + trkpt.getCompassHeading()
					+ "\n\t\t\t\t\tcompAccuracy: " + trkpt.getCompassAccuracy()
					+ CDATA_END+"</cmt>"+"\n");
		}

		String extensions = "";
		if(trkpt.getSpeed() != null) {
			extensions += "\t\t\t\t\t" + "<speed>" + trkpt.getSpeed() + "</speed>" + "\n";
		}
		if ( OSMTracker.Preferences.VAL_OUTPUT_COMPASS_EXTENSION.equals(compass)
				&& trkpt.getCompassHeading() != null ) {
			extensions += "\t\t\t\t\t" + "<compass>" + trkpt.getCompassHeading() + "</compass>" + "\n";
			extensions += "\t\t\t\t\t" + "<compass_accuracy>" + trkpt.getCompassAccuracy()
					+ "</compass_accuracy>" + "\n";
		}

		//Checking if the database contains atmospheric_pressure data
		if (trkpt.getAtmosphericPressure() != null ) {
			double pressure = trkpt.getAtmosphericPressure();
			String pressure_formatted = String.format("%.1f", pressure);
			extensions += "\t\t\t\t\t" + "<baro>" + pressure_formatted + "</baro>" + "\n";
		}

		if(! extensions.equals("")) {
			out.append("\t\t\t\t" + "<extensions>\n");
			out.append(extensions);
			out.append("\t\t\t\t" + "</extensions>\n");
		}

		out.append("\t\t\t" + "</trkpt>" + "\n");

		return out.toString();
	}


	/**
	 * Iterates on way points and write them.
	 * @param fw Writer to the target file.
	 * @param trackId
	 * @param accuracyInfo Constant describing how to include (or not) accuracy info for way points.
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @param compass Indicates if and how to write compass heading to the GPX ('none', 'comment',
	 *                   'extension')
	 * @throws IOException
	 */
	private void writeWayPoints(Writer fw, long trackId, String accuracyInfo, boolean fillHDOP,
								String compass) throws IOException {

		DataHelper dh = new DataHelper(context);
		List<Integer> waypointIds = dh.getWayPointIdsOfTrack(trackId);

		WayPoint wpt;
		String wptString;
		for (Integer wayPointId : waypointIds) {
			wpt = dh.getWayPointById(wayPointId);
			wptString = buildWayPointString(wpt, accuracyInfo, fillHDOP, compass);
			fw.write(wptString);
		}
	}


	/**
	 * @param wpt WayPoint object
	 * @param accuracyInfo Constant describing how to include (or not) accuracy info for way points.
	 * @param fillHDOP Indicates whether fill <hdop> tag with approximation from location accuracy.
	 * @param compass Indicates if and how to write compass heading to the GPX ('none', 'comment',
	 *                  'extension')
	 * @return
	 */
	protected String buildWayPointString(WayPoint wpt, String accuracyInfo, boolean fillHDOP,
											  String compass) {

		// Label for meter unit
		String meterUnit = context.getResources().getString(R.string.various_unit_meters);
		// Word "accuracy"
		String accuracy = context.getResources().getString(R.string.various_accuracy);

		StringBuilder out = new StringBuilder();
		out.append("\t" + "<wpt lat=\"" + wpt.getLatitude() + "\" " + "lon=\""
				+ wpt.getLongitude() + "\">" + "\n");
		if (wpt.getElevation() != null) {
			out.append("\t\t" + "<ele>" + wpt.getElevation() + "</ele>" + "\n");
		}
		out.append("\t\t" + "<time>" + pointDateFormatter.format(
				new Date(wpt.getPointTimestamp())) + "</time>" + "\n");

		// name (optionally with accuracy)
		out.append("\t\t" + "<name>" + CDATA_START + wpt.getName());
		// accuracy should be included with name?
		if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_NAME.equals(accuracyInfo)
				&& wpt.getAccuracy() != null) {
			// Add accuracy to name
			out.append(" (" + wpt.getAccuracy() + meterUnit + ")");
		}
		out.append(CDATA_END + "</name>" + "\n");

		// Comment with accuracy and/or compass (optionally)
		String comment = "";
		if (OSMTracker.Preferences.VAL_OUTPUT_ACCURACY_WPT_CMT.equals(accuracyInfo)
				&& wpt.getAccuracy() != null) {
			comment += accuracy + ": " + wpt.getAccuracy() + meterUnit;
		}
		if (OSMTracker.Preferences.VAL_OUTPUT_COMPASS_COMMENT.equals(compass)
				&& wpt.getCompassHeading() != null) {
			if (! comment.equals("") ) {
				comment += "\n\t\t\t";
			}
			comment += "compass heading: " + wpt.getCompassHeading() + "deg"
					+ "\n\t\t\tcompass accuracy: " + wpt.getCompassAccuracy();
		}
		if (! comment.equals("")) {
			out.append("\t\t" + "<cmt>" + CDATA_START + comment + CDATA_END + "</cmt>" + "\n");
		}


		String link = wpt.getLink();
		if (link != null) {
			out.append("\t\t" + "<link href=\"" + URLEncoder.encode(link) + "\">" + "\n");
			out.append("\t\t\t" + "<text>" + link +"</text>\n");
			out.append("\t\t" + "</link>" + "\n");
		}

		if (wpt.getNumberOfSatellites() != null) {
			out.append("\t\t" + "<sat>" + wpt.getNumberOfSatellites() + "</sat>" + "\n");
		}

		if(fillHDOP && wpt.getAccuracy() != null) {
			out.append("\t\t" + "<hdop>"
					+ (wpt.getAccuracy() / OSMTracker.HDOP_APPROXIMATION_FACTOR)
					+ "</hdop>" + "\n");
		}


		String extensions = "";

		if(OSMTracker.Preferences.VAL_OUTPUT_COMPASS_EXTENSION.equals(compass)
				&& wpt.getCompassHeading() != null) {
			extensions += "\t\t\t\t\t" + "<compass>" + wpt.getCompassHeading()
					+ "</compass>" + "\n";
			extensions += "\t\t\t\t\t" + "<compass_accuracy>" + wpt.getCompassAccuracy()
					+ "</compass_accuracy>" + "\n";
		}

		//Checking if the database contains atmospheric_pressure data
		if (wpt.getAtmosphericPressure() != null) {
			double pressure = wpt.getAtmosphericPressure();
			String pressure_formatted = String.format("%.1f", pressure);
			extensions += "\t\t\t\t\t" + "<baro>" + pressure_formatted + "</baro>" + "\n";
		}

		if(! extensions.equals("")) {
			out.append("\t\t\t\t" + "<extensions>\n");
			out.append(extensions);
			out.append("\t\t\t\t" + "</extensions>\n");
		}

		out.append("\t" + "</wpt>" + "\n");

		return out.toString();

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
	 * @param track  Track info: {@link Track}
	 * @return  GPX filename, not including the path
	 */
	public String buildGPXFilename(Track track, File parentDirectory) {
		String desiredOutputFormat = PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
				OSMTracker.Preferences.VAL_OUTPUT_FILENAME);

		long trackStartDate = track.getTrackDate();
		String formattedTrackStartDate = DataHelper.FILENAME_FORMATTER.format(new Date(trackStartDate));

		String trackName =  track.getName();
		if(trackName != null)
			trackName = sanitizeTrackName(trackName);

		String firstGpxFilename = formatGpxFilename(desiredOutputFormat, trackName, formattedTrackStartDate);

		firstGpxFilename = getUniqueChildNameFor(parentDirectory, firstGpxFilename, EXTENSION_GPX);
		return firstGpxFilename;

	}

	public String formatGpxFilename(String desiredOutputFormat, String sanitizedTrackName, String formattedTrackStartDate){
		String result = "";
		boolean thereIsTrackName = sanitizedTrackName != null && sanitizedTrackName.length() >= 1;

		switch(desiredOutputFormat){
			case OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME:
				if(thereIsTrackName)
					result += sanitizedTrackName;
				else
					result += formattedTrackStartDate; // fallback case
				break;
			case OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME_DATE:
				if(thereIsTrackName)
					result += sanitizedTrackName + "_" + formattedTrackStartDate;
				else
					result += formattedTrackStartDate;
				break;
			case OSMTracker.Preferences.VAL_OUTPUT_FILENAME_DATE:
				result += formattedTrackStartDate;
				break;
		}
		return result;
	}



	public String sanitizeTrackName(String trackName){
		String first = trackName.trim().replace(':', ';');
		String second = FILENAME_CHARS_BLACKLIST_PATTERN.matcher(first).replaceAll("_");
		return second;
	}

	/**
	 * Message in case of an error
	 */
	public String getErrorMsg() {
		return errorMsg;
	}
}