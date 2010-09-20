package me.guillaumin.android.osmtracker.activity;

import java.io.File;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TracklistAdapter;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.exception.CreateTrackException;
import me.guillaumin.android.osmtracker.gpx.ExportTrackTask;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Lists existing tracks.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackManager extends ListActivity {
	
	private static final String TAG = TrackManager.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trackmanager);	
		getListView().setEmptyView(findViewById(R.id.trackmgr_empty));
		registerForContextMenu(getListView());
	}

	@Override
	protected void onResume() {
		// Tell service to stop notifying user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));

		Cursor cursor = getContentResolver().query(
				TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
				Schema.COL_START_DATE + " asc");
		startManagingCursor(cursor);
		setListAdapter(new TracklistAdapter(TrackManager.this, cursor));

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
		getMenuInflater().inflate(R.menu.trackmgr_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.trackmgr_menu_newtrack:
			// Start track logger activity
			try {
				long trackId = createNewTrack();
				Intent i = new Intent(this, TrackLogger.class);
				i.putExtra(Schema.COL_TRACK_ID, trackId);
				startActivity(i);
			} catch (CreateTrackException cte) {
				Toast.makeText(this,
						getResources().getString(R.string.trackmgr_newtrack_error).replace("{0}", cte.getMessage()),
						Toast.LENGTH_LONG)
						.show();
			}
			break;
		case R.id.trackmgr_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		case R.id.trackmgr_menu_about:
			// Start About activity
			startActivity(new Intent(this, About.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.trackmgr_contextmenu, menu);
		menu.setHeaderTitle(R.string.trackmgr_contextmenu_title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		switch(item.getItemId()) {
		case R.id.trackmgr_contextemenu_delete:
			
			// Confirm and delete selected track
			new AlertDialog.Builder(this)
				.setTitle(R.string.trackmgr_contextmenu_delete)
				.setMessage(getResources().getString(R.string.trackmgr_delete_confirm).replace("{0}", Long.toString(info.id)))
				.setCancelable(true)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						getContentResolver().delete(
								ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, info.id),
								null, null);
						((CursorAdapter) TrackManager.this.getListAdapter()).getCursor().requery();
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();				
					}
				}).create().show();
			
			break;
		case R.id.trackmgr_contextemenu_export:	
			new ExportTrackTask(this, info.id).execute();
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	/**
	 * Create a new track, in DB and on SD card
	 * @returns The ID of the new track
	 * @throws CreateTrackException
	 */
	private long createNewTrack() throws CreateTrackException {

		// Create directory for track
		File sdRoot = Environment.getExternalStorageDirectory();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String storageDir = prefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);
		if (sdRoot.canWrite()) {
			// Create base OSMTracker directory on SD Card
			File osmTrackerDir = new File(sdRoot + storageDir);
			if (!osmTrackerDir.exists()) {
				osmTrackerDir.mkdir();
			}

			// Create track directory
			Date startDate = new Date();
			File trackDir = new File(osmTrackerDir + File.separator + DataHelper.FILENAME_FORMATTER.format(startDate));
			trackDir.mkdir();
			
			// Create entry in TRACK table
			ContentValues values = new ContentValues();
			values.put(Schema.COL_NAME, "");
			values.put(Schema.COL_START_DATE, startDate.getTime());
			values.put(Schema.COL_DIR, trackDir.getAbsolutePath());
			values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
			Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
			long trackId = ContentUris.parseId(trackUri);

			// Only one track active at a time, as a safety measure
			values.clear();
			values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_INACTIVE);
			getContentResolver().update(TrackContentProvider.CONTENT_URI_TRACK, values, Schema.COL_ID + "<> ?", new String[] {Long.toString(trackId)});
			
			return trackId;
		} else {
			throw new CreateTrackException(getResources().getString(R.string.error_externalstorage_not_writable));
		}
	}
	
}
