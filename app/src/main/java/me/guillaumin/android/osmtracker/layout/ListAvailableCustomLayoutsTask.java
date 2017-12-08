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

/**
 * Created by james on 07/12/17.
 */

public class ListAvailableCustomLayoutsTask extends AsyncTask<Void, Integer, List<String>> {
    private static final String TAG = "List Custom Layouts";

    @Override
    protected List<String> doInBackground(Void... voids) {
        // https://crunchify.com/in-java-how-to-read-github-file-contents-using-httpurlconnection-convert-stream-to-string-utility/

        //TODO: get default server_URL (without metadata folder in it)
        String server_url = "https://api.github.com/repos/LabExperimental-SIUA/osmtracker-android/contents/layouts/metadata?ref=layouts";

        try {
            //URL url = new URL(urlString[0]);
            URL url = new URL(server_url);
            // Open Url Connection
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream stream = httpConnection.getInputStream();
            String response = getStringFromStream(stream);

            Log.i(TAG, response);

            List<String> options = parseResponse(response);
            return options;

        } catch (Exception e) {
            Log.e(TAG, "Error. Exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    // ConvertStreamToString() Utility
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
            } finally {
                stream.close();
            }
            return writer.toString();
        } else {
            // return "No Contents";
            throw new  IOException();
        }
    }


    private List<String> parseResponse(String response) {
        /*
        parse the string (representation of a json) to get only the values associated with
        key "name", which are the file names of the folder requested before.
         */

        List<String> options = new ArrayList<String>();


        try {
            // create JSON Object
            JSONArray jsonArray = new JSONArray(response);

            for (int i= 0; i < jsonArray.length(); i++) {
                // create json object for every element of the array
                JSONObject object = jsonArray.getJSONObject(i);
                // get the value associated with
                options.add( object.getString("name") );
            }


        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return options;
    }


}
