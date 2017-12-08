package me.guillaumin.android.osmtracker.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import me.guillaumin.android.osmtracker.activity.ButtonsPresets;
import me.guillaumin.android.osmtracker.activity.Preferences;

/**
 * Created by adma9717 on 12/8/17.
 */

public class CustomLayoutsUtils {
    //File Extension for the layouts in different languages
    public final static String LAYOUT_EXTENSION_ISO = "_xx.xml";
    /**
     * @param fileName is the name of a .xml that you want to convert to presentation name
     * @param iso 0 = meta 'file' name, 1 = layout 'file' name
     * @return a String presentation name, if the type isn't valid returns the same fileName parameter
     */
    public static String convertFileName(String fileName, boolean iso) {
        String subName = "";
        //0 = metadata file name
        if (iso) {
            subName = fileName.substring(0, fileName.length() - LAYOUT_EXTENSION_ISO.length());
        }
        else{
            String tmpName = fileName.substring(0, fileName.length() - Preferences.LAYOUT_FILE_EXTENSION.length());
            subName = tmpName.replace("meta_", "");
        }
        return subName.replace("_", " ");
    }

    /**
     * @param representation is the layout name shown in the UI
     * @return the filename of the layout. Which is the input with the "meta_", "_" and ".xml"
     */
    public static String unconvertFileName(String representation){
        return "meta_"+representation.replace(" ","_")+".xml";
    }

}
