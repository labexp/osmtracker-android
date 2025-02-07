package net.osmtracker.gpx;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.exception.ExportTrackException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Exports to a temporary file. Will not export associated
 * media, only the GPX file.
 *
 */
public abstract class ExportToTempFileTask extends ExportTrackTask {

	private static final String TAG = ExportToTempFileTask.class.getSimpleName();
	
	private final File tmpFile;
	private String filename;
	
	public ExportToTempFileTask(Context context, long trackId) {
		super(context, trackId);
		try {
			String exportLabelName = PreferenceManager.getDefaultSharedPreferences(context).getString(
					OSMTracker.Preferences.KEY_OUTPUT_FILENAME_LABEL,	OSMTracker.Preferences.VAL_OUTPUT_FILENAME_LABEL);
			String trackName = new DataHelper(context).getTrackById(trackId).getName();
			long date = new DataHelper(context).getTrackById(trackId).getTrackDate();

			String formattedTrackStartDate = DataHelper.FILENAME_FORMATTER.format(new Date(date));

			// Create temporary file
			String namefinal = createFile(trackName, formattedTrackStartDate, exportLabelName);
			tmpFile = new File(context.getCacheDir(),namefinal+".gpx");
			Log.d(TAG, "Temporary file: "+ tmpFile.getAbsolutePath());
		} catch (Exception ioe) {
			Log.e(TAG, "Could not create temporary file", ioe);
			throw new IllegalStateException("Could not create temporary file", ioe);
		}
	}
	//create temporary file
	private String createFile(String sanitizedTrackName, String formattedTrackStartDate, String exportLabelName) throws IOException{
		String result = "";
		String desiredOutputFormat = PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
				OSMTracker.Preferences.VAL_OUTPUT_FILENAME);

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
					if(sanitizedTrackName.equals(formattedTrackStartDate)) {
						result += sanitizedTrackName;
					}else{
						result += sanitizedTrackName + "_"  + formattedTrackStartDate; // name is not equal
					}
				else
					result += formattedTrackStartDate;
				break;
			case OSMTracker.Preferences.VAL_OUTPUT_FILENAME_DATE_NAME:
				if(thereIsTrackName){
					if(sanitizedTrackName.equals(formattedTrackStartDate)){
						result += formattedTrackStartDate;
					}else{
						result += formattedTrackStartDate  + "_" + sanitizedTrackName;
					}
				}else{
					result += formattedTrackStartDate;
				}
				break;
			case OSMTracker.Preferences.VAL_OUTPUT_FILENAME_DATE:
				result += formattedTrackStartDate;
				break;
		}
		if(!(exportLabelName.equals("")))
			result += "_"+ exportLabelName;
		return result;
	}

	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
		return tmpFile.getParentFile();
	}

	@Override
	public String buildGPXFilename(Cursor c, File parentDirectory) {
		filename = super.buildGPXFilename(c, parentDirectory);
		return tmpFile.getName();
	}

	@Override
	protected boolean exportMediaFiles() {
		return false;
	}
	
	@Override
	protected boolean updateExportDate() {
		return false;
	}
	
	public File getTmpFile() {
		return tmpFile;
	}
	
	public String getFilename() {
		return filename;
	}
	
	@Override
	protected void onPostExecute(Boolean success) {
		super.onPostExecute(success);
		executionCompleted();
	}
	
	protected abstract void executionCompleted();
}
