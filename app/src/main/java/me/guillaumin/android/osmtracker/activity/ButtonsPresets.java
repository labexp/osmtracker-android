package me.guillaumin.android.osmtracker.activity;

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
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;

/**
 * Created by emmanuel on 20/10/17.
 */

public class ButtonsPresets extends PreferenceActivity {

    CheckBoxChangedListener listener = new CheckBoxChangedListener();
    String DEFAULT_CHECKBOX_KEY = "default";
    public ArrayList<String> checkBoxNames = new ArrayList<String>();
    SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.e(">","bien");
        addPreferencesFromResource(R.xml.buttons_presets);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String storageDir = "/osmtracker";
        File layoutsDir = new File(Environment.getExternalStorageDirectory(), storageDir + File.separator + Preferences.LAYOUTS_SUBDIR + File.separator);
        if (layoutsDir.exists() && layoutsDir.canRead()) {
            String[] layoutFiles = layoutsDir.list(new FilenameFilter() {
                                                        @Override
                                                        public boolean accept(File dir, String filename) {
                                                            return filename.endsWith(".xml");//Preferences.LAYOUT_FILE_EXTENSION);
                                                        }
                                                    });
            PreferenceCategory seccion = (PreferenceCategory)findPreference("lista");
            for(String name : layoutFiles){
                checkBoxNames.add(name);
            }
            for(String name : checkBoxNames){
                CheckBoxPreference c = new CheckBoxPreference(this);
                c.setTitle(name.substring(0,name.indexOf(".")));
                c.setKey(name);
                c.setOnPreferenceChangeListener(listener);
                c.setChecked(false);
                seccion.addPreference(c);
            }
            checkBoxNames.add(DEFAULT_CHECKBOX_KEY);
            ((CheckBoxPreference)findPreference(DEFAULT_CHECKBOX_KEY)).setOnPreferenceChangeListener(listener);

        }
    }

    //Class that manages the changes on the selected layout
    private class CheckBoxChangedListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if(preference.getKey().equals(DEFAULT_CHECKBOX_KEY)){
//                notify("cancelado");
                changeNotDefaultCheckBoxesState( ! (Boolean)newValue );
            }
            else{
                for(String name : checkBoxNames){
                    CheckBoxPreference currentCheckBox = (CheckBoxPreference) findPreference(name);
                    currentCheckBox.setChecked( currentCheckBox.getKey().equals(preference.getKey()) );
                }
            }
//            prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,""+preference.getTitle());
//                    OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT;
            return true;
        }

        //For disabling the downloaded layouts when the default one is selected
        private void changeNotDefaultCheckBoxesState(boolean newState){
            for(String name : checkBoxNames) {
                if(! name.equals(DEFAULT_CHECKBOX_KEY)) {
                    CheckBoxPreference currentCheckBox = (CheckBoxPreference) findPreference(name);
                    currentCheckBox.setEnabled(newState);
                    currentCheckBox.setChecked(false);
                }
            }
        }

        //Utility function for showing messages through a Toast
        void notify(String message){
            Toast toast = Toast.makeText(getApplicationContext(), message , Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}