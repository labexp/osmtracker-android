package me.guillaumin.android.osmtracker.activity;

import java.io.File;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.WaypointListAdapter;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
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
	protected void onResume() {
		Long trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		
		Cursor cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
				null, null, null, Schema.COL_TIMESTAMP + " desc");
		startManagingCursor(cursor);
		setListAdapter(new WaypointListAdapter(WaypointList.this, cursor));
		
		super.onResume();
	}
	
	@Override
	protected void onListItemClick(ListView lv, View v, int pos, long id) {
		System.out.println("on list item click ");

		/*		
		 * TODO:
		 * 
		WaypointListAdapter wpa = (WaypointListAdapter)getListAdapter();
		Cursor c = wpa.getCursor();

		Object o = getListView().getItemAtPosition(pos);
		System.out.println("debugitem "+o.getClass().getName());
		CursorWrapper g = (CursorWrapper)o;
		
		if (wpa != null) {

			String audioFile = getIntent().getExtras().getString(Schema.COL_LINK);
			if (audioFile!=null && audioFile.endsWith(".3gpp")) {
				Uri u = Uri.fromFile(new File(audioFile));
				MediaPlayer player = MediaPlayer.create(this, u);
				if (player != null) {
					player.setLooping(false);
					player.start();
				}
			}
		}
		*/
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
