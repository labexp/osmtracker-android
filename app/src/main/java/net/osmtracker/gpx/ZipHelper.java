package net.osmtracker.gpx;

import android.content.Context;
import android.util.Log;

import java.io.*;
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
            Log.e(TAG, "No hay archivos para comprimir en: " + directory.getAbsolutePath());
            return null;
        }
        String name = filegpx.getName();

        File zipFile = new File(context.getCacheDir(), name.substring(0, name.length() - 3)+"zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                if (!file.isDirectory()) { // Evita agregar carpetas vacías
                    // solo se agrega archivos que no sean .zip
                    if (!file.getName().endsWith(".zip")) {
                        addFileToZip(file, zos);
                    }else {
                        Log.d(TAG, "No se agrega archivo: " + file.getAbsolutePath());
                    }
                }
            }
            // Agrega el archivo gpx original
            addFileToZip(filegpx, zos);

            Log.d(TAG, "Archivo ZIP creado: " + zipFile.getAbsolutePath());
            return zipFile;

        } catch (IOException e) {
            Log.e(TAG, "Error al crear el archivo ZIP", e);
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
            Log.e(TAG, "Archivo no encontrado: " + file.getAbsolutePath());
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
