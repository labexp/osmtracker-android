package me.guillaumin.android.osmtracker.gpx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TreeMap;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;

/**
 * Exports to a a temporary file, then shares intent with uri
 */

public class ExportAndShareTask extends ExportTrackTask {
    private static final String TAG = ExportToTempFileTask.class.getSimpleName();

    static final TreeMap<String, File> sharedFiles = new TreeMap<String, File>();
    final File tmpFile;
    String filename;

    public ExportAndShareTask(Context context, long trackId) {
        super(context, trackId);
        try {
            tmpFile = File.createTempFile("shared-track", ".gpx", context.getCacheDir());
            Log.d(TAG, "Temporary file: " + tmpFile.getAbsolutePath());
        } catch (IOException ioe) {
            Log.e(TAG, "Could not create temporary file", ioe);
            throw new IllegalStateException("Could not create temporary file", ioe);
        }
    }

    /**
     * @param startDate
     * @return The directory in which the track file should be created
     * @throws ExportTrackException
     */
    @Override
    protected File getExportDirectory(Date startDate) throws ExportTrackException {
        return tmpFile.getParentFile();
    }

    /**
     * Whereas to export the media files or not
     *
     * @return
     */
    @Override
    protected boolean exportMediaFiles() {
        return false;
    }

    @Override
    protected String buildGPXFilename(Cursor c) {
        filename = super.buildGPXFilename(c);
        return tmpFile.getName();
    }

    /**
     * Whereas to update the track export date in the database at the end or not
     *
     * @return
     */
    @Override
    protected boolean updateExportDate() {
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        sharedFiles.put(filename, tmpFile);
        Uri tempFileUri = Uri.parse("content://me.guillaumin.android.osmtracker.fileshareprovider/" + filename);
        Intent shareTrack = new Intent(Intent.ACTION_SEND);
        shareTrack.setType("application/xml");
        shareTrack.putExtra(Intent.EXTRA_STREAM, tempFileUri);

        PackageManager packageManager = context.getPackageManager();
        if (packageManager.queryIntentActivities(shareTrack, PackageManager.MATCH_DEFAULT_ONLY).size() > 0)
            context.startActivity(shareTrack);
        else
        {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setMessage(context.getResources().getString(R.string.error_no_appropriate_activity));
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }
    }
}
