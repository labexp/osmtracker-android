package net.osmtracker.layout;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import net.osmtracker.util.URLCreator;

import javax.net.ssl.HttpsURLConnection;
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
        String githubUsername = params[0];
        String repoName = params[1];
        String branchName = params[2];
        return customLayoutsRepoValidator(githubUsername, repoName, branchName);
    }

    protected boolean customLayoutsRepoValidator(String githubUsername, String repoName, String branchName){
        String server_url = URLCreator.createTestURL(githubUsername, repoName, branchName);
        boolean status;
        try {
            URL url = new URL(server_url);
            // Open Url Connection
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
            HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();

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
