package net.osmtracker.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.layout.DownloadCustomLayoutTask;
import net.osmtracker.util.CustomLayoutsUtils;
import net.osmtracker.util.FileSystemUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Hashtable;

/**
 * Created by emmanuel on 20/10/17.
 */

public class ButtonsPresets extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = Preferences.class.getSimpleName();

    final private int RC_WRITE_PERMISSION = 1;

    private CheckBox checkboxHeld;
    private CheckBoxChangedListener listener;
    private CheckBox selected;
    private CheckBox defaultCheckBox;
    private SharedPreferences prefs;
    //Container for the file names and the presentation names
    private static Hashtable<String, String> layoutsFileNames;
    private static String storageDir;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initializeAttributes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ( writeExternalStoragePermissionGranted() ) {
            refreshActivity();
        } else {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // TODO: explain why we need permission.
                Log.w(TAG, "we should explain why we need read permission");

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_PERMISSION);
            }

        }
    }

    public void refreshActivity(){
        LinearLayout downloadedLayouts = (LinearLayout) findViewById(R.id.list_layouts);
        //main layout for the default layout checkbox
        LinearLayout defaultSection = (LinearLayout) findViewById(R.id.buttons_presets);
        //restart the hashtable
        layoutsFileNames = new Hashtable<String, String>();
        listLayouts(downloadedLayouts);
        checkCurrentLayout(downloadedLayouts, defaultSection);
    }

    private void initializeAttributes(){
        setTitle(getResources().getString(R.string.prefs_ui_buttons_layout));
        setContentView(R.layout.buttons_presets);
        listener = new CheckBoxChangedListener();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        layoutsFileNames = new Hashtable<String, String>();
        storageDir = File.separator + OSMTracker.Preferences.VAL_STORAGE_DIR;
    }

    private void listLayouts(LinearLayout rootLayout){
        File layoutsDir = new File(this.getExternalFilesDir(null), storageDir +
                File.separator + Preferences.LAYOUTS_SUBDIR + File.separator);
        int AT_START = 0; //the position to insert the view at
        int fontSize = 20;
        if (layoutsDir.exists() && layoutsDir.canRead()) {
            //Ask for the layout's filenames
            String[] layoutFiles = layoutsDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(Preferences.LAYOUT_FILE_EXTENSION);
                }
            });
            //Remove all the layouts
            while(rootLayout.getChildAt(0) instanceof CheckBox){
                rootLayout.removeViewAt(0);
            }
            //Fill with the new ones
            for(String name : layoutFiles) {
                CheckBox newCheckBox = new CheckBox(this);
                newCheckBox.setTextSize((float) fontSize);
                String newName = CustomLayoutsUtils.convertFileName(name);
                layoutsFileNames.put(newName, name);
                newCheckBox.setText(newName);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(60, 0, 0, 0);
                newCheckBox.setLayoutParams(layoutParams);
                newCheckBox.setPadding(10,20,10,20);
                newCheckBox.setOnClickListener(listener);
                registerForContextMenu(newCheckBox);
                rootLayout.addView(newCheckBox, AT_START);
            }
        }

        defaultCheckBox = (CheckBox) findViewById(R.id.def_layout);
        defaultCheckBox.setOnClickListener(listener);
        //this is the maping default(It depends on the language of the mobile)->default
        layoutsFileNames.put(defaultCheckBox.getText().toString(),OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
        //verify the size of the layoutsFileNames, if it is greater than 1, we put invisible the message (in the downloaded layouts section)
        if(layoutsFileNames.size() > 1){
            TextView empyText = (TextView) findViewById(R.id.btnpre_empty);
            empyText.setVisibility(View.INVISIBLE);
        }else{
            TextView empyText = (TextView) findViewById(R.id.btnpre_empty);
            empyText.setVisibility(View.VISIBLE);
        }
    }


    /**
     * @param downloadedLayouts: this linear layout contains the downloaded custom layouts representation
     * @param defaultSection: it contains the default layout representation
     * It asks for the layout being used and checks it in the list
     */
    private void checkCurrentLayout(LinearLayout downloadedLayouts, LinearLayout defaultSection){
        String activeLayoutName = CustomLayoutsUtils.getCurrentLayoutName(getApplicationContext());
        boolean defLayout = false;

        //first, we check if the default layout is activated
        View defCheck = defaultSection.getChildAt(1); //the default checkbox in the activity
        if(defCheck instanceof CheckBox){
            CheckBox defCheckCast = (CheckBox) defCheck;
            String defCheckName = layoutsFileNames.get(defCheckCast.getText());
            if (activeLayoutName.equals(defCheckName)) {
                selected = defCheckCast;
                defLayout = true;
            }
        }

        boolean found = false;
        //then, if the default layout isn't activated, we verify the other layouts
        if (!defLayout) {
            for (int i = 0; i < downloadedLayouts.getChildCount(); i++) {
                View current = downloadedLayouts.getChildAt(i);
                if (current instanceof CheckBox) {
                    CheckBox currentCast = (CheckBox) current;
                    String currentName = layoutsFileNames.get(currentCast.getText());
                    if (activeLayoutName.equals(currentName)) {
                        selected = currentCast;
                        found = true;
                        break;
                    }
                }
            }
            //if not found the active layout then set the default
            if(!found){
                selected = (CheckBox) defCheck;
                String targetLayout = layoutsFileNames.get(selected.getText());
                prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                        targetLayout).commit();
                //reload the activity
                refreshActivity();
            }
        }

        selected.setChecked(true);
    }

    private void selectLayout(CheckBox pressed){
        selected.setChecked(false);
        pressed.setChecked(true);
        selected=pressed;
        String targetLayout = layoutsFileNames.get(pressed.getText());
        prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                targetLayout).commit();
    }

    //Class that manages the changes on the selected layout
    private class CheckBoxChangedListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            selectLayout( (CheckBox)view );
        }
    }

    //methods for the context menu for each checkbox
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //inflate the menu for the view selected
        getMenuInflater().inflate(R.menu.btnprecb_context_menu, menu);
        checkboxHeld = (CheckBox) v;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        File externalFilesDir = this.getExternalFilesDir(null);
        switch (item.getItemId()){
            //this case download again the layout held and install it
            case R.id.cb_update_and_install:
                String layoutName = checkboxHeld.getText().toString();
                String iso = getIso(layoutsFileNames.get(checkboxHeld.getText()));
                String info[]= {layoutName, iso};
                final ProgressDialog dialog = new ProgressDialog(checkboxHeld.getContext());
                dialog.setMessage(getResources().getString(R.string.buttons_presets_updating_layout));
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.show();
                new DownloadCustomLayoutTask(this){
                    protected void onPostExecute(Boolean status){
                        //if the download is correct we activate it
                        if (status) {
                            selectLayout(checkboxHeld);
                            //re-load the activity
                            refreshActivity();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.buttons_presets_successful_update), Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.buttons_presets_unsuccessful_update), Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                }.execute(info);
                checkboxHeld.setChecked(false);
                break;
            //this case open a new confirm dialog to delete a layout, also, if the layout have a icon directory, it is deleted
            case R.id.cb_delete:
                new AlertDialog.Builder(this).
                setTitle(checkboxHeld.getText())
                .setMessage(getResources().getString(R.string.buttons_presets_delete_message).replace("{0}", checkboxHeld.getText()))
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getResources().getString(R.string.buttons_presets_delete_positive_confirmation), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = layoutsFileNames.get(checkboxHeld.getText());
                        String rootDir = storageDir + File.separator + Preferences.LAYOUTS_SUBDIR + File.separator;
                        File fileToDelete = new File(externalFilesDir, rootDir + fileName);
                        String iconDirName = fileName.substring(0, fileName.length() - CustomLayoutsUtils.LAYOUT_EXTENSION_ISO.length())
                                + Preferences.ICONS_DIR_SUFFIX;
                        File iconDirToDelete = new File(externalFilesDir, rootDir + iconDirName);

                        boolean successfulDeletion = FileSystemUtils.delete(fileToDelete, false);

                        if(iconDirToDelete.exists())
                            successfulDeletion &= FileSystemUtils.delete(iconDirToDelete, true);

                        int messageToShowId = (successfulDeletion) ? R.string.buttons_presets_successful_delete :
                                R.string.buttons_presets_unsuccessful_delete;
                        String message = getResources().getString(messageToShowId);

                        Log.println(successfulDeletion ? Log.INFO : Log.ERROR, "TOAST", message);

                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                        //reload the activity
                        refreshActivity();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.menu_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create().show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * This method obtain the iso of any layout file name
     * Assumes that layoutName looks like a filename => name_xx.ext
     * Example: given "foo_es.xml" return only "es"
     */
    private String getIso(String layoutName){
        String tmp = layoutName.substring(0, layoutName.length() - Preferences.LAYOUT_FILE_EXTENSION.length());
        String iso = "";
        for (int i=tmp.length() - AvailableLayouts.ISO_CHARACTER_LENGTH; i<tmp.length(); i++){
                iso += tmp.charAt(i);
        }
        return iso;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.launch_available_layouts_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.launch_available){
            startActivity(new Intent(this,AvailableLayouts.class));
        }
        return super.onOptionsItemSelected(item);
    }


    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RC_WRITE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    refreshActivity();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //TODO: add an informative message.
                    Log.w(TAG, "we should explain why we need read permission");
                }
            }
        }
    }

    //TODO: improve permissions management.
    private boolean writeExternalStoragePermissionGranted(){
        // On versions lower than Android 11, write external storage permission is required.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.d(TAG, "CHECKING - Write");
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            Log.d(TAG, "Write External Storage is granted");
            return true;
        }
    }

}