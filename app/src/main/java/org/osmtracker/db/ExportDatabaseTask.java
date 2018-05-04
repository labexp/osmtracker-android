package org.osmtracker.db;

import android.os.AsyncTask;
import android.os.Bundle;

import org.osmtracker.activity.About;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Task to export the internal database to the external storage,
 * for debugging purposes
 */
public class ExportDatabaseTask extends AsyncTask<File, Float, String>{

    private static final int BUF_SIZE = 16 * 1024;

    private static final String DB_FILE_EXT = ".sqlitedb.gz";

    /** Activity to show the progress dialog */
    private final About activity;

    /** Target folder where to save the export */
    private final File targetFolder;

    /**
     * Export the database to a target folder
     * @param activity Activity to display progress dialog
     * @param targetFolder Folder where to save the database file
     */
    public ExportDatabaseTask(About activity, File targetFolder) {
        this.activity = activity;
        this.targetFolder = targetFolder;
    }

    @Override
    protected String doInBackground(File... files) {
        if (files.length > 1) {
            throw new IllegalArgumentException("More than 1 file is not supported");
        }

        File targetFile = new File(targetFolder, DatabaseHelper.DB_NAME + DB_FILE_EXT);
        targetFile.getParentFile().mkdirs();

        long fileSize = files[0].length();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(files[0]);
            os = new GZIPOutputStream(new FileOutputStream(targetFile));
            byte[] buffer = new byte[BUF_SIZE];
            long copied = 0;
            int count;

            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
                copied += count;
                publishProgress((float) fileSize / copied);
            }

            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            return e.getLocalizedMessage();
        } finally {
            if (is != null) {
                try { is.close(); }
                catch (IOException ioe) { }
            }
            if (os != null) {
                try { os.close(); }
                catch (IOException ioe) { }
            }
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        if (activity.getExportDbProgressDialog() != null) {
            activity.getExportDbProgressDialog().setProgress(Math.round(values[0] * 100));
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Bundle b = new Bundle();
        b.putString("result", result);
        activity.removeDialog(About.DIALOG_EXPORT_DB);
        activity.showDialog(About.DIALOG_EXPORT_DB_COMPLETED, b);
    }
}
