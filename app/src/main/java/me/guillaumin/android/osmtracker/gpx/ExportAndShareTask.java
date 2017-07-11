package me.guillaumin.android.osmtracker.gpx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;

/**
 * Exports the track and media (if any) to a a temporary file, then shares it with another app
 */

public class ExportAndShareTask extends ExportTrackTask {
    private static final String TAG = ExportToTempFileTask.class.getSimpleName();

    /**
     * Maps from name that is passed in URI to another app to a real file in the cache dir.
     */
    static final TreeMap<String, File> sharedFiles = new TreeMap<String, File>();
    /**
     * Name of the file, passed to another app in URI.
     */
    private String filename;
    /**
     * Subfolder in cacheDir, where track is exported
     */
    private File trackDir;

    public ExportAndShareTask(Context context, long trackId) {
        super(context, trackId);
        File cacheDir = context.getCacheDir();
        // Generate name for a temporary directory
        trackDir = new File(cacheDir, "trackDirectory");
        int i = 0;
        while (trackDir.exists())
            trackDir = new File(cacheDir, "trackDirectory" + i++);
        if (trackDir.mkdir())
            Log.d(TAG, "Temporary file: " + trackDir.getAbsolutePath());
        else {
            Log.e(TAG, "Could not create temporary directory");
            throw new IllegalStateException("Could not create temporary directory");
        }
    }

    /**
     * @param startDate
     * @return The directory in which the track file should be created
     * @throws ExportTrackException
     */
    @Override
    protected File getExportDirectory(Date startDate) throws ExportTrackException {
        return trackDir;
    }

    /**
     * Whereas to export the media files or not
     *
     * @return
     */
    @Override
    protected boolean exportMediaFiles() {
        return true;
    }

    @Override
    protected String buildGPXFilename(Cursor c) {
        filename = super.buildGPXFilename(c);
        return filename;
    }

    /**
     * Whereas to update the track export date in the database at the end or not
     *
     * @return
     */
    @Override
    protected boolean updateExportDate() {
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);

        File cacheDir = context.getCacheDir();
        File fileToShare;
        File[] generatedFiles = trackDir.listFiles();
        boolean a;
        if (generatedFiles.length == 1) {
            // if there's only one file in the directory - move file to cache root and share it
            fileToShare = new File(cacheDir, "track-to-share.gpx");
            int i = 0;
            while (fileToShare.exists())
                fileToShare = new File(cacheDir, "track-to-share" + i++ + ".gpx");
            if (generatedFiles[0].renameTo(fileToShare))
                a = trackDir.delete();
            else
                // in some strange situation, when file can't be moved
                fileToShare = generatedFiles[0];
        }
        else if (generatedFiles.length > 1) {
            // if there are several files - put them into zip and share it
            try {
                if (filename.endsWith(".gpx"))
                    filename = filename.substring(0, filename.length() - 4);
                filename = filename + ".zip";
                fileToShare = File.createTempFile("track-to-share", ".zip", cacheDir);
                BufferedInputStream bufferedInputStream;
                byte[] buffer = new byte[4096];
                int count;
                FileOutputStream outputStream = new FileOutputStream(fileToShare);
                ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));
                for (File f : generatedFiles) {
                    ZipEntry entry = new ZipEntry(f.getName());
                    zipOut.putNextEntry(entry);
                    bufferedInputStream = new BufferedInputStream(new FileInputStream(f));
                    while ((count = bufferedInputStream.read(buffer, 0, buffer.length)) > -1)
                        zipOut.write(buffer, 0, count);
                    bufferedInputStream.close();
                    f.delete();
                }
                zipOut.close();
                trackDir.delete();
            }
            catch (IOException ioe) {
                Log.e(TAG, "Can't add entry to zip file", ioe);
                throw new IllegalStateException("Can't add entry to zip file", ioe);
            }
        }
        else {
            Log.e(TAG, "There's no files in track directory");
            throw new IllegalStateException("There's no files in track directory");
        }
        sharedFiles.put(filename, fileToShare);

        Uri tempFileUri = Uri.parse("content://me.guillaumin.android.osmtracker.fileshareprovider/" + filename);
        Intent shareTrack = new Intent(Intent.ACTION_SEND);
        shareTrack.setType("application/xml");
        shareTrack.putExtra(Intent.EXTRA_STREAM, tempFileUri);

        PackageManager packageManager = context.getPackageManager();
        if (packageManager.queryIntentActivities(shareTrack, PackageManager.MATCH_DEFAULT_ONLY).size() > 0)
            context.startActivity(Intent.createChooser(shareTrack, context.getResources().getString(R.string.sharetrack_choose_app)));
        else
        {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setMessage(context.getResources().getString(R.string.error_no_appropriate_activity));
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }
    }
}
