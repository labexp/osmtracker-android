package me.guillaumin.android.osmtracker.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.DownloadCustomLayoutTask;
import me.guillaumin.android.osmtracker.layout.GetStringResponseTask;
import me.guillaumin.android.osmtracker.layout.URLValidatorTask;
import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;
import me.guillaumin.android.osmtracker.util.URLCreator;

/**
 * Created by emmanuel on 10/11/17.
 */

public class AvailableLayouts extends Activity {

    //this variable indicates if the default github configuration is activated
    private boolean isDefChecked;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    //options for repository settings
    private EditText github_username;
    private EditText repository_name;
    private EditText branch_name;
    private CheckBox defaultServerCheckBox;
    private CheckBox customServerCheckBox;

    public static final int ISO_CHARACTER_LENGTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPrefs.edit();
        // call task to download and parse the response to get the list of available layouts
        if (isNetworkAvailable(this)) {
            retrieveAvailableLayouts();
        } else {
            Toast.makeText(getApplicationContext(),"Error: unable to connect to the Internet.",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void retrieveAvailableLayouts(){
        final String waitingMessage = "(Connecting...)"; //while it makes the request
        setTitle("Available Layouts "+waitingMessage);
        String url = URLCreator.createMetadataDirUrl(this);
        new GetStringResponseTask() {
            protected void onPostExecute(String response) {
                setContentView(R.layout.available_layouts);
                setAvailableLayouts(parseResponse(response));
                setTitle(("" + getTitle()).replace(waitingMessage, "")); //when the request is done
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
        for(String option : options){
            TextView layoutTextView = new TextView(this);
            layoutTextView.setHeight(200);
            layoutTextView.setText(CustomLayoutsUtils.convertFileName(option, false));
            layoutTextView.setTextSize((float)30);
            layoutTextView.setOnClickListener(listener);
            rootLayout.addView(layoutTextView,AT_START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.srvsttg_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //this override method creates the github repository settings windows, and upload the values in the shared preferences file if those changed
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.srvsttg_config){
            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            //this is for prevent any error with the inflater
            assert inflater != null;
            //This is the pop up that's appears when the config button in the top right corner is pressed
            @SuppressLint("InflateParams") final View repositoryConfigWindow = inflater.inflate(R.layout.github_repository_settings, null);
            //instancing the edit texts of the layoutName inflate
            github_username = (EditText) repositoryConfigWindow.findViewById(R.id.github_username);
            repository_name = (EditText) repositoryConfigWindow.findViewById(R.id.repository_name);
            branch_name = (EditText) repositoryConfigWindow.findViewById(R.id.branch_name);
            //instancing the checkbox option and setting the click listener
            defaultServerCheckBox = (CheckBox) repositoryConfigWindow.findViewById(R.id.default_server);
            customServerCheckBox = (CheckBox) repositoryConfigWindow.findViewById(R.id.custom_server);

            //first, we verify if the default checkbox is activated, if true we put the default options into the edit texts and make them not editable
            if(sharedPrefs.getBoolean("defCheck", true)){
                toggleRepositoryOptions(true);
            }
            //if the default checkbox isn't checked we put the shared preferences values into the edit texts
            else{
                toggleRepositoryOptions(false);
            }

            defaultServerCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                    toggleRepositoryOptions(false);
                    isDefChecked = false;
                    //we save the status into the sharedPreferences file
                    editor.putBoolean("defCheck", isDefChecked);
                    editor.commit();
                }
            });
            //creating the alert dialog with the github_repository_setting view
            new AlertDialog.Builder(this)
                    .setTitle("Github Repository Settings")
                    .setView(repositoryConfigWindow)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String[] repositoryCustomOptions = {github_username.getText().toString(), repository_name.getText().toString(), branch_name.getText().toString()};
                            //we verify if the entered options are correct
                            new URLValidatorTask(){
                                protected void onPostExecute(Boolean result){
                                    //validating the github repository
                                    if(result){
                                        Toast.makeText(AvailableLayouts.this, "The server is valid", Toast.LENGTH_SHORT).show();
                                        //save the entered options into the shared preferences file
                                        editor.putString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, repositoryCustomOptions[0]);
                                        editor.putString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, repositoryCustomOptions[1]);
                                        editor.putString(OSMTracker.Preferences.KEY_BRANCH_NAME, repositoryCustomOptions[2]);
                                        editor.commit();
                                        retrieveAvailableLayouts();
                                    }else{
                                        Toast.makeText(AvailableLayouts.this, "Invalid server", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }.execute(repositoryCustomOptions);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
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
        github_username.setEnabled(!status);
        branch_name.setEnabled(!status);
        repository_name.setEnabled(!status);

        //setting the default options into text fields
        if(status){
            github_username.setText(OSMTracker.Preferences.VAL_GITHUB_USERNAME);
            repository_name.setText(OSMTracker.Preferences.VAL_REPOSITORY_NAME);
            branch_name.setText(OSMTracker.Preferences.VAL_BRANCH_NAME);
        }
        //setting the custom options into text fields
        else{
            github_username.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, ""));
            repository_name.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME,""));
            branch_name.setText(sharedPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME, ""));
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
            //step to lang start tag
            parser.next();
            //step to first iso start tag
            while(!(eventType==XmlPullParser.START_TAG && parser.getName().length()== ISO_CHARACTER_LENGTH)){
                eventType = parser.next();
            }
            while(eventType != XmlPullParser.END_DOCUMENT){
                //We are at the start tag of a iso
                //Then look for the name tag inside it...
                String iso = parser.getName();
                String name=""; //The key,value pairs for the hashmap
                while(!(eventType == XmlPullParser.START_TAG && parser.getName().equals("name"))){
                    eventType = parser.next();
                }
                parser.next();//step to the content of the <name> tag
                name = parser.getText();
                //Skip to the next language iso start tag
                while(!(eventType == XmlPullParser.END_TAG && parser.getName().equals(iso))){
                    eventType = parser.next();
                }
                languages.put(name,iso);
                eventType = parser.next(); //step to the next iso tag
                parser.next();
                if(parser.getName().length()!=2) {//check it's a iso tag
                    eventType =  XmlPullParser.END_DOCUMENT; //We're done here
                }
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
            boolean descriptionFound = false;
            while(eventType != XmlPullParser.END_DOCUMENT && ! descriptionFound ){
                if(eventType == XmlPullParser.START_TAG && parser.getName().equals(localeLanguage)){
                    //We are at the start of the <es>, <en> tag, must look for the <desc> tag
                    while(!(eventType == XmlPullParser.START_TAG && parser.getName().equals("desc"))){
                        eventType = parser.next();
                    }
                    //We are at start of desc tag must get Text
                    eventType = parser.next();//Step from the start of the tag to its content
                    description = parser.getText();
                    descriptionFound = true;
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
        b.setNegativeButton("Cancel",null);
        b.setPositiveButton("Download", new DownloadListener(layoutName, iso, this));
        b.setMessage(description);
        b.create().show();
    }

    private void showLanguageSelectionDialog(final HashMap<String,String> languages, final String xmlFile, final String layoutName){
        Set<String> keys = languages.keySet();
        final CharSequence options[] = new CharSequence[keys.toArray().length];
        for(int i=0 ; i<keys.toArray().length ; i++){
            options[i] = (String)keys.toArray()[i];
        }
        Toast.makeText(this,"Your language is not available select one from the list",
                        Toast.LENGTH_LONG).show();
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Available Languages");
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
            new GetStringResponseTask(){
                @Override
                protected void onPostExecute(String response) {
                    String xmlFile = response;
                    String localLang = Locale.getDefault().getLanguage();
                    String description = getDescriptionFor(xmlFile, localLang);
                    if (description != null) {
                        showDescriptionDialog(layoutName,description,localLang);
                    } else {//List all other languages
                        HashMap<String, String> languages = getLanguagesFor(xmlFile);
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
            Toast.makeText(this.context,"Trying to download "+this.layoutName +" "+this.iso,
                    Toast.LENGTH_LONG).show();

            String info[] = {this.layoutName, this.iso};
            Log.e("#","Result "+info[0]+","+info[1]);

            new DownloadCustomLayoutTask(this.context){
                protected void onPostExecute(Boolean status){
                    String message="";
                    if (status) {
                        message = "Ok";
                    }
                    else {
                        message = "Download error";
                    }
                    Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
                }

            }.execute(info);
        }
    }
}
