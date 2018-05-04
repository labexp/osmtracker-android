package org.osmtracker.layout;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import org.osmtracker.util.URLCreator;

import static android.content.ContentValues.TAG;

/**
 * Created by labexp on 13/12/17.
 */

public class URLValidatorTask extends AsyncTask<String, Integer, Boolean>{
    @Override
    protected Boolean doInBackground(String... params) {
        /*
         * params[0] = Github Username
         * params[1] = Repository Name
         * params[2] = Branch Name
         */
        String server_url = URLCreator.createTestURL(params[0], params[1], params[2]);
        boolean status;
        try {
            URL url = new URL(server_url);
            // Open Url Connection
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            //If Connection response is OK then change the status to true
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "Server returned HTTP " + httpConnection.getResponseCode()
                        + " " + httpConnection.getResponseMessage());
                status = true;
            } else{
                Log.e(TAG, "The connection could not be established, server return: " + httpConnection.getResponseCode());
                status = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error. Exception: " + e.toString());
            e.printStackTrace();
            status = false;
        }
        return status;
    }
}
