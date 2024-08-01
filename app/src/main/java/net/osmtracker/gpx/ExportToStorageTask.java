package net.osmtracker.gpx;

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

import static net.osmtracker.util.FileSystemUtils.getUniqueChildNameFor;

import androidx.core.content.ContextCompat;

/**
 * Exports to the external storage / SD card
 * in a folder defined by the user.
 */
public class ExportToStorageTask extends ExportTrackTask {

	private static final String TAG = ExportToStorageTask.class.getSimpleName();
	private String ERROR_MESSAGE;


	public ExportToStorageTask(Context context, long... trackId) {
		super(context, trackId);
		ERROR_MESSAGE = context.getString(R.string.error_create_track_dir);
	}

	@Override
	protected File getExportDirectory(Date startDate) throws ExportTrackException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String trackName = getSanitizedTrackNameByStartDate(startDate);
        boolean shouldCreateDirectoryPerTrack = shouldCreateDirectoryPerTrack(preferences);
        File finalExportDirectory = getBaseExportDirectory(preferences);

        if( shouldCreateDirectoryPerTrack && trackName.length() >= 1){
            String uniqueFolderName = getUniqueChildNameFor(finalExportDirectory, trackName, "");
            finalExportDirectory = new File(finalExportDirectory, uniqueFolderName);
            finalExportDirectory.mkdirs();
        }
        if(! finalExportDirectory.exists() )
            throw new ExportTrackException(ERROR_MESSAGE);

        return finalExportDirectory;
	}


    /**
     *
     * @param startDate
     * @return
     */
    public String getSanitizedTrackNameByStartDate(Date startDate){

        DataHelper dh = new DataHelper(context);
        Track track = dh.getTrackByStartDate(startDate);

        String trackName = "";
        if(track != null){
            trackName = track.getName();
        }
        if(trackName != null && trackName.length() >= 1) {
            trackName = trackName.replace("/", "_").trim();
        }
        return trackName;
    }

    public boolean shouldCreateDirectoryPerTrack(SharedPreferences prefs){
	    return prefs.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK,
                OSMTracker.Preferences.VAL_OUTPUT_GPX_OUTPUT_DIR_PER_TRACK);

    }

	// Checks if a volume containing external storage is available for read and write.
	private boolean isExternalStorageWritable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	// Create before returning if not exists
    public File getBaseExportDirectory(SharedPreferences prefs) throws ExportTrackException {

		if (!isExternalStorageWritable()) {
			throw new ExportTrackException(
					context.getResources().getString(R.string.error_externalstorage_not_writable));
		}

		String exportDirectoryNameInPreferences = prefs.getString(
				OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);
		Log.d(TAG,"exportDirectoryNameInPreferences: " + exportDirectoryNameInPreferences);

		File baseExportDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				exportDirectoryNameInPreferences);

		if(! baseExportDirectory.exists()){
			boolean ok = baseExportDirectory.mkdirs();
			if (!ok) {
				throw new ExportTrackException(
						context.getResources().getString(
								R.string.error_externalstorage_not_writable));
			}
		}

		Log.d(TAG, "BaseExportDirectory: " + baseExportDirectory);
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
