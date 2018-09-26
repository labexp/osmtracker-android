package net.osmtracker.activity;

import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.WaypointListAdapter;
import android.app.ListActivity;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import java.io.File;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

	@Override
	protected void onResume() {
		Long trackId = getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		
		Cursor cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " desc");
		startManagingCursor(cursor);
		setListAdapter(new WaypointListAdapter(WaypointList.this, cursor));
		
		super.onResume();
	}
	
	@Override
	protected void onListItemClick(ListView lv, View v, int pos, long id) {


		WaypointListAdapter wpa = (WaypointListAdapter)getListAdapter();
		
		if (wpa != null) {
			String audioFile = getIntent().getExtras().getString(TrackContentProvider.Schema.COL_LINK);
			if (audioFile!=null && audioFile.endsWith(".3gpp")) {
				Uri u = Uri.fromFile(new File(audioFile));
				MediaPlayer player = MediaPlayer.create(this, u);
				if (player != null) {
					player.setLooping(false);
					player.start();
				}
			}
		}

	}

	@Override
	protected void onPause() {
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

}
