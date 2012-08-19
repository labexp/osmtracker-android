package me.guillaumin.android.osmtracker.gpx;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import me.guillaumin.android.osmtracker.exception.ExportTrackException;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

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
			tmpFile = File.createTempFile("osm-upload", ".gpx", context.getCacheDir());
			Log.d(TAG, "Temporary file: " + tmpFile.getAbsolutePath());
		} catch (IOException ioe) {
			Log.e(TAG, "Could not create temporary file", ioe);
			throw new IllegalStateException("Could not create temporary file", ioe);
		}
	}
	
	
	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
		return tmpFile.getParentFile();
	}

	@Override
	protected String buildGPXFilename(Cursor c) {
		filename = super.buildGPXFilename(c);
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
