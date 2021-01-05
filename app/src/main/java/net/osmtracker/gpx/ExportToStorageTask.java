package net.osmtracker.gpx;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.exception.ExportTrackException;

import java.io.File;
import java.util.Date;

import static net.osmtracker.util.FileSystemUtils.getUniqueChildNameFor;

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

    // Create before returning if not exists
    public File getBaseExportDirectory(SharedPreferences prefs){
        File rootStorageDirectory = Environment.getExternalStorageDirectory();

        String exportDirectoryNameInPreferences = prefs.getString(
                OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);

        File baseExportDirectory = new File(rootStorageDirectory, exportDirectoryNameInPreferences);
        if(! baseExportDirectory.exists()){
            baseExportDirectory.mkdirs();
        }
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
