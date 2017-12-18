package me.guillaumin.android.osmtracker.layout;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;

/**
 * Created by james on 07/12/17.
 */

public class GetStringResponseTask extends AsyncTask<String, Integer, String> {
    private static final String TAG = "GetStringResponseTask";

    /**
     *
     * @param params params[0] must be the URL from which the String will be retrieved.
     * @return
     */
    @Override
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(params[0]);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream stream = httpConnection.getInputStream();
            return CustomLayoutsUtils.getStringFromStream(stream);

        } catch (Exception e) {
            Log.e(TAG, "Error. Exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

}
