package me.guillaumin.android.osmtracker.layout;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.activity.Preferences;
import me.guillaumin.android.osmtracker.util.CheckForSDCard;
import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;
import me.guillaumin.android.osmtracker.util.URLCreator;

/**
 * Created by aton1698 on 13/12/17.
 */

public class DownloadCustomLayoutTask extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = "Download Custom Layout" ;

    private Context context;

    public DownloadCustomLayoutTask(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String[] layoutData) {

        String layoutName = layoutData[0];
        String iso = layoutData[1];

        String layoutURL = URLCreator.createLayoutFileURL(context, layoutName, iso);
        String layoutPath = Environment.getExternalStorageDirectory() + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator +
                Preferences.LAYOUTS_SUBDIR + File.separator;

        String iconsPath = Environment.getExternalStorageDirectory() + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator +
                Preferences.LAYOUTS_SUBDIR + File.separator  + layoutName + File.separator;

        Boolean status = false;

        try {
            // download layout
            createDir(layoutPath);
            downloadFile(layoutURL,layoutPath +File.separator + CustomLayoutsUtils.createFileName(layoutName, iso));

            // TODO: download metadata file

            // downloading icons
            createDir(iconsPath);
            HashMap<String, String> iconsInfo = getIconsHash(layoutName);

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
         FIXME: Change this code for the same functionality that it's used when an gpx is exported.
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

    /**
     *
     * @param layoutName
     * @return
     */
    private HashMap<String,String> getIconsHash(String layoutName) {

        final HashMap<String,String> iconsHash = new HashMap<String, String>();

        String link = URLCreator.createIconsDirUrl(context, layoutName);
        System.out.println("Download icons hash from: " + link);

        try {
            URL url = new URL(link);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream stream = httpConnection.getInputStream();
            String response = CustomLayoutsUtils.getStringFromStream(stream);
            JSONArray jsonArray = new JSONArray(response);

            for (int i= 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                iconsHash.put(object.getString("name"), object.getString("download_url"));
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return iconsHash;

    }



}