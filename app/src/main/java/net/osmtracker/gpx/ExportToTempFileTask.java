package net.osmtracker.gpx;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;
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
					OSMTracker.Preferences.KEY_EXPORT_LABEL,	OSMTracker.Preferences.VAL_EXPORT_LABEL);
			String trackName = new DataHelper(context).getTrackById(trackId).getName();

			// Create temporary file
			tmpFile = new File(context.getCacheDir(), trackName + exportLabelName+".gpx");
			Log.d(TAG, "Temporary file: " + tmpFile.getAbsolutePath());

		} catch (Exception ioe) {
			Log.e(TAG, "Could not create temporary file", ioe);
			throw new IllegalStateException("Could not create temporary file", ioe);
		}
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
