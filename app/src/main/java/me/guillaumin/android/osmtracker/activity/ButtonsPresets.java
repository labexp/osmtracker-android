package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Hashtable;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;

/**
 * Created by emmanuel on 20/10/17.
 */

public class ButtonsPresets extends Activity {

    CheckBoxChangedListener listener;
    String DEFAULT_CHECKBOX_NAME;
    public CheckBox selected;
    SharedPreferences prefs;

    /**
     * Container for the file names and the presentation names
     */
    public static Hashtable<String, String> container = new Hashtable<String, String>();

    /**
     * File Extension for the layouts in different llanguages
     */
    public final static String LAYOUT_EXTENSION_ISO = "_xx.xml";

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initializeAttributes();
        LinearLayout rootLayout = (LinearLayout)findViewById(R.id.buttons_presets);
        listLayouts(rootLayout);
        checkCurrentLayout(rootLayout);

    }
    private void checkCurrentLayout(LinearLayout rootLayout){
        String activeLayoutName = prefs.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
        for(int i=0 ; i<rootLayout.getChildCount() ; i++){
            View current = rootLayout.getChildAt(i);
            if(current instanceof CheckBox) {
                CheckBox currentCast = (CheckBox) current;
                if(activeLayoutName.contains(""+currentCast.getText())){ //For ignoring de .xml termination
                    currentCast.setChecked(true);
                    selected = currentCast;
                }

            }
        }
    }
    private void initializeAttributes(){
        setTitle("Buttons Presets");
        setContentView(R.layout.buttons_presets);
        DEFAULT_CHECKBOX_NAME = "default";
        listener = new CheckBoxChangedListener();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void listLayouts(LinearLayout rootLayout){
        String storageDir = "/osmtracker";
        File layoutsDir = new File(Environment.getExternalStorageDirectory(), storageDir + File.separator + Preferences.LAYOUTS_SUBDIR + File.separator);
        int AT_START = 0; //the position to insert the view at
        int fontSize = 20;
        if (layoutsDir.exists() && layoutsDir.canRead()) {
            String[] layoutFiles = layoutsDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".xml");//Preferences.LAYOUT_FILE_EXTENSION);
                }
            });
            for(String name : layoutFiles) {
                CheckBox c = new CheckBox(this);
                c.setTextSize((float) fontSize);
                String newName = convertFileName(name);
                c.setText(newName);
                c.setOnClickListener(listener);
                rootLayout.addView(c, AT_START);
            }
        }
        CheckBox def = new CheckBox(this);
        def.setTextSize((float)fontSize);
        def.setText(DEFAULT_CHECKBOX_NAME);
        def.setOnClickListener(listener);
        rootLayout.addView(def,AT_START);

    }
    public void launch_availables(View v){ //For the button
        startActivity(new Intent(this,AvailableLayouts.class));
    }
    
    /**
     * This method converts a xml file name to a simple name for presentation in the Downloaded Layouts
     * section of the Buttons Presets Activity
     */
    public String convertFileName(String fileName){

        String oldName = fileName.substring(0, fileName.length() - LAYOUT_EXTENSION_ISO.length());

        String key = "";
        int j = 0;
        for(int i=0; i<oldName.length(); i++){
            //if there is a "_", it is remove from the string
            if(oldName.substring(j, i+1).equals("_")){
                key += " ";
            }else{
                key += oldName.substring(j, i+1);
            }
            j++;
        }

        container.put(key, fileName);

        return key;
    }

    //Class that manages the changes on the selected layout
    private class CheckBoxChangedListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            CheckBox pressed = (CheckBox)view;
            selected.setChecked(false);
            pressed.setChecked(true);
            selected=pressed;
            String targetLayout = "";
            if(selected.getText().equals(DEFAULT_CHECKBOX_NAME)){
                targetLayout = DEFAULT_CHECKBOX_NAME;
            }else {
                targetLayout = selected.getText() + ".xml";
            }
//            Log.e("#","Layout changed to "+targetLayout);
            prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                    targetLayout).commit();
        }
    }
}