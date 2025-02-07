package net.osmtracker.gpx;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.osmtracker.db.DataHelper;

public class ZipHelper {

    private static final String TAG = "ZipHelper";

    /**
     * Compresses all files associated with a track into a ZIP file.
     *
     * @param context   Application context.
     * @param trackId   Track ID.
     * @param filegpx   Original GPX file.
     * @return The created ZIP file or null if an error occurred.
     */
    public static File zipCacheFiles(Context context, long trackId, File filegpx) {
        File directory = DataHelper.getTrackDirectory(trackId,context);
        if (!directory.exists() || !directory.isDirectory()) {
            return filegpx;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            Log.e(TAG, "There are no files to compress in: " + directory.getAbsolutePath());
            return null;
        }
        String name = filegpx.getName();

        File zipFile = new File(context.getCacheDir(), name.substring(0, name.length() - 3)+"zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                if (!file.isDirectory()) { // Avoid adding empty folders
                    // only add files that are not .zip files
                    if (!file.getName().endsWith(".zip")) {
                        addFileToZip(file, zos);
                    }else {
                        Log.d(TAG, "No file is added: " + file.getAbsolutePath());
                    }
                }
            }
            // Adds the original gpx file
            addFileToZip(filegpx, zos);

            Log.d(TAG, "ZIP file created: " + zipFile.getAbsolutePath());
            return zipFile;

        } catch (IOException e) {
            Log.e(TAG, "Error creating ZIP file", e);
            return null;
        }
    }


    /**
     * Adds a file to the ZIP archive.
     *
     * @param file The file to add.
     * @param zos  The ZipOutputStream to which the file will be added.
     */
    private static void addFileToZip(File file, ZipOutputStream zos) throws IOException {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }
}
