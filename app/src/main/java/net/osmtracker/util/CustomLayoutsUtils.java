package net.osmtracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.AvailableLayouts;
import net.osmtracker.activity.Preferences;
import net.osmtracker.layout.GetStringResponseTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Created by adma9717 on 12/8/17.
 */

public class CustomLayoutsUtils {
    //File Extension for the layouts in different languages
    public final static String LAYOUT_EXTENSION_ISO = "_xx.xml";

    /**
     * @param fileName is the name of a .xml that you want to convert to presentation name
     * @return a String presentation name, if the type isn't valid returns the same fileName parameter
     */
    public static String convertFileName(String fileName) {
        //Remove de file extension
        String subname = fileName.replace(Preferences.LAYOUT_FILE_EXTENSION,"");

        //Check if it has iso:
        if(subname.matches("\\w+_..")){
            //Remove "_es"
            subname = subname.substring(0,subname.length() - (AvailableLayouts.ISO_CHARACTER_LENGTH+1));
        }

        //Replace "_" to " "
        return subname.replace("_"," ");
    }

    /**
     * @param representation is the layout name shown in the UI
     * @return Layout filename.
     */
    public static String unconvertFileName(String representation){
        return representation.replace(" ","_") + Preferences.LAYOUT_FILE_EXTENSION;
    }

    /**
     * Creates a layoutFileName for installing or updating the layout
     *
     * @param layoutName String shown in the UI (human readable).
     * @param iso String language code of the layout (ISO 639-1)
     * @return layout file name as String.
     */
    public static String createFileName(String layoutName, String iso) {
        String fileName = layoutName.replace(" ", "_");
        fileName = fileName + LAYOUT_EXTENSION_ISO.replace("xx", iso);
        return fileName;
    }

    /**
     * FIXME: Create a util class with this method. This method is a copy&paste of the one in {@link GetStringResponseTask}
     * Converts an InputStream to a String using the UTF-8 charset.
     *
     * @param stream the InputStream to read from
     * @return a String containing all characters read from the stream
     * @throws IOException if an I/O error occurs
     */
    public static String getStringFromStream(InputStream stream) throws IOException {
        if (stream != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[2048];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
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

    /**
     * Obtain the current inflated layout name and returns it.
     *
     * @param context current activity where this method is invoke
     * @return the inflated layout name like a string = name_layout_xx.xml
     */
    public static String getCurrentLayoutName(Context context){
        String layoutName = "";
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            layoutName = sharedPreferences.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return layoutName;
    }
}
