package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Simply display the about screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class About extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
	}
	
	@Override
	protected void onResume() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		super.onPause();
	}
}
