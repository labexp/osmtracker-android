package me.guillaumin.android.osmtracker.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.List;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.ListAvailableCustomLayoutsTask;
import me.guillaumin.android.osmtracker.util.CustomLayoutsUtils;

/**
 * Created by emmanuel on 10/11/17.
 */

public class AvailableLayouts extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Available Layouts");
        setContentView(R.layout.available_layouts);

        // call task to downloand and parse the response to get the list of
        // available layouts
        new ListAvailableCustomLayoutsTask(){
            protected void onPostExecute(List<String> options){
                setAvailableLayouts(options);
            }

        }.execute();


    }


    public void setAvailableLayouts(List<String> options) {
        LinearLayout ly = (LinearLayout)findViewById(R.id.root_layout);
        int AT_START = 0; //the position to insert the view at
        for(String option : options){
            TextView c = new TextView(this);
            c.setHeight(200);
            c.setText(CustomLayoutsUtils.convertFileName(option, false));
            c.setTextSize((float)30);
            registerForContextMenu(c);
            c.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    String name = ""+((TextView)view).getText();
                    Log.e("#","Layout "+name+" held");
                    return false;
                }
            });
            ly.addView(c,AT_START);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.layout_options, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.e("#","Option <"+item.getTitle()+"> pressed");
        return super.onContextItemSelected(item);

    }

    public void launch_server_settings(View v){
        startActivity(new Intent(this, ServerSettings.class));
    }
}
