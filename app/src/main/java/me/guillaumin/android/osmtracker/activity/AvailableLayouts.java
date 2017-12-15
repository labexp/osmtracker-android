package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.GetStringResponseTask;
import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;

/**
 * Created by emmanuel on 10/11/17.
 */

public class AvailableLayouts extends Activity {
    private DownloadListener downloadListener = new DownloadListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String tag = "(Connecting...)"; //while it makes the request
        setTitle("Available Layouts "+tag);
        setContentView(R.layout.available_layouts);

        // call task to download and parse the response to get the list of
        // available layouts

        String url = "https://api.github.com/repos/LabExperimental-SIUA/osmtracker-android/contents/layouts/metadata?ref=layouts";
        final InputStream res;
        new GetStringResponseTask(){
            protected void onPostExecute(String response){
                setAvailableLayouts(parseResponse(response));
                setTitle((""+getTitle()).replace(tag,"")); //when the request is done
            }

        }.execute(url);
    }

    


    public void setAvailableLayouts(List<String> options) {
        LinearLayout ly = (LinearLayout)findViewById(R.id.root_layout);
        int AT_START = 0; //the position to insert the view at
        ClickListener listener = new ClickListener();
        for(String option : options){
            TextView c = new TextView(this);
            c.setHeight(200);
            c.setText(CustomLayoutsUtils.convertFileName(option, false));
            c.setTextSize((float)30);
            c.setOnClickListener(listener);
            ly.addView(c,AT_START);
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
            while(!(eventType==XmlPullParser.START_TAG && parser.getName().length()==2)){
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
    *
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

    private void showDescriptionDialog(String title, String description){
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(title);
        b.setNegativeButton("Cancel",null);
        b.setPositiveButton("Download", this.downloadListener);
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
                showDescriptionDialog(layoutName,desc);
            }
        });
        b.create().show();
    }

    public void launch_server_settings(View v){
        startActivity(new Intent(this, ServerSettings.class));
    }

    private class ClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            final String text = ""+((TextView) view).getText();
            final String fileName = CustomLayoutsUtils.unconvertFileName(text);

            String url = "https://raw.githubusercontent.com/LabExperimental-SIUA/osmtracker-android/layouts/layouts/metadata/";
            new GetStringResponseTask(){
                @Override
                protected void onPostExecute(String response) {
                    String xmlFile = response;
                    String localLang = "";//Locale.getDefault().getLanguage();
                    String description = getDescriptionFor(xmlFile, localLang);
                    if (description != null) {
                        showDescriptionDialog(fileName,description);
                    } else {//List all other languages
                        HashMap<String, String> languages = getLanguagesFor(xmlFile);
                        showLanguageSelectionDialog(languages, xmlFile, text);

                    }
                }
            }.execute(url+fileName);
        }
    }

    private class DownloadListener implements AlertDialog.OnClickListener{
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //Code for downloading the layout, must get the layout name here
        }
    }
}
