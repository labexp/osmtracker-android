package me.guillaumin.android.osmtracker.activity;

import android.app.Activity;
import android.os.Bundle;

public class SetPreferenceActivity extends Activity{
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
     // TODO Auto-generated method stub
     super.onCreate(savedInstanceState);
     
     getFragmentManager().beginTransaction().replace(android.R.id.content,
                   new Preferences()).commit();
    }

}
