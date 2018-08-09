package net.osmtracker.layout;

import android.os.AsyncTask;
import android.util.Log;

import net.osmtracker.util.CustomLayoutsUtils;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

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
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
            HttpsURLConnection l_connection = (HttpsURLConnection) url.openConnection();
            return CustomLayoutsUtils.getStringFromStream(l_connection.getInputStream());

        } catch (Exception e) {
            Log.e(TAG, "Error. Exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

}
