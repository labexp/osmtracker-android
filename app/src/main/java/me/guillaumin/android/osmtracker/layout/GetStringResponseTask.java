package me.guillaumin.android.osmtracker.layout;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;

/**
 * Created by james on 07/12/17.
 */

public class GetStringResponseTask extends AsyncTask<String, Integer, String> {
    private static final String TAG = "List Custom Layouts";

    @Override
    protected String doInBackground(String... params) {
        // https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/
        try {
            URL url = new URL(params[0]); //get the url passed as parameter must be built.

            // Open Url Connection
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream stream = httpConnection.getInputStream();
            return getStringFromStream(stream);

        } catch (Exception e) {
            Log.e(TAG, "Error. Exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * @param stream
     * @return all the characters in the stream as a single String
     * @throws IOException
     */
    private String getStringFromStream(InputStream stream) throws IOException {
        if (stream != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[2048];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                int counter;
                while ((counter = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, counter);
                }
            }finally {
                stream.close();
            }
            return writer.toString();
        } else {
            throw new  IOException();
        }
    }
}
