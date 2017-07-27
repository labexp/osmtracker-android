package me.guillaumin.android.osmtracker.gpx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.exception.ExportTrackException;

/**
 * Export the track and media (if any) to a a temporary file, share it with another app, remove the
 * temporary file.
 *
 * @author NoktaStrigo
 *
 */

public class ExportAndShareTask extends ExportTrackTask {
    private static final String TAG = ExportAndShareTask.class.getSimpleName();

    enum FileTypes { ZIP, GPX }

    /**
     * Stores File object that points to saved shared file and it's type (zip or gpx)
     */
    class SharedFile{
        FileTypes fileType;
        public File file;
        SharedFile(FileTypes type, File f) {
            fileType = type;
            file = f;
        }
    }
    /**
     * Maps from name that is passed in URI to another app ('virtual' name) to a real file in the cache dir.
     */
    static final Map<String, SharedFile> sharedFiles = new TreeMap<String, SharedFile>();
    /**
     * Maps from requestCode to 'virtual' filename, passed in Uri with that request
     */
    static private final Map<Integer, String> sharedUris = new TreeMap<Integer, String>();
    /**
     * Name of the file, passed to another app in URI ('virtual' name).
     */
    private static File cacheDir;
    private String virtualFilename;
    /**
     * Subfolder in cacheDir, where track is exported
     */
    private File trackDir;

    public ExportAndShareTask(Context context, long trackId) {
        super(context, trackId);
        cacheDir = context.getCacheDir();
        String cacheSubDirName = "trackDirectory";
        // Generate name for a temporary directory
        trackDir = new File(cacheDir, cacheSubDirName);
        int i = 0;
        while (trackDir.exists()) {
            trackDir = new File(cacheDir, cacheSubDirName + i++);
        }
        if (trackDir.mkdir()) {
            Log.d(TAG, "Temporary directory: " + trackDir.getAbsolutePath());
        }
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
        virtualFilename = super.buildGPXFilename(c);
        return virtualFilename;
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

        FileTypes sharedFileType;
        File cacheDir = context.getCacheDir();
        File fileToShare;
        File[] generatedFiles = trackDir.listFiles();
        if (generatedFiles.length == 1) {
            // if there's only one file in the directory - just share it
            sharedFileType = FileTypes.GPX;
            fileToShare = generatedFiles[0];
        }
        else if (generatedFiles.length > 1) {
            // if there are several files - put them into zip and share it
            try {
                sharedFileType = FileTypes.ZIP;
                if (virtualFilename.endsWith(DataHelper.EXTENSION_GPX))
                    virtualFilename = virtualFilename.substring(0, virtualFilename.length() - 4);
                virtualFilename = virtualFilename + DataHelper.EXTENSION_ZIP;
                fileToShare = File.createTempFile("track-to-share", DataHelper.EXTENSION_ZIP, cacheDir);
                BufferedInputStream bufferedInputStream;
                byte[] buffer = new byte[4096];
                int count;
                FileOutputStream outputStream = new FileOutputStream(fileToShare);
                ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));
                for (File f : generatedFiles) {
                    ZipEntry entry = new ZipEntry(f.getName());
                    zipOut.putNextEntry(entry);
                    bufferedInputStream = new BufferedInputStream(new FileInputStream(f));
                    while ((count = bufferedInputStream.read(buffer, 0, buffer.length)) > -1) {
                        zipOut.write(buffer, 0, count);
                    }
                    bufferedInputStream.close();
                    f.delete();
                }
                zipOut.close();
                trackDir.delete();
            }
            catch (IOException ioe) {
                Log.e(TAG, "Can't add entry to zip file", ioe);
                for (File f : trackDir.listFiles()) {
                    f.delete();
                }
                trackDir.delete();
                throw new IllegalStateException("Can't add entry to zip file", ioe);
            }
        }
        else {
            Log.e(TAG, "There's no files in track directory");
            trackDir.delete();
            throw new IllegalStateException("There's no files in track directory");
        }
        sharedFiles.put(virtualFilename, new SharedFile(sharedFileType, fileToShare));

        Uri tempFileUri = Uri.parse("content://me.guillaumin.android.osmtracker.fileshareprovider/" + virtualFilename);
        Intent shareTrack = new Intent(Intent.ACTION_SEND);
        shareTrack.setType((sharedFileType == FileTypes.ZIP) ? "application/zip" : "application/xml");
        shareTrack.putExtra(Intent.EXTRA_STREAM, tempFileUri);

        PackageManager packageManager = context.getPackageManager();
        if (packageManager.queryIntentActivities(shareTrack, PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {
            int requestCode = 0;
            while (sharedUris.containsKey(requestCode)) {
                requestCode++;
            }
            sharedUris.put(requestCode, virtualFilename);
            ((Activity) context).startActivityForResult(Intent.createChooser(shareTrack, context.getResources().getString(R.string.sharetrack_choose_app)), requestCode);
        }
        else
        {
            deleteSharedFile(virtualFilename);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            String extension = (sharedFileType == FileTypes.ZIP) ? "ZIP" : "GPX";
            dialogBuilder.setMessage(String.format(context.getResources().getString(R.string.error_no_appropriate_activity), extension));
            dialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {}
            });
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }
    }

    /**
     * @param requestCode code of completed request (the file shared with that request can be deleted)
     */
    static public void deleteSharedFile(int requestCode) {
        String virtualFilenameToDelete = sharedUris.get(requestCode);
        if (virtualFilenameToDelete != null) {
            if (deleteSharedFile(virtualFilenameToDelete)) {
                sharedUris.remove(requestCode);
            }
        }
    }

    /**
     * @param filenameToDelete 'virtual' name of file to delete
     * @return false if the file was found in sharedFiles, but wasn't deleted, true otherwise
     */
    static private boolean deleteSharedFile(String filenameToDelete) {
        File tempFile = sharedFiles.get(filenameToDelete).file;
        if (tempFile != null) {
            if (tempFile.delete()) {
                if (tempFile.getParentFile().compareTo(cacheDir) != 0) {
                    //if shared file was saved in cacheDir subfolder (happens when sharing .gpx file)
                    tempFile.getParentFile().delete();
                }
            }
            if (!tempFile.exists()) {
                //if the file is deleted we don't need info about it
                sharedFiles.remove(filenameToDelete);
            }
            else {
                return false;
            }
        }
        return true;
    }
}
