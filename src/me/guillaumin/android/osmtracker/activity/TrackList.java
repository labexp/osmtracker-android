package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TracklistAdapter;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;

/**
 * Lists existing tracks.
 * @author Nicolas Guillaumin
 *
 */
public class TrackList extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	};
	
	@Override
	protected void onResume() {
		// Tell service to stop notifying user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
	
		Cursor cursor = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACK, null, null, null, Schema.COL_START_DATE + " asc");
		startManagingCursor(cursor);
		setListAdapter(new TracklistAdapter(TrackList.this, cursor));
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		if (adapter != null) {
			// Properly close the adapter cursor
			Cursor cursor = adapter.getCursor();
			stopManagingCursor(cursor);
			cursor.close();
			setListAdapter(null);
		}
		
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklist_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.tracklist_menu_newtrack:
			// Start tracklogger activity
			startActivity(new Intent(this, TrackLogger.class));
			break;
		case R.id.tracklist_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		case R.id.tracklist_menu_about:
			// Start About activity
			startActivity(new Intent(this, About.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
