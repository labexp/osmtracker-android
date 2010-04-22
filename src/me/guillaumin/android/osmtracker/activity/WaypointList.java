package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.WaypointListAdapter;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.app.ListActivity;
import android.content.Intent;
import android.widget.CursorAdapter;

/**
 * Activity that list the previous waypoints tracked
 * by the user.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class WaypointList extends ListActivity {
	
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new WaypointListAdapter(WaypointList.this, getContentResolver().query(TrackContentProvider.CONTENT_URI_WAYPOINT, null, null, null, Schema.COL_TIMESTAMP + " asc")));
	};
	
	@Override
	protected void onResume() {
		// Tell service to stop notifying user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		if (adapter != null) {
			// Properly close the adapter cursor
			adapter.getCursor().close();
			setListAdapter(null);
		}
		
		super.onPause();
	}
	
}
