package me.guillaumin.android.osmtracker.gpx;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

/**
 * Exports to a temporary file. Will not export associated
 * media, only the GPX file.
 * If this track has already been exported, tries to find and re-use that file.
 *
 */
public abstract class ExportToTempFileTask extends ExportTrackTask {

	private static final String TAG = ExportToTempFileTask.class.getSimpleName();
	
	/** Temp file created in constructor, or actual export file if found */
	private File tmpFile;
	private String filename;
	/** True if a previous export file was found */
	private boolean reusedRealExport = false;
	
	public ExportToTempFileTask(Context context, long trackId) {
		super(context, trackId);
		tmpFile = checkForExportFile();
		if (tmpFile != null)
			return;
		try {
			tmpFile = File.createTempFile("osm-upload", ".gpx", context.getCacheDir());
			Log.d(TAG, "Temporary file: " + tmpFile.getAbsolutePath());
		} catch (IOException ioe) {
			Log.e(TAG, "Could not create temporary file", ioe);
			throw new IllegalStateException("Could not create temporary file", ioe);
		}
	}
	
	/**
	 * Checks whether this file's already been exported, based on the track
	 * db and our naming conventions. If so, we can reuse that GPX file,
	 * instead of creating a new temporary one.
	 * Sets {@link #reusedRealExport} but does not set {@link #tmpFile}.
	 * @return  Export file if it exists
	 */
	private File checkForExportFile() {
		reusedRealExport = false;

		Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(
				TrackContentProvider.CONTENT_URI_TRACK, trackId), null, null,
				null, null);
		if (null == c)
			return null;  // <--- Early return: could not query ---

		// Get the exportDate and startDate of this track
		Date exportDate = null, startDate = null;
		if (1 <= c.getCount()) {
			c.moveToFirst();
			final int eidx = c.getColumnIndex(Schema.COL_EXPORT_DATE);
			if (! c.isNull(eidx))
			{
				exportDate = new Date();
				exportDate.setTime(c.getLong(eidx));
				startDate = new Date();
				startDate.setTime(c.getLong(c.getColumnIndex(Schema.COL_START_DATE)));
			}
		}

		if (exportDate == null) {
			c.close();
			return null;  // <--- Early return: Not exported ---
		}

		final String exportDirectoryPath = ExportToStorageTask.getExportDirectory(context, startDate);
		final File sdRoot = Environment.getExternalStorageDirectory();
		File expDir = new File(sdRoot + exportDirectoryPath);
		if (! expDir.exists())
			expDir = new File(exportDirectoryPath); // Specific hack for Google Nexus S (See issue #168)
		if (! expDir.exists()) {
			c.close();
			return null;  // <--- Early return: Export dir doesn't exist ---
		}

		// Export dir exists. Does the GPX file exist with the expected name?
		final String filenameBase = super.buildGPXFilename(c);
		c.close();
		File f = new File(expDir, filenameBase);
		if (f.exists()) {
			reusedRealExport = true;
			return f;
		} else {
			return null;
		}
	}

	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
		return tmpFile.getParentFile();
	}

	@Override
	protected String buildGPXFilename(Cursor c) {
		if (reusedRealExport)
			filename = tmpFile.getName();
		else
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
	protected Boolean doInBackground(Void... params) {
		if (reusedRealExport) {
			trackFile = tmpFile;
			return true;
		} else {
			return super.doInBackground(params);
		}
	}

	@Override
	protected void onPostExecute(Boolean success) {
		super.onPostExecute(success);
		executionCompleted();
	}
	
	protected abstract void executionCompleted();
}
