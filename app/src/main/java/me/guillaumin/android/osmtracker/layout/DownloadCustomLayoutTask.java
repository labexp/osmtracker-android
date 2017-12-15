package me.guillaumin.android.osmtracker.layout;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.activity.Preferences;
import me.guillaumin.android.osmtracker.util.CheckForSDCard;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by aton1698 on 13/12/17.
 */

public class DownloadCustomLayoutTask extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = "Download Custom Layout" ;

    @Override
    protected Boolean doInBackground(String[] layoutNames) {

        //Change with the method "createLayoutUrl"
        String layoutURL="https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico_es.xml";
        String layoutPath = Environment.getExternalStorageDirectory() + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator + Preferences.LAYOUTS_SUBDIR + File.separator;

        String iconsPath = Environment.getExternalStorageDirectory() + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator +
                Preferences.LAYOUTS_SUBDIR+ File.separator  + layoutNames[0] + File.separator;

        Boolean status = false;

        try {
            createDir(layoutPath);
            downloadFile(layoutURL,layoutPath +File.separator + layoutNames[0]+ layoutNames[1] + Preferences.LAYOUT_FILE_EXTENSION);


            createDir(iconsPath);
            HashMap<String, String> iconsInfo;
            iconsInfo = createIconsHash();

            Set<String> keys = iconsInfo.keySet();

            String currentKey;
            String currentValue;

            for(int i=0 ; i<keys.toArray().length ; i++){
                currentKey= (String)keys.toArray()[i];
                currentValue = iconsInfo.get(currentKey);
                downloadFile(currentValue, iconsPath + File.separator + currentKey);
            }

            status = true;

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            status = false;
        }

        return status;
    }
    private void createDir(String dirPath) {
        /**
         TODO: Change this code for the same functionality that it's used when an gpx is exported.
         */

        //Get File if SD card is present
        File apkStorage = null;
        if (new CheckForSDCard().isSDCardPresent()) {
            apkStorage = new File(dirPath);
        }
        //If File is not present create directory
        if (!apkStorage.exists()) {
            apkStorage.mkdirs();
            Log.e(TAG, "Directory Created.");
        }
    }
    private void downloadFile( String downloadUrl, String outputFile) throws Throwable {

        URL url = new URL(downloadUrl); //Create Download URl
        HttpURLConnection c = (HttpURLConnection) url.openConnection(); //Open Url Connection
        InputStream is = c.getInputStream(); //Get InputStream for connection

        if (is != null){
            FileOutputStream fos = new FileOutputStream(outputFile);//Get OutputStream for NewFile Location

            byte[] buffer = new byte[2048]; //Set buffer type
            int len = 0; //init length

            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);//Write new file
            }
            fos.close();
            is.close();
        }
        else{
            throw new IOException("No Contents");
        }
    }

    private HashMap<String,String> createIconsHash(){
        /**
         * Create a HashMap with the name and the download_url for each icon
         * TODO: Update this functionality with the code which Altaros97 is developing
         */

        HashMap<String, String> iconsInfo = new HashMap<String, String>();

        iconsInfo.put("App-05.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/App-05.png");
        iconsInfo.put("App-06.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/App-06.png");
        iconsInfo.put("App-07.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/App-07.png");
        iconsInfo.put("App-08.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/App-08.png");
        iconsInfo.put("alquiler.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/alquiler.png");
        iconsInfo.put("bus_terminal.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/bus_terminal.png");
        iconsInfo.put("ciclovia.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/ciclovia.png");
        iconsInfo.put("estacion_tren.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/estacion_tren.png");
        iconsInfo.put("parqueo_bici.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/parqueo_bici.png");
        iconsInfo.put("taxi.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/taxi.png");
        iconsInfo.put("transporte_publico.png", "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/transporte_publico/transporte_publico.png");

        return iconsInfo;
    }
}