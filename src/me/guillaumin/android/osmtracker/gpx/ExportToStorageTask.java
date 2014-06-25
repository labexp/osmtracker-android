package me.guillaumin.android.osmtracker.gpx;

import java.io.File;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Exports to the external storage / SD card
 * in a folder defined by the user.
 */
public class ExportToStorageTask extends ExportTrackTask {

	private static final String TAG = ExportToStorageTask.class.getSimpleName();

	public ExportToStorageTask(Context context, long trackId) {
		super(context, trackId);
	}

	/**
	 * Calculate a track's export directory, and create if it doesn't exist already.
	 * @param startDate  The track's starting date, from
	 *   {@link me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema#COL_START_DATE Schema.COL_START_DATE}
	 * @return The export directory
	 * @throws ExportTrackException if the directory can't be created
	 * @see #getExportDirectory(Context, Date)
	 */
	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
		File sdRoot = Environment.getExternalStorageDirectory();
		final String exportDirectoryPath = getExportDirectory(context, startDate);		
		
		// Create a file based on the path we've generated above
		File trackGPXExportDirectory = new File(sdRoot + exportDirectoryPath);

		// Create track directory if needed
		if (! trackGPXExportDirectory.exists()) {
			if (! trackGPXExportDirectory.mkdirs()) {
				Log.w(TAG,"Failed to create directory [" 
						+trackGPXExportDirectory.getAbsolutePath()+ "]");
			}
			
			if (! trackGPXExportDirectory.exists()) {
				// Specific hack for Google Nexus  S(See issue #168)
				if (android.os.Build.MODEL.equals(OSMTracker.Devices.NEXUS_S)) {
					// exportDirectoryPath always starts with "/"
					trackGPXExportDirectory = new File(exportDirectoryPath);
					trackGPXExportDirectory.mkdirs();
				}
			}
			
			if (! trackGPXExportDirectory.exists()) {
				throw new ExportTrackException(context.getResources().getString(R.string.error_create_track_dir,
						trackGPXExportDirectory.getAbsolutePath()));
			}
		}

		return trackGPXExportDirectory;
	}	

	@Override
	protected boolean exportMediaFiles() {
		return true;
	}
	
	@Override
	protected boolean updateExportDate() {
		return true;
	}

	/**
	 *<p>Calculate a track's export directory path. Does not create the directory if missing.</p>
	 *
	 *<p>The returned directory is relative to <tt>sdRoot</tt> because of bug #168
	 * mentioned in {@link #getExportDirectory(Date)}.  If you can't find the
	 * returned directory at <tt>sdRoot + directory</tt>, try just <tt>directory</tt>.</p>
	 *
	 * @param context  Calling activity context, for {@link PreferenceManager#getDefaultSharedPreferences(Context)}
	 * @param startDate  The track's starting date, from
	 *   {@link me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema#COL_START_DATE Schema.COL_START_DATE}
	 * @return track export directory name within sdcard;
	 *   The full path to dirname is {@link Environment#getExternalStorageDirectory()} + dirname
	 * @see #getExportDirectory(Date)
	 */
	public static String getExportDirectory(Context context, Date startDate) {

		// The location that the user has specified gpx files 
		// and associated content to be written
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String userGPXExportDirectoryName = prefs.getString(
				OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);

		boolean directoryPerTrack = prefs.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK, 
				OSMTracker.Preferences.VAL_OUTPUT_GPX_OUTPUT_DIR_PER_TRACK);
				
		// Create the path to the directory to which we will be writing
		// Trim the directory name, as additional spaces at the end will 
		// not allow the directory to be created if required
		String exportDirectoryPath = userGPXExportDirectoryName.trim();
		String perTrackDirectory = "";
		if (directoryPerTrack) {
			// If the user wants a directory per track, then create a name for the destination directory
			// based on the start date of the track
			perTrackDirectory = File.separator + DataHelper.FILENAME_FORMATTER.format(startDate);
		}

		return exportDirectoryPath + perTrackDirectory;
	}
}
