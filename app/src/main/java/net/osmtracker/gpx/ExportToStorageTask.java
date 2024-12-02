package net.osmtracker.gpx;

import static net.osmtracker.util.FileSystemUtils.getUniqueChildNameFor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.exception.ExportTrackException;

import java.io.File;
import java.util.Date;

/**
 * ExportToStorageTask is responsible for exporting track data to the device's storage.
 * It extends the ExportTrackTask class and provides specific implementations for
 * exporting track data to a directory on the external storage.
 */
public class ExportToStorageTask extends ExportTrackTask {

	private static final String TAG = ExportToStorageTask.class.getSimpleName();
	private final String ERROR_MESSAGE;
	private final DataHelper dataHelper;
	private final SharedPreferences sharedPreferences;

	/**
	 * Constructor for ExportToStorageTask.
	 *
	 * @param context the context of the application
	 * @param trackId the IDs of the tracks to be exported
	 */
	public ExportToStorageTask(Context context, long... trackId) {
		this(context, new DataHelper(context), trackId);
	}

	/**
	 * Constructor for ExportToStorageTask with a DataHelper instance.
	 *
	 * @param context the context of the application
	 * @param dataHelper the DataHelper instance for accessing track data
	 * @param trackId the IDs of the tracks to be exported
	 */
	public ExportToStorageTask(Context context, DataHelper dataHelper, long... trackId) {
		super(context, trackId);
		this.dataHelper = dataHelper;
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		ERROR_MESSAGE = context.getString(R.string.error_create_track_dir);
	}

	/**
	 * Gets the directory where the track data will be exported.
	 *
	 * @param startDate the start date of the track
	 * @return the directory where the track data will be exported
	 * @throws ExportTrackException if the directory cannot be created
	 */
	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
        String trackName = getSanitizedTrackNameByStartDate(startDate);
        boolean shouldCreateDirectoryPerTrack = shouldCreateDirectoryPerTrack();
        File finalExportDirectory = getBaseExportDirectory();
		Log.d(TAG, "absolute dir: " + finalExportDirectory.getAbsolutePath());

        if( shouldCreateDirectoryPerTrack && !trackName.isEmpty()){
            String uniqueFolderName = getUniqueChildNameFor(finalExportDirectory, trackName, "");
            finalExportDirectory = new File(finalExportDirectory, uniqueFolderName);
            finalExportDirectory.mkdirs();
        }
        if(! finalExportDirectory.exists() )
            throw new ExportTrackException(ERROR_MESSAGE);

        return finalExportDirectory;
	}

	/**
	 * Gets a sanitized track name based on the start date.
	 *
	 * @param startDate the start date of the track
	 * @return the sanitized track name
	 */
	public String getSanitizedTrackNameByStartDate(Date startDate) {
		Track track = dataHelper.getTrackByStartDate(startDate);

		String trackName = "";
		if (track != null) {
			trackName = track.getName();
		}
		if (trackName != null && !trackName.isEmpty()) {
			trackName = trackName.replace("/", "_").trim();
		}
		return trackName;
	}

	/**
	 * Determines whether a separate directory should be created for each track.
	 *
	 * @return true if a separate directory should be created for each track, false otherwise
	 */
    public boolean shouldCreateDirectoryPerTrack(){
	    return sharedPreferences.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK,
                OSMTracker.Preferences.VAL_OUTPUT_GPX_OUTPUT_DIR_PER_TRACK);
    }

	/**
	 * Checks if external storage is writable.
	 *
	 * @return true if external storage is writable, false otherwise
	 */
	private boolean isExternalStorageWritable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	/**
	 * Gets the base directory where the track data will be exported.
	 * Creates the directory if it does not exist.
	 *
	 * @return the base directory where the track data will be exported
	 * @throws ExportTrackException if the directory cannot be created or is not writable
	 */
	public File getBaseExportDirectory() throws ExportTrackException {

		if (!isExternalStorageWritable()) {
			throw new ExportTrackException(
					context.getResources().getString(R.string.error_externalstorage_not_writable));
		}
		String exportDirectoryNameInPreferences = sharedPreferences.getString(
				OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);
		Log.d(TAG,"exportDirectoryNameInPreferences: " + exportDirectoryNameInPreferences);

		File baseExportDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				exportDirectoryNameInPreferences);
		
		// if folder not exists, create it
		if (!baseExportDirectory.exists()) {
			boolean ok = baseExportDirectory.mkdirs();
			if (!ok) {
				throw new ExportTrackException(
						context.getResources().getString(R.string.error_externalstorage_not_writable));
			}
		}

		Log.d(TAG, "BaseExportDirectory: " + baseExportDirectory.getAbsolutePath());
		return baseExportDirectory;
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
