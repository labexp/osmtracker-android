package net.osmtracker.layout;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.util.CustomLayoutsUtils;
import net.osmtracker.util.URLCreator;

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
        return downloadLayout(layoutName,iso);
    }


    public boolean downloadLayout(String layoutName, String iso){
        String layoutFolderName = layoutName.replace(" ", "_");
        // No usage of SharedPreferences why?
        String storageDir = File.separator + OSMTracker.Preferences.VAL_STORAGE_DIR;
        Log.d(TAG,"storage directory: " + storageDir);

        String layoutURL = URLCreator.createLayoutFileURL(context, layoutFolderName, iso);
        String layoutPath = context.getExternalFilesDir(null) + storageDir + File.separator +
                OSMTracker.LAYOUTS_SUBDIR + File.separator;

        String iconsPath = context.getExternalFilesDir(null)  + storageDir + File.separator +
                OSMTracker.LAYOUTS_SUBDIR + File.separator  + layoutFolderName + OSMTracker.ICONS_DIR_SUFFIX +
                File.separator;

        Boolean status = false;

        try {
            // download layout
            createDir(layoutPath);
            downloadFile(layoutURL,layoutPath +File.separator +
                    CustomLayoutsUtils.createFileName(layoutName, iso));
            status = true;

            // downloading icons
            HashMap<String, String> iconsInfo = getIconsHash(layoutName);
            if (iconsInfo != null) {
                createDir(iconsPath);
                Set<String> keys = iconsInfo.keySet();
                String currentKey;
                String currentValue;
                for(int i=0 ; i<keys.toArray().length ; i++){
                    currentKey= (String)keys.toArray()[i];
                    currentValue = iconsInfo.get(currentKey);
                    downloadFile(currentValue, iconsPath + File.separator + currentKey);
                }
            }
        } catch (Throwable throwable) {
            Log.e(TAG, throwable.toString());
            status = false;
        }
        return status;
    }


    //TODO: reuse export create dir functionality
    private void createDir(String dirPath) throws IOException {
        // Checks if a volume containing external storage is available for read and write.
        if (! Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
            throw new IOException(context.getResources().getString(R.string.error_externalstorage_not_writable));
        }

        File directory = new File(dirPath);
         //If File is not present create directory
        if (! directory.exists() ) {
            boolean ok = directory.mkdirs();
            if (! ok) {
                throw new IOException(context.getResources().getString(R.string.error_externalstorage_not_writable));
            }
        }
        Log.d(TAG, "Directory Created: " + directory.toString());
    }

    private boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private void downloadFile( String downloadUrl, String outputFile) throws Throwable {

        URL url = new URL(downloadUrl); //Create Download URl
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Open Url Connection
        InputStream is = connection.getInputStream(); //Get InputStream for connection

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
        String layoutFolderName = layoutName.replace(" ", "_");

        String link = URLCreator.createIconsDirUrl(context, layoutFolderName);
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
            Log.e(TAG, e.toString());
            return null;
        }

        return iconsHash;

    }



}