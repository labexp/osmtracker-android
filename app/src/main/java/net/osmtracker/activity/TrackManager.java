package net.osmtracker.activity;

import java.io.File;
import java.util.Date;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.TracklistAdapter;
import net.osmtracker.exception.CreateTrackException;
import net.osmtracker.gpx.ExportToStorageTask;
import net.osmtracker.util.FileSystemUtils;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lists existing tracks.
 * Each track is displayed using {@link TracklistAdapter}.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackManager extends ListActivity {
	
	@SuppressWarnings("unused")
	private static final String TAG = TrackManager.class.getSimpleName();

	final private int RC_WRITE_PERMISSIONS_EXPORT_ALL = 1;
	final private int RC_WRITE_PERMISSIONS_EXPORT_ONE = 2;

	/** Bundle key for {@link #prevItemVisible} */
	private static final String PREV_VISIBLE = "prev_visible";

	/** Constant used if no track is active (-1)*/
	private static final long TRACK_ID_NO_TRACK = -1;
	
	/** The active track being recorded, if any, or {@link TRACK_ID_NO_TRACK}; value is updated in {@link #onResume()} */
	private long currentTrackId = TRACK_ID_NO_TRACK;

	/** The previous item visible, or -1; for scrolling back to its position in {@link #onResume()} */
	private int prevItemVisible = -1;

	/** Track Identifier to export after request for write permission **/
	private long trackId = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trackmanager);	
		getListView().setEmptyView(findViewById(R.id.trackmgr_empty));
		registerForContextMenu(getListView());
		if (savedInstanceState != null) {
			prevItemVisible = savedInstanceState.getInt(PREV_VISIBLE, -1);
		}
	}

	@Override
	protected void onResume() {
		Cursor cursor = getContentResolver().query(
				TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
				TrackContentProvider.Schema.COL_START_DATE + " desc");
		startManagingCursor(cursor);
		setListAdapter(new TracklistAdapter(TrackManager.this, cursor));
		getListView().setEmptyView(findViewById(R.id.trackmgr_empty));  // undo change from onPause

		// Is any track active?
		currentTrackId = DataHelper.getActiveTrackId(getContentResolver());
		if (currentTrackId != TRACK_ID_NO_TRACK) {
			((TextView) findViewById(R.id.trackmgr_hint)).setText(
					getResources().getString(R.string.trackmgr_continuetrack_hint)
						.replace("{0}", Long.toString(currentTrackId)));

			// Scroll to the active track of the list
			cursor.moveToFirst();
			// we will use the flag selectionSet to handle the while loop
			boolean selectionSet = false;
			while(!selectionSet && cursor.moveToNext()){
				if(cursor.getInt(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ACTIVE)) == 1){
					// This is the active track
					// set selection to the current cursor position
					getListView().setSelection(cursor.getPosition());
					selectionSet = true;
				}
			}
		} else {
			((TextView) findViewById(R.id.trackmgr_hint)).setText(R.string.trackmgr_newtrack_hint);

			// Scroll to the previous listview position,
			// now that we're bound to data again
			if (prevItemVisible != -1) {
				final int cmax = getListView().getCount() - 1;
				if (prevItemVisible > cmax) {
					prevItemVisible = cmax;
				}
				getListView().setSelection(prevItemVisible);
			}
		}

		super.onResume();
	}

	@Override
	protected void onPause() {
		// Remember position in listview (before any adapter change)
		prevItemVisible = getListView().getFirstVisiblePosition();

		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		if (adapter != null) {
			// Prevents on-screen 'no tracks' message
			getListView().setEmptyView(findViewById(android.R.id.empty));
			// Properly close the adapter cursor
			Cursor cursor = adapter.getCursor();
			stopManagingCursor(cursor);
			cursor.close();
			setListAdapter(null);
		}

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(PREV_VISIBLE, prevItemVisible);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		prevItemVisible = state.getInt(PREV_VISIBLE, -1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.trackmgr_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (currentTrackId != -1) {
			// Currently tracking. Display "Continue" option
			menu.findItem(R.id.trackmgr_menu_continuetrack).setVisible(true);
			
			// Display a 'stop tracking' option
			menu.findItem(R.id.trackmgr_menu_stopcurrenttrack).setVisible(true);
		} else {
			// Not currently tracking. Remove "Continue" option
			menu.findItem(R.id.trackmgr_menu_continuetrack).setVisible(false);
			
			// Remove the 'stop tracking' option
			menu.findItem(R.id.trackmgr_menu_stopcurrenttrack).setVisible(false);
		}
		
		// Remove "delete all" button if no tracks
		menu.findItem(R.id.trackmgr_menu_deletetracks).setVisible(getListView().getCount() > 0);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.trackmgr_menu_newtrack:
			// Start track logger activity
			try {
				Intent i = new Intent(this, TrackLogger.class);
				// New track
				currentTrackId = createNewTrack();
				i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
				startActivity(i);
			} catch (CreateTrackException cte) {
				Toast.makeText(this,
						getResources().getString(R.string.trackmgr_newtrack_error).replace("{0}", cte.getMessage()),
						Toast.LENGTH_LONG)
						.show();
			}
			break;
		case R.id.trackmgr_menu_continuetrack:
			Intent i = new Intent(this, TrackLogger.class);
			i.putExtra(TrackLogger.STATE_IS_TRACKING, true);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
			startActivity(i);
			break;
		case R.id.trackmgr_menu_stopcurrenttrack:
			stopActiveTrack();
			break;
		case R.id.trackmgr_menu_deletetracks:
			// Confirm and delete all track
			new AlertDialog.Builder(this)
				.setTitle(R.string.trackmgr_contextmenu_delete)
				.setMessage(getResources().getString(R.string.trackmgr_deleteall_confirm))
				.setCancelable(true)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.menu_deletetracks, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteAllTracks();
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
		case R.id.trackmgr_menu_exportall:
			// Confirm
			new AlertDialog.Builder(this)
				.setTitle(R.string.menu_exportall)
				.setMessage(getResources().getString(R.string.trackmgr_exportall_confirm))
				.setCancelable(true)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.menu_exportall, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						requestPermissionAndExport(TrackManager.this.RC_WRITE_PERMISSIONS_EXPORT_ALL);
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).create().show();
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


	private void requestPermissionAndExport(int typeCode){
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				// TODO: explain why we need permission.
				Log.w(TAG, "we should explain why we need write permission_REQUEST");
				Toast.makeText(this, "To export the GPX trace we need to write on the storage.", Toast.LENGTH_LONG).show();

			} else {

				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, typeCode);
			}

		}  else {
			switch (typeCode) {
				case RC_WRITE_PERMISSIONS_EXPORT_ALL:
					exportAllTracks();

				case RC_WRITE_PERMISSIONS_EXPORT_ONE:
					exportOneTrack();
			}
		}
	}

	private void exportOneTrack(){
		if (trackId != -1) {
			new ExportToStorageTask(this, trackId).execute();
			trackId = -1;
		}
	}

	private void exportAllTracks(){
		Cursor cursor = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACK,
				null, null, null, TrackContentProvider.Schema.COL_START_DATE + " desc");
		if (cursor.moveToFirst()) {
			long[] ids = new long[cursor.getCount()];
			int idCol = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID);
			int i=0;
			do {
				ids[i++] = cursor.getLong(idCol);
			} while (cursor.moveToNext());

			new ExportToStorageTask(TrackManager.this, ids).execute();
		}
		cursor.close();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.trackmgr_contextmenu, menu);
		
		long selectedId = ((AdapterContextMenuInfo) menuInfo).id;
		menu.setHeaderTitle(getResources().getString(R.string.trackmgr_contextmenu_title).replace("{0}", Long.toString(selectedId)));
		if(currentTrackId == selectedId){
			// the selected one is the active track, so we will show the stop item
			menu.findItem(R.id.trackmgr_contextmenu_stop).setVisible(true);
		}else{
			// the selected item is not active, so we need to hide the stop item
			menu.findItem(R.id.trackmgr_contextmenu_stop).setVisible(false);
		}
		menu.setHeaderTitle(getResources().getString(R.string.trackmgr_contextmenu_title).replace("{0}", Long.toString(selectedId)));
		if ( currentTrackId ==  selectedId) {
			// User has pressed the active track, hide the delete option
			menu.removeItem(R.id.trackmgr_contextmenu_delete);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		Intent i;
		
		switch(item.getItemId()) {
		case R.id.trackmgr_contextmenu_stop:
			// stop the active track
			stopActiveTrack();
			break;
		case R.id.trackmgr_contextmenu_resume:
			// let's activate the track and start the TrackLogger activity
			setActiveTrack(info.id);
			i = new Intent(this, TrackLogger.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, info.id);
			startActivity(i);
			break;
		case R.id.trackmgr_contextmenu_delete:
			
			// Confirm and delete selected track
			new AlertDialog.Builder(this)
				.setTitle(R.string.trackmgr_contextmenu_delete)
				.setMessage(getResources().getString(R.string.trackmgr_delete_confirm).replace("{0}", Long.toString(info.id)))
				.setCancelable(true)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteTrack(info.id);
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
		case R.id.trackmgr_contextmenu_export:
			trackId = info.id;
			requestPermissionAndExport(this.RC_WRITE_PERMISSIONS_EXPORT_ONE);
			break;
		case R.id.trackmgr_contextmenu_osm_upload:
			i = new Intent(this, OpenStreetMapUpload.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, info.id);
			startActivity(i);
			break;
		case R.id.trackmgr_contextmenu_display:
			// Start display track activity, with or without OSM background
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
			} else {
				i = new Intent(this, DisplayTrack.class);
			}
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, info.id);
			startActivity(i);
			break;
		case R.id.trackmgr_contextmenu_details:
			i = new Intent(this, TrackDetail.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, info.id);
			startActivity(i);
			break;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * User has clicked the active track or a previous track.
	 * @param lv listview; this
	 * @param iv item clicked
	 * @param position position within list
	 * @param id  track ID
	 */
	@Override
	protected void onListItemClick(ListView lv, View iv, final int position, final long id) {
		Intent i;
		if (id == currentTrackId) {
			// continue recording the current track
			i = new Intent(this, TrackLogger.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
			i.putExtra(TrackLogger.STATE_IS_TRACKING, true);
		} else {
			// show track info
			i = new Intent(this, TrackDetail.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, id);
		}
		startActivity(i);
	}

	/**
	 * Creates a new track, in DB and on SD card
	 * @returns The ID of the new track
	 * @throws CreateTrackException
	 */
	private long createNewTrack() throws CreateTrackException {
		Date startDate = new Date();
		
		// Create entry in TRACK table
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_NAME, "");
		values.put(TrackContentProvider.Schema.COL_START_DATE, startDate.getTime());
		values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
		Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);

		// set the active track
		setActiveTrack(trackId);
		
		return trackId;
	}
	
	/**
	 * Deletes the track with the specified id from DB and SD card
	 * @param The ID of the track to be deleted
	 */
	private void deleteTrack(long id) {
		getContentResolver().delete(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, id),
				null, null);
		((CursorAdapter) TrackManager.this.getListAdapter()).getCursor().requery();

		// Delete any data stored for the track we're deleting
		File trackStorageDirectory = DataHelper.getTrackDirectory(id);
		if (trackStorageDirectory.exists()) {
			FileSystemUtils.delete(trackStorageDirectory, true);
		}
	}

	/**
	 * Deletes all tracks and their data
	 */
	private void deleteAllTracks() {
		Cursor cursor = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACK, null, null, null, TrackContentProvider.Schema.COL_START_DATE + " asc");

		// Stop any currently active tracks
		if (currentTrackId != -1) {
			stopActiveTrack();
		}

		if (cursor.moveToFirst()) {
			int id_col = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID);
			do {
				deleteTrack(cursor.getLong(id_col));
			} while (cursor.moveToNext());
		}
		cursor.close();
	}

	/**
	 * Sets the active track
	 * calls {@link stopActiveTrack()} to stop all currently 
	 * @param trackId ID of the track to activate
	 */
	private void setActiveTrack(long trackId){
		
		// to be sure that no tracking will be in progress when we set a new track
		stopActiveTrack();
		
		// set the track active
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
		getContentResolver().update(TrackContentProvider.CONTENT_URI_TRACK, values, TrackContentProvider.Schema.COL_ID + " = ?", new String[] {Long.toString(trackId)});
	}
	
	/**
	 * Stops the active track
	 * Sends a broadcast to be received by GPSLogger to stop logging
	 * and forces the DataHelper to stop tracking.
	 */
	private void stopActiveTrack(){
		if(currentTrackId != TRACK_ID_NO_TRACK){
			// we send a broadcast to inform all registered services to stop tracking 
			Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
			sendBroadcast(intent);
			
			// need to get sure, that the database is up to date
			DataHelper dataHelper = new DataHelper(this);
			dataHelper.stopTracking(currentTrackId);

			// set the currentTrackId to "no track"
			currentTrackId = TRACK_ID_NO_TRACK;
			
		}
	}

	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case RC_WRITE_PERMISSIONS_EXPORT_ALL: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					exportAllTracks();

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "we should explain why we need write permission_EXPORT_ALL");
					Toast.makeText(this, "To export the GPX trace we need to write on the storage.", Toast.LENGTH_LONG).show();
				}
			}
			case RC_WRITE_PERMISSIONS_EXPORT_ONE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					exportOneTrack();

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "we should explain why we need write permission_EXPORT_ONE");
					Toast.makeText(this, "To export the GPX trace we need to write on the storage.", Toast.LENGTH_LONG).show();
				}
			}
		}
	}


}
