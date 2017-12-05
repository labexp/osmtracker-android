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
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private CheckBox defaultCheckBox;
    SharedPreferences prefs;
    //Container for the file names and the presentation names
    private static Hashtable<String, String> container;
    //File Extension for the layouts in different languages
    private final static String LAYOUT_EXTENSION_ISO = "_xx.xml";

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initializeAttributes();
        LinearLayout rootLayout = (LinearLayout)findViewById(R.id.list_layouts); //root layout for the downloaded xml files
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.buttons_presets); //main layout for the default layout checkbox
        listLayouts(rootLayout);
        checkCurrentLayout(rootLayout, mainLayout);

    }
    private void checkCurrentLayout(LinearLayout rootLayout, LinearLayout mainLayout){
        String activeLayoutName = prefs.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
        boolean defLayout = false;

        //first, we check if the default layout is activated
        View defCheck = mainLayout.getChildAt(1); //the default checkbox in the activity
        if(defCheck instanceof CheckBox){
            CheckBox defCheckCast = (CheckBox) defCheck;
            String defCheckName = container.get(defCheckCast.getText());
            if (activeLayoutName.contains(defCheckName)) { //For ignoring de .xml termination
                selected = defCheckCast;
                defLayout = true;
            }
        }
        //then, if the dafult layout isn't activated, we verify the other layouts
        if(defLayout == false) {
            boolean found = false;
            for (int i = 0; i < rootLayout.getChildCount() && !found; i++) {
                View current = rootLayout.getChildAt(i);
                if (current instanceof CheckBox) {
                    CheckBox currentCast = (CheckBox) current;
                    String currentName = container.get(currentCast.getText());
                    Log.e("#", "lookin for:" + activeLayoutName + "curr: " + currentName);
                    if (activeLayoutName.contains(currentName)) { //For ignoring de .xml termination
                        selected = currentCast;
                        found = true;
                    }
                }
            }
        }
        selected.setChecked(true);
    }
    private void initializeAttributes(){
        setTitle("Buttons Presets");
        setContentView(R.layout.buttons_presets);
        DEFAULT_CHECKBOX_NAME = "default";
        listener = new CheckBoxChangedListener();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        container = new Hashtable<String, String>();
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
                container.put(newName, name);
                c.setText(newName);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(60, 0, 0, 0);
                c.setLayoutParams(lp);
                c.setPadding(10,20,10,20);
                c.setOnClickListener(listener);
                registerForContextMenu(c);
                rootLayout.addView(c, AT_START);
            }
        }

        defaultCheckBox = (CheckBox) findViewById(R.id.def_layout);
        defaultCheckBox.setOnClickListener(listener);
        //this is the maping default->default
        container.put(OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT,OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
        //verify the size of the container, if it is greater than 1, we put invisible the message (in the downloaded layouts section)
        if(container.size() > 1){
            TextView empyText = (TextView) findViewById(R.id.btnpre_empty);
            empyText.setVisibility(View.INVISIBLE);
        }else{
            TextView empyText = (TextView) findViewById(R.id.btnpre_empty);
            empyText.setVisibility(View.VISIBLE);
        }
    }
    public void launch_availables(View v){ //For the button
        startActivity(new Intent(this,AvailableLayouts.class));
    }
    
    //This method converts a xml file name to a simple name for presentation in the Downloaded Layouts section of the Buttons Presets Activity
    public String convertFileName(String fileName){
        String oldName = fileName.substring(0, fileName.length() - LAYOUT_EXTENSION_ISO.length());
        StringBuilder key = new StringBuilder();
        int j = 0;
        for(int i=0; i<oldName.length(); i++){
            //if there is a "_", it is remove from the string
            if(oldName.substring(j, i+1).equals("_")){
                key.append(" ");
            }else{
                key.append(oldName.substring(j, i + 1));
            }
            j++;
        }
        return key.toString();
    }

    //Class that manages the changes on the selected layout
    private class CheckBoxChangedListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            CheckBox pressed = (CheckBox)view;
            selected.setChecked(false);
            pressed.setChecked(true);
            selected=pressed;
            String targetLayout = container.get(pressed.getText());
            prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                    targetLayout).commit();
        }
    }

    //methods for the context menu for each checkbox
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //inflate the menu for the view selected
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.btnprecb_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.cb_update_and_install:
                Toast mensaje = Toast.makeText(getApplicationContext(), "Update and Install options was pressed",
                        Toast.LENGTH_LONG);
                mensaje.show();
                break;
            case R.id.cb_delete:
                Toast mensaje2 = Toast.makeText(getApplicationContext(), "Delete option was pressed",
                        Toast.LENGTH_LONG);
                mensaje2.show();
        }
        return super.onContextItemSelected(item);
    }
}