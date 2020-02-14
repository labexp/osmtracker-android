package net.osmtracker.gpx;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.exception.ExportTrackException;

import java.io.File;
import java.util.Date;

/**
 * Exports to the external storage / SD card
 * in a folder defined by the user.
 */
public class ExportToStorageTask extends ExportTrackTask {

	private static final String TAG = ExportToStorageTask.class.getSimpleName();

	public ExportToStorageTask(Context context, long... trackId) {
		super(context, trackId);
	}

	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
		File sdRoot = Environment.getExternalStorageDirectory();
		
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

			String trackName = "";

			// Get the name of the track with the received start date
			String selection = TrackContentProvider.Schema.COL_START_DATE + " = ?";
			String[] args = {String.valueOf(startDate.getTime())};

			Cursor c = context.getContentResolver().query(
					TrackContentProvider.CONTENT_URI_TRACK, null, selection, args, null);
			
			if(c != null && c.moveToFirst()){
				int i = c.getColumnIndex(TrackContentProvider.Schema.COL_NAME);
				trackName = c.getString(i);
			}
			if(trackName != null && trackName.length() >= 1) {
				trackName = trackName.replace("/", "_");
				perTrackDirectory = File.separator + trackName.trim();
			}

		}
		
		// Create a file based on the path we've generated above
		File trackGPXExportDirectory = new File(sdRoot + exportDirectoryPath + perTrackDirectory);

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
					trackGPXExportDirectory = new File(exportDirectoryPath + perTrackDirectory);
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
}
