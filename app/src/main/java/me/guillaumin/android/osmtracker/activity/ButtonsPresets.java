package me.guillaumin.android.osmtracker.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Hashtable;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.DownloadCustomLayoutTask;
import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;
import me.guillaumin.android.osmtracker.util.FileSystemUtils;

/**
 * Created by emmanuel on 20/10/17.
 */

public class ButtonsPresets extends Activity {

    private CheckBox selectedCheckBox;
    private CheckBoxChangedListener listener;
    public CheckBox selected;
    private CheckBox defaultCheckBox;
    private SharedPreferences prefs;
    //Container for the file names and the presentation names
    private static Hashtable<String, String> container;
    private static String storageDir;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initializeAttributes();
        LinearLayout rootLayout = (LinearLayout) findViewById(R.id.list_layouts);
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.buttons_presets); //main layout for the default layout checkbox
        listLayouts(rootLayout);
        checkCurrentLayout(rootLayout, mainLayout);

    }

    private void initializeAttributes(){
        setTitle("Buttons Presets");
        setContentView(R.layout.buttons_presets);
        listener = new CheckBoxChangedListener();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        container = new Hashtable<String, String>();
        storageDir = File.separator + prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR,
                                                 OSMTracker.Preferences.VAL_STORAGE_DIR);
    }

    private void listLayouts(LinearLayout rootLayout){
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
                String newName = CustomLayoutsUtils.convertFileName(name, true);
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

        boolean found = false;
        //then, if the dafult layout isn't activated, we verify the other layouts
        if (!defLayout) {
            for (int i = 0; i < rootLayout.getChildCount(); i++) {
                View current = rootLayout.getChildAt(i);
                if (current instanceof CheckBox) {
                    CheckBox currentCast = (CheckBox) current;
                    String currentName = container.get(currentCast.getText());
                    Log.e("#", "lookin for:" + activeLayoutName + "curr: " + currentName);
                    if (activeLayoutName.contains(currentName)) { //For ignoring de .xml termination
                        selected = currentCast;
                        found = true;
                        break;
                    }//end if (activeLayoutName.contains(currentName))
                }//end if (current instanceof CheckBox)
            }//end for

            //if not found the active layout then set the default
            if(!found){
                selected = (CheckBox) defCheck;
                String targetLayout = container.get(selected.getText());
                prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                        targetLayout).commit();
                //reload the activity
                finish();
                startActivity(getIntent());
            }//end if(found == false)
        }//end if (defLayout == false)

        selected.setChecked(true);
    }

    public void launch_availables(View v){ //For the button
        startActivity(new Intent(this,AvailableLayouts.class));
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
        selectedCheckBox = (CheckBox) v;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.cb_update_and_install:
                String name = selectedCheckBox.getText().toString().replace(" ", "_");
                Log.i("#", name + ": " + container.get(selectedCheckBox.getText()));
                String iso = getIso(container.get(selectedCheckBox.getText()));
                String info[]= {name, iso};
                new DownloadCustomLayoutTask(){
                    protected void onPostExecute(Boolean status){
                        if (status) {
                            Log.i("Download Custom Layout", "Ok");
                            String targetLayout = container.get(selectedCheckBox.getText());
                            prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                                    targetLayout).commit();
                            finish();
                            startActivity(getIntent());
                            Toast.makeText(getApplicationContext(), "Layout was updated successfully", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Log.e("Download Custom Layout", "Download error");
                            Toast.makeText(getApplicationContext(), "Layout was not updated, try again later.", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute(info);
                break;

            case R.id.cb_delete:
                new AlertDialog.Builder(this).
                setTitle(selectedCheckBox.getText())
                .setMessage("Are you sure to delete the " + selectedCheckBox.getText() + " layout?")
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = container.get(selectedCheckBox.getText());
                        String rootDir = storageDir + File.separator + Preferences.LAYOUTS_SUBDIR + File.separator;
                        File fileToDelete = new File(Environment.getExternalStorageDirectory(), rootDir + fileName);

                        if(FileSystemUtils.delete(fileToDelete, false)){
                            Toast.makeText(getApplicationContext(), "The file was deleted successfully", Toast.LENGTH_SHORT).show();

                            String iconDirName = fileName.substring(0, fileName.length() - CustomLayoutsUtils.LAYOUT_EXTENSION_ISO.length());
                            File iconDirToDelete = new File(Environment.getExternalStorageDirectory(), rootDir + iconDirName);

                            if(FileSystemUtils.delete(iconDirToDelete, true)){
                                Toast.makeText(getApplicationContext(), "The icon directory was deleted successfully", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(), "This file didn't have any icon directory associated", Toast.LENGTH_SHORT).show();
                            }

                        }else{
                            Toast.makeText(getApplicationContext(), "The file could not be delete", Toast.LENGTH_SHORT).show();
                        }
                        //reload the activity
                        finish();
                        startActivity(getIntent());
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create().show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    //this method obtain the iso of any layout file name
    private String getIso(String layoutName){
        String tmp = layoutName.substring(0, layoutName.length() - Preferences.LAYOUT_FILE_EXTENSION.length());
        String iso = "";
        for (int i=0; i<tmp.length(); i++){
            if(i >= tmp.length() - 3){
                iso += tmp.charAt(i);
                Log.i("#", "Looking into iso variable: " + iso);
            }
        }
        return iso;
    }
}