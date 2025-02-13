package net.osmtracker.activity;

import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.WaypointListAdapter;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.CursorAdapter;
import android.widget.ListView;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView listView = getListView();
		listView.setFitsSystemWindows(true);
		listView.setClipToPadding(false);
		listView.setPadding(0, 48, 0, 0);
	}

	@Override
	protected void onResume() {
		Long trackId = getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);

		Cursor cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " desc");
		startManagingCursor(cursor);
		setListAdapter(new WaypointListAdapter(WaypointList.this, cursor));

		super.onResume();
	}

}
