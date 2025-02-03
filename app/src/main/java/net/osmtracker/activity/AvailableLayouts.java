package net.osmtracker.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.layout.DownloadCustomLayoutTask;
import net.osmtracker.layout.GetStringResponseTask;
import net.osmtracker.layout.URLValidatorTask;
import net.osmtracker.util.CustomLayoutsUtils;
import net.osmtracker.util.URLCreator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by emmanuel on 10/11/17.
 */

public class AvailableLayouts extends Activity {

    private final static String TMP_SHARED_PREFERENCES_FILE = "net.osmtracker.tmpspfile";

    //this variable indicates if the default github configuration is activated
    private boolean isDefChecked;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    //options for repository settings
    private EditText etxGithubUsername;
    private EditText etxRepositoryName;
    private EditText etxBranchName;
    private CheckBox defaultServerCheckBox;
    private CheckBox customServerCheckBox;

    private boolean checkBoxPressed;

    public static final int ISO_CHARACTER_LENGTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPrefs.edit();
        setTitle(getResources().getString(R.string.prefs_ui_available_layout));
        // call task to download and parse the response to get the list of available layouts
        if (isNetworkAvailable(this)) {
            validateDefaultOptions();
        } else {
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.available_layouts_connection_error),Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void validateDefaultOptions(){
        String usernameGitHub = sharedPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, OSMTracker.Preferences.VAL_GITHUB_USERNAME);
        String repositoryName = sharedPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, OSMTracker.Preferences.VAL_REPOSITORY_NAME);
        String branchName = sharedPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME, OSMTracker.Preferences.VAL_BRANCH_NAME);
        final String[] repositoryDefaultOptions = {usernameGitHub, repositoryName, branchName};
        //we verify if the entered options are correct
        new URLValidatorTask(){
            protected void onPostExecute(Boolean result){
                //validating the github repository
                if(result){
                    retrieveAvailableLayouts();
                }else{
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.available_layouts_response_null_exception),Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }.execute(repositoryDefaultOptions);
    }

    @SuppressLint("StaticFieldLeak")
    public void retrieveAvailableLayouts(){
        //while it makes the request
        final String waitingMessage = getResources().getString(R.string.available_layouts_connecting_message);
        setTitle(getResources().getString(R.string.prefs_ui_available_layout) + waitingMessage);
        String url = URLCreator.createMetadataDirUrl(this);
        new GetStringResponseTask() {
            protected void onPostExecute(String response) {
                if(response == null){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.available_layouts_response_null_exception),Toast.LENGTH_LONG).show();
                    finish();
                }
                else{
                    setContentView(R.layout.available_layouts);
                    setAvailableLayouts(parseResponse(response));
                    //when the request is done
                    setTitle(getResources().getString(R.string.prefs_ui_available_layout));
                }
            }

        }.execute(url);
    }

    /**
     * It's used for asking there is internet before doing any other networking
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    /**
     * It receives a string list with the names of the layouts to be listed in the activity
     */
    public void setAvailableLayouts(List<String> options) {
        LinearLayout rootLayout = (LinearLayout)findViewById(R.id.root_layout);
        int AT_START = 0; //the position to insert the view at
        ClickListener listener = new ClickListener();
        Log.e("#",options.toString());
        for(String option : options) {
            Button layoutButton = new Button(this);
            layoutButton.setHeight(150);
            layoutButton.setText(CustomLayoutsUtils.convertFileName(option));
            layoutButton.setTextSize(16f);
            layoutButton.setTextColor(Color.WHITE);
            layoutButton.setSingleLine(false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(50, 10, 50, 10);
            layoutButton.setLayoutParams(layoutParams);
            layoutButton.setPadding(40, 30, 40, 30);
            layoutButton.setOnClickListener(listener);
            rootLayout.addView(layoutButton,AT_START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.github_repository_settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //this override method creates the github repository settings windows, and upload the values in the shared preferences file if those changed
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if(item.getItemId() == R.id.github_config){
            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            //this is for prevent any error with the inflater
            assert inflater != null;
            //This is the pop up that's appears when the config button in the top right corner is pressed
            @SuppressLint("InflateParams") final View repositoryConfigWindow = inflater.inflate(R.layout.github_repository_settings, null);
            //instancing the edit texts of the layoutName inflate
            etxGithubUsername = (EditText) repositoryConfigWindow.findViewById(R.id.github_username);
            etxRepositoryName = (EditText) repositoryConfigWindow.findViewById(R.id.repository_name);
            etxBranchName = (EditText) repositoryConfigWindow.findViewById(R.id.branch_name);
            //instancing the checkbox option and setting the click listener
            defaultServerCheckBox = (CheckBox) repositoryConfigWindow.findViewById(R.id.default_server);
            customServerCheckBox = (CheckBox) repositoryConfigWindow.findViewById(R.id.custom_server);

            //internal private shared preferences to manage the incorrect server requested by the user
            final SharedPreferences tmpSharedPref = getApplicationContext().getSharedPreferences(TMP_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
            //flag to manage if the user put an invalid server
            boolean isCallBack = tmpSharedPref.getBoolean("isCallBack", false);

            //if the user put an invalid GitHub server, the user can edit the values again until the server is valid
            if(!isCallBack){
                //first, we verify if the default checkbox is activated, if true we put the default options into the edit texts and make them not editable
                if(sharedPrefs.getBoolean("defCheck", true)){
                    toggleRepositoryOptions(true);
                }
                //if the default checkbox isn't checked we put the shared preferences values into the edit texts
                else{
                    toggleRepositoryOptions(false);
                }
            }
            else{
                toggleRepositoryOptions(false);
                etxGithubUsername.setText(tmpSharedPref.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, ""));
                etxRepositoryName.setText(tmpSharedPref.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, ""));
                etxBranchName.setText(tmpSharedPref.getString(OSMTracker.Preferences.KEY_BRANCH_NAME, ""));
            }

            checkBoxPressed = false;

            defaultServerCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBoxPressed = true;
                    toggleRepositoryOptions(true);
                    isDefChecked = true;
                    //we save the status into the sharedPreferences file
                    editor.putBoolean("defCheck", isDefChecked);
                    editor.commit();
                }
            });
            customServerCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBoxPressed = true;
                    toggleRepositoryOptions(false);
                    isDefChecked = false;
                    //we save the status into the sharedPreferences file
                    editor.putBoolean("defCheck", isDefChecked);
                    editor.commit();
                }
            });
            //creating the alert dialog with the github_repository_setting view
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.prefs_ui_github_repository_settings))
                    .setView(repositoryConfigWindow)
                    .setPositiveButton(getResources().getString(R.string.menu_save), new DialogInterface.OnClickListener() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String[] repositoryCustomOptions = {etxGithubUsername.getText().toString(), etxRepositoryName.getText().toString(), etxBranchName.getText().toString()};
                            //we verify if the entered options are correct
                            new URLValidatorTask(){
                                protected void onPostExecute(Boolean result){
                                    //validating the github repository
                                    if(result){
                                        String message = getResources().getString(R.string.github_repository_settings_valid_server);
                                        Log.i("TOAST", message);
                                        Toast.makeText(AvailableLayouts.this, message, Toast.LENGTH_SHORT).show();
                                        //save the entered options into the shared preferences file
                                        editor.putString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, repositoryCustomOptions[0]);
                                        editor.putString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, repositoryCustomOptions[1]);
                                        editor.putString(OSMTracker.Preferences.KEY_BRANCH_NAME, repositoryCustomOptions[2]);
                                        editor.commit();
                                        //to avoid the request of invalid server at the beginning
                                        tmpSharedPref.edit().putBoolean("isCallBack", false).commit();
                                        retrieveAvailableLayouts();
                                    }else{
                                        String message = getResources().getString(R.string.github_repository_settings_invalid_server);
                                        Log.e("TOAST", message);
                                        Toast.makeText(AvailableLayouts.this, message, Toast.LENGTH_SHORT).show();
                                        tmpSharedPref.edit().putString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, repositoryCustomOptions[0]).commit();
                                        tmpSharedPref.edit().putString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, repositoryCustomOptions[1]).commit();
                                        tmpSharedPref.edit().putString(OSMTracker.Preferences.KEY_BRANCH_NAME, repositoryCustomOptions[2]).commit();
                                        //to make a request at the beginning of pop-up
                                        tmpSharedPref.edit().putBoolean("isCallBack", true).commit();
                                        onOptionsItemSelected(item);
                                    }
                                }
                            }.execute(repositoryCustomOptions);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.menu_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            tmpSharedPref.edit().putBoolean("isCallBack", false).commit();
                            if (checkBoxPressed){
                                if(!isDefChecked){
                                    toggleRepositoryOptions(true);
                                    isDefChecked = true;
                                    //save the status into the sharedPreferences file
                                    editor.putBoolean("defCheck", isDefChecked);
                                    editor.commit();
                                }
                                else{
                                    toggleRepositoryOptions(false);
                                    isDefChecked = false;
                                    //save the status into the sharedPreferences file
                                    editor.putBoolean("defCheck", isDefChecked);
                                    editor.commit();
                                }
                            }
                            dialog.cancel();
                        }
                    })
                    .setCancelable(true)
                    .create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    * This toggles (default/custom) the states of repository settings options in function of boolean param
    * status true: tries to activated default options
    * status false: tries to activated custom options
    * */
    private void toggleRepositoryOptions(boolean status){
        customServerCheckBox.setChecked(!status);
        customServerCheckBox.setEnabled(status);
        defaultServerCheckBox.setChecked(status);
        defaultServerCheckBox.setEnabled(!status);
        etxGithubUsername.setEnabled(!status);
        etxBranchName.setEnabled(!status);
        etxRepositoryName.setEnabled(!status);

        //setting the default options into text fields
        if(status){
            etxGithubUsername.setText(OSMTracker.Preferences.VAL_GITHUB_USERNAME);
            etxRepositoryName.setText(OSMTracker.Preferences.VAL_REPOSITORY_NAME);
            etxBranchName.setText(OSMTracker.Preferences.VAL_BRANCH_NAME);
        }
        //setting the custom options into text fields
        else{
            etxGithubUsername.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, ""));
            etxRepositoryName.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, OSMTracker.Preferences.VAL_REPOSITORY_NAME));
            etxBranchName.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME, OSMTracker.Preferences.VAL_BRANCH_NAME));
        }
    }

    /*
    parse the string (representation of a json) to get only the values associated with
    key "name", which are the file names of the folder requested before.
    */
    private List<String> parseResponse(String response) {
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

    /**
     * @param xmlFile is the meta xmlFile put in a String
     * @return a HashMap like (LanguageName,IsoCode) Example: English -> en.
     */
    private HashMap<String,String> getLanguagesFor(String xmlFile){
        HashMap<String,String> languages = new HashMap<String,String>();
        try{
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput (new ByteArrayInputStream(xmlFile.getBytes()),"UTF-8");
            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                //Move to a <option> tag
                if(parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("option")){
                    String name = parser.getAttributeValue(null,"name");
                    String iso = parser.getAttributeValue(null,"iso");
                    languages.put(name,iso);
                }
                eventType = parser.next();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return languages;
    }

    /* xmlFile is the XML meta file parsed to string
    *  localeLanguage is the ISO code of the phone's locale language
    * Searches a description in the locale language and returns it if it is in xmlFile
    * or null if it is not there
    */
    private String getDescriptionFor(String xmlFile, String localeLanguage){
        String description = null;
        try{
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput (new ByteArrayInputStream(xmlFile.getBytes()),"UTF-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT
                    && description == null ){
                if(eventType == XmlPullParser.START_TAG
                        && parser.getName().equals("option")){
                    //We are in an option start tag
                    //Ask for the option's iso
                    String iso = parser.getAttributeValue("","iso");
                    if(iso != null && iso.equals(localeLanguage)){
                        //If the start tag has "iso" attribute and matches the locale language
                        //Move to the content to the tag
                        parser.next();
                        //Save its content
                        description = parser.getText();
                    }
                }
                eventType = parser.next();
            }

        }catch(Exception e){
            Log.e("#","Error parsing metadata files: "+e.toString());
        }
        return description;
    }

    private void showDescriptionDialog(String layoutName, String description, String iso){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(layoutName);
        b.setNegativeButton(getResources().getString(R.string.menu_cancel),null);
        b.setPositiveButton(getResources().getString(R.string.available_layouts_description_dialog_positive_confirmation), new DownloadListener(layoutName, iso, this));
        b.setMessage(description);
        b.create().show();
    }

    private void showLanguageSelectionDialog(final HashMap<String,String> languages, final String xmlFile, final String layoutName){
        Set<String> keys = languages.keySet();
        final CharSequence options[] = new CharSequence[keys.toArray().length];
        for(int i=0 ; i<keys.toArray().length ; i++){
            options[i] = (String)keys.toArray()[i];
        }
        Toast.makeText(this,getResources().getString(R.string.available_layouts_not_available_language),
                        Toast.LENGTH_LONG).show();
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getResources().getString(R.string.available_layouts_language_dialog_title));
        b.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String desc = getDescriptionFor(xmlFile,languages.get(options[i]));
                showDescriptionDialog(layoutName,desc,languages.get(options[i]));
            }
        });
        b.create().show();
    }

    private class ClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            final String layoutName = ""+((TextView) view).getText();
            String url = URLCreator.createMetadataFileURL(view.getContext(), layoutName);
            final ProgressDialog dialog = new ProgressDialog(view.getContext());
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage(getResources().getString(R.string.available_layouts_checking_language_dialog));
            dialog.show();
            new GetStringResponseTask(){
                @Override
                protected void onPostExecute(String response) {
                    dialog.dismiss();
                    String xmlFile = response;
                    String localLang = Locale.getDefault().getLanguage();
                    String description = getDescriptionFor(xmlFile, localLang);
                    if (description != null) {
                        showDescriptionDialog(layoutName,description,localLang);
                    } else {//List all other languages
                        HashMap<String, String> languages = getLanguagesFor(xmlFile);
                        Log.e("#",languages.toString());
                        showLanguageSelectionDialog(languages, xmlFile, layoutName);
                    }
                }
            }.execute(url);
        }
    }

    private class DownloadListener implements AlertDialog.OnClickListener{
        private String layoutName;
        private String iso;
        private Context context;

        public DownloadListener(String layoutName, String iso, Context context) {
            this.layoutName = layoutName;
            this.iso = iso;
            this.context = context;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //Code for downloading the layoutName, must get the layoutName name here
            String info[] = {this.layoutName, this.iso};
            final ProgressDialog dialog = new ProgressDialog(this.context);
            dialog.setMessage(getResources().getString(R.string.available_layouts_downloading_dialog));
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
            new DownloadCustomLayoutTask(this.context){
                protected void onPostExecute(Boolean status){
                    String message="";
                    if (status) {
                        message = getResources().getString(R.string.available_layouts_successful_download);
                        Log.i("TOAST", message);
                    }
                    else {
                        message = getResources().getString(R.string.available_layouts_unsuccessful_download);
                        Log.e("TOAST", message);
                    }
                    Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }.execute(info);
        }
    }
}
