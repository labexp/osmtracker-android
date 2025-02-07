package net.osmtracker.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.exception.CreateTrackException;
import net.osmtracker.gpx.ExportToStorageTask;
import net.osmtracker.gpx.ExportToTempFileTask;
import net.osmtracker.gpx.ZipHelper;
import net.osmtracker.util.FileSystemUtils;

import java.io.File;
import java.util.Date;

/**
 * Lists existing tracks. Each track is displayed using {@link RecyclerView}
 *
 *  Original @author Nicolas Guillaumin
 */
public class TrackManager extends AppCompatActivity
		implements TrackListRVAdapter.TrackListRecyclerViewAdapterListener {

	private static final String TAG = TrackManager.class.getSimpleName();;

	final private int RC_WRITE_PERMISSIONS_UPLOAD = 4;
	final private int RC_WRITE_STORAGE_DISPLAY_TRACK = 3;
	final private int RC_WRITE_PERMISSIONS_EXPORT_ALL = 1;
	final private int RC_WRITE_PERMISSIONS_EXPORT_ONE = 2;
	final private int RC_GPS_PERMISSION = 5;
	final private int RC_WRITE_PERMISSIONS_SHARE = 6;

	/** Bundle key for {@link #prevItemVisible} */
	private static final String PREV_VISIBLE = "prev_visible";

	/** Constant used if no track is active (-1)*/
	private static final long TRACK_ID_NO_TRACK = -1;

	// The active track being recorded, if any, or {TRACK_ID_NO_TRACK};
	// value is updated in {@link #onResume()}
	private long currentTrackId = TRACK_ID_NO_TRACK;

	//Use to know which view holder's trackId was selected on the recycler view
	private long contextMenuSelectedTrackid = TRACK_ID_NO_TRACK;

	/** The previous item visible, or -1; for scrolling back to its position in {#onResume()} */
	private int prevItemVisible = -1;

	// This variable is used to communicate between code trying to start TrackLogger
	// and the code that actually starts it when have GPS permissions
	private Intent TrackLoggerStartIntent = null;

	private TrackListRVAdapter recyclerViewAdapter;

	// To check if the RecyclerView already has a DividerItemDecoration added
	private boolean hasDivider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trackmanager);

		Toolbar myToolbar = findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		if (savedInstanceState != null) {
			prevItemVisible = savedInstanceState.getInt(PREV_VISIBLE, -1);
		}

		FloatingActionButton fab = findViewById(R.id.trackmgr_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startTrackLoggerForNewTrack();
			}
		});

		// should check if is the first time using the app
		boolean showAppIntro = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO,
						OSMTracker.Preferences.VAL_DISPLAY_APP_INTRO);
		if (showAppIntro) {
			Intent intro = new Intent(this, Intro.class);
			startActivity(intro);
		}
		RecyclerView recyclerView = findViewById(R.id.recyclerview);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		// Adding a horizontal divider
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
		dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider)); // Using a custom drawable

		recyclerView.addItemDecoration(dividerItemDecoration);
	}

	@Override
	protected void onResume() {
		setRecyclerView();

		TextView emptyView = findViewById(R.id.trackmgr_empty);
		//No tracks
		if (recyclerViewAdapter.getItemCount() == 0) {
			emptyView.setVisibility(View.VISIBLE);
		} else{
			emptyView.setVisibility(View.INVISIBLE);
			// Is any track active?
			currentTrackId = DataHelper.getActiveTrackId(getContentResolver());
			if (currentTrackId != TRACK_ID_NO_TRACK) {
				Snackbar.make(findViewById(R.id.trackmgr_fab),
						getResources().getString(R.string.trackmgr_continuetrack_hint)
						.replace("{0}", Long.toString(currentTrackId)), Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		}

		super.onResume();
	}


	/**
	 * Configures and initializes the RecyclerView for displaying the list of tracks.
	 */
	private void setRecyclerView() {
		RecyclerView recyclerView = findViewById(R.id.recyclerview);

		LinearLayoutManager layoutManager = new LinearLayoutManager(this,
				LinearLayoutManager.VERTICAL, false);
		recyclerView.setLayoutManager(layoutManager);
		// adds a divider decoration if not already present
		if (!hasDivider) {
			DividerItemDecoration did = new DividerItemDecoration(recyclerView.getContext(),
					layoutManager.getOrientation());
			recyclerView.addItemDecoration(did);
			hasDivider = true;
		}
		recyclerView.setHasFixedSize(true);
		Cursor cursor = getContentResolver().query(
				TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
				TrackContentProvider.Schema.COL_START_DATE + " desc");

		recyclerViewAdapter = new TrackListRVAdapter(this, cursor, this);
		recyclerView.setAdapter(recyclerViewAdapter);
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
		int tracksCount = recyclerViewAdapter.getItemCount();
		menu.findItem(R.id.trackmgr_menu_deletetracks).setVisible(tracksCount > 0);
		menu.findItem(R.id.trackmgr_menu_exportall).setVisible(tracksCount > 0);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.trackmgr_menu_newtrack:
				startTrackLoggerForNewTrack();
				break;
			case R.id.trackmgr_menu_continuetrack:
				Intent i = new Intent(this, TrackLogger.class);
				i.putExtra(TrackLogger.STATE_IS_TRACKING, true);
				i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
				tryStartTrackLogger(i);
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
				if (!writeExternalStoragePermissionGranted()){
					Log.e(TAG, "ExportAllWrite - Permission asked");
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							RC_WRITE_PERMISSIONS_EXPORT_ALL);
				}
				else exportTracks(false);
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


	/**
	 * Starts TrackLogger Activity if GPS Permission is granted
	 * If there's no GPS Permission, then requests it and the OnPermissionResult will call this
	 * method again if granted
	 */
	private void tryStartTrackLogger(Intent intent){
		// If GPS Permission Granted
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			Log.i(TAG,"Permission granted on try");
			startActivity(intent);
		} else{
			// Permission is not granted
			Log.i(TAG,"Not Granted on try");
			this.TrackLoggerStartIntent = intent;
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.ACCESS_FINE_LOCATION)) {
				Log.i(TAG,"Should explain");
				Toast.makeText(this, R.string.gps_perms_required,
						Toast.LENGTH_LONG).show();
			}

			// No explanation needed, just request the permission.
			Log.i(TAG,"Should not explain");
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, RC_GPS_PERMISSION);

		}
	}

	/**
	 * This method prepare the new track and set an id, then start a new TrackLogger with the new track id
	 */
	private void startTrackLoggerForNewTrack(){
		// Start track logger activity
		try {
			Intent i = new Intent(this, TrackLogger.class);
			// New track
			currentTrackId = createNewTrack();
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
			tryStartTrackLogger(i);
		} catch (CreateTrackException cte) {
			Toast.makeText(this,
					getResources().getString(R.string.trackmgr_newtrack_error).replace("{0}",
							cte.getMessage()), Toast.LENGTH_LONG).show();
		}
	}


	/* Export tracks
	 * onlySelectedTrack: will export only the track selected on the recycle view.
	 */
	private void exportTracks(boolean onlyContextMenuSelectedTrack) {

		long[] trackIds = null;

		// Select the trackIds to be exported
		if (onlyContextMenuSelectedTrack) {
			trackIds = new long[1];
			trackIds[0] = contextMenuSelectedTrackid;
		} else {
			Cursor cursor = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACK,
					null, null, null,
					TrackContentProvider.Schema.COL_START_DATE + " desc");
			if (cursor.moveToFirst()) {
				trackIds = new long[cursor.getCount()];
				int idCol = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID);
				int i = 0;
				do {
					trackIds[i++] = cursor.getLong(idCol);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}

		// Invoke the Async Task
		new ExportToStorageTask(this, trackIds) {
			@Override
			protected void onPostExecute(Boolean success) {
				dialog.dismiss();
				if (!success) {
					new AlertDialog.Builder(context).setTitle(android.R.string.dialog_alert_title)
							.setMessage(context.getResources()
									.getString(R.string.trackmgr_export_error)
									.replace("{0}", super.getErrorMsg()))
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setNeutralButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
				}else{
					Snackbar.make(findViewById(R.id.trackmgr_fab),
							getResources().getString(R.string.various_export_finished),
							Snackbar.LENGTH_LONG).setAction("Action", null).show();
					updateTrackItemsInRecyclerView();
				}
			}
		}.execute();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo, long trackId) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.trackmgr_contextmenu, menu);
		contextMenuSelectedTrackid = trackId;

		menu.setHeaderTitle(getResources().getString(R.string.trackmgr_contextmenu_title).replace("{0}", Long.toString(contextMenuSelectedTrackid)));
		if(currentTrackId == contextMenuSelectedTrackid){
			// the selected one is the active track, so we will show the stop item
			menu.findItem(R.id.trackmgr_contextmenu_stop).setVisible(true);
		}else{
			// the selected item is not active, so we need to hide the stop item
			menu.findItem(R.id.trackmgr_contextmenu_stop).setVisible(false);
		}
		menu.setHeaderTitle(getResources().getString(R.string.trackmgr_contextmenu_title).replace("{0}", Long.toString(contextMenuSelectedTrackid)));
		if ( currentTrackId ==  contextMenuSelectedTrackid) {
			// User has pressed the active track, hide the delete option
			menu.removeItem(R.id.trackmgr_contextmenu_delete);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent i;

		switch(item.getItemId()) {
			case R.id.trackmgr_contextmenu_stop:
				// stop the active track
				stopActiveTrack();
				break;
			case R.id.trackmgr_contextmenu_resume:
				// Activate the selected track if it is different from the currently active one
				// (or if no track is currently active)
				if (currentTrackId != contextMenuSelectedTrackid) {
					setActiveTrack(contextMenuSelectedTrackid);
				}
				// Start the TrackLogger activity to begin logging the selected track
				i = new Intent(this, TrackLogger.class);
				i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, contextMenuSelectedTrackid);
				tryStartTrackLogger(i);
				break;

			case R.id.trackmgr_contextmenu_delete:
				// Confirm and delete selected track
				new AlertDialog.Builder(this)
						.setTitle(R.string.trackmgr_contextmenu_delete)
						.setMessage(getResources().getString(R.string.trackmgr_delete_confirm)
								.replace("{0}", Long.toString(contextMenuSelectedTrackid)))
						.setCancelable(true)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteTrack(contextMenuSelectedTrackid);
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
				if (writeExternalStoragePermissionGranted()) {
					exportTracks(true);
				} else {
					Log.e(TAG, "ExportAsGPXWrite - Permission asked");
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							RC_WRITE_PERMISSIONS_EXPORT_ONE);
				}
				break;

			case R.id.trackmgr_contextmenu_share:
				if (writeExternalStoragePermissionGranted()) {
					prepareAndShareTrack(contextMenuSelectedTrackid, this);
				} else {
					Log.e(TAG, "Share GPX - Permission asked");
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							RC_WRITE_PERMISSIONS_SHARE);
				}
				break;

			case R.id.trackmgr_contextmenu_osm_upload:
				if (writeExternalStoragePermissionGranted()) {
					uploadTrack(contextMenuSelectedTrackid);
				} else {
					Log.e(TAG, "OsmUploadWrite - Permission asked");
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							RC_WRITE_PERMISSIONS_UPLOAD);
				}
				break;

			case R.id.trackmgr_contextmenu_display:
				if (writeExternalStoragePermissionGranted()) {
					displayTrack(contextMenuSelectedTrackid);
				} else {
					Log.e(TAG, "DisplayTrackMapWrite - Permission asked");
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_STORAGE_DISPLAY_TRACK);
				}
				break;

			case R.id.trackmgr_contextmenu_details:
				i = new Intent(this, TrackDetail.class);
				i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, contextMenuSelectedTrackid);
				startActivity(i);
				break;
		}
		return super.onContextItemSelected(item);
	}

	private void uploadTrack(long trackId){
		Intent i = new Intent(this, OpenStreetMapUpload.class);
		i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
		startActivity(i);
	}

	private void displayTrack(long trackId){
		Log.e(TAG, "On Display Track");
		// Start display track activity, with or without OSM background
		Intent i;
		boolean useOpenStreetMapBackground = PreferenceManager
				.getDefaultSharedPreferences(this).getBoolean(
						OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM,
						OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
		if (useOpenStreetMapBackground) {
			i = new Intent(this, DisplayTrackMap.class);
		} else {
			i = new Intent(this, DisplayTrack.class);
		}
		i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
		startActivity(i);
	}

	private boolean writeExternalStoragePermissionGranted(){
		// On versions lower than Android 11, write external storage permission is required.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
			Log.d(TAG, "CHECKING - Write");
			return ContextCompat.checkSelfPermission(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		} else {
			Log.d(TAG, "Write External Storage is granted");
			return true;
		}
	}

	@Override
	public void onClick(long trackId) {
		Intent i;
		if (trackId == currentTrackId) {
			// continue recording the current track
			i = new Intent(this, TrackLogger.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
			i.putExtra(TrackLogger.STATE_IS_TRACKING, true);
			tryStartTrackLogger(i);

		} else {
			// show track info
			i = new Intent(this, TrackDetail.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
			startActivity(i);
		}
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
		values.put(TrackContentProvider.Schema.COL_NAME,
				DataHelper.FILENAME_FORMATTER.format(new Date()));
		values.put(TrackContentProvider.Schema.COL_START_DATE, startDate.getTime());
		values.put(TrackContentProvider.Schema.COL_ACTIVE,
				TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
		Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);

		// set the active track
		setActiveTrack(trackId);

		return trackId;
	}

	// This should be static because contains an AsyncTask
	// AsyncTasks has to live inside a static environment
	// That's why the Context is passed as a parameter
	private static void prepareAndShareTrack(final long trackId, Context context) {
		// Create temp file that will remain in cache
		new ExportToTempFileTask(context, trackId){
			@Override
			protected void executionCompleted(){
				// Create zip file or an archive in case there are no files associated with the trace
				File zipFile = ZipHelper.zipCacheFiles(context, trackId, this.getTmpFile());
				shareFile(zipFile, context);
			}

			@Override
			protected void onPostExecute(Boolean success) {
				dialog.dismiss();
				if (!success) {
					new AlertDialog.Builder(context)
							.setTitle(android.R.string.dialog_alert_title)
							.setMessage(context.getResources()
									.getString(R.string.trackmgr_prepare_for_share_error)
									.replace("{0}", Long.toString(trackId)))
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.show();
				}else{
					executionCompleted();
				}
			}
		}.execute();
	}

	/**
	 * Allows user to share gpx file from storage to another app
	 * @param tmpGPXFile track identifier
	 */
	private static void shareFile(File tmpGPXFile, Context context) {

		// Get gpx content URI
		Uri trackUriContent = FileProvider.getUriForFile(context,
				DataHelper.FILE_PROVIDER_AUTHORITY,
				tmpGPXFile);

		// Sharing intent
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, trackUriContent);
		shareIntent.setType(DataHelper.MIME_TYPE_GPX);
		context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.trackmgr_contextmenu_share)));

	}

	/**
	 * Deletes the track with the specified id from DB and SD card
	 * @param  id of the track to be deleted
	 */
	private void deleteTrack(long id) {
		getContentResolver().delete(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, id),
				null, null);
		updateTrackItemsInRecyclerView();

		// Delete any data stored for the track we're deleting
		File trackStorageDirectory = DataHelper.getTrackDirectory(id, this);
		if (trackStorageDirectory.exists()) {
			FileSystemUtils.delete(trackStorageDirectory, true);
		}
	}

	/*
	 * This method updates the track items in the user interface . Is used when data in DB change
	 * (export or delete track) to force the UI reflect the change.
	 */
	private void updateTrackItemsInRecyclerView() {
		recyclerViewAdapter.getCursorAdapter().getCursor().requery();
		recyclerViewAdapter.notifyDataSetChanged();
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
		recyclerViewAdapter.getItemId(0);

		if (cursor != null && cursor.moveToFirst()) {
			int id_col = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID);
			do {
				deleteTrack(cursor.getLong(id_col));
			} while (cursor.moveToNext());
			cursor.close();
		}

	}

	/**
	 * Sets the active track
	 * calls {stopActiveTrack()} to stop all currently
	 * @param trackId ID of the track to activate
	 */
	private void setActiveTrack(long trackId){

		// to be sure that no tracking will be in progress when we set a new track
		stopActiveTrack();

		// set the track active
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_ACTIVE,
				TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
		getContentResolver().update(TrackContentProvider.CONTENT_URI_TRACK, values,
				TrackContentProvider.Schema.COL_ID + " = ?",
				new String[] {Long.toString(trackId)});
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
			intent.setPackage(this.getPackageName());
			sendBroadcast(intent);

			// need to get sure, that the database is up to date
			DataHelper dataHelper = new DataHelper(this);
			dataHelper.stopTracking(currentTrackId);

			// set the currentTrackId to "no track"
			currentTrackId = TRACK_ID_NO_TRACK;

			// Change icon on track item
			updateTrackItemsInRecyclerView();

		}
	}

	public void onRequestPermissionsResult(int requestCode, String permissions[],
										   int[] grantResults) {
		switch (requestCode) {
			case RC_WRITE_PERMISSIONS_EXPORT_ALL: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					exportTracks(false);

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "we should explain why we need write permission_EXPORT_ALL");
					Toast.makeText(this, R.string.storage_permission_for_export_GPX, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case RC_WRITE_PERMISSIONS_EXPORT_ONE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					exportTracks(true);

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "we should explain why we need write permission_EXPORT_ONE");
					Toast.makeText(this, R.string.storage_permission_for_export_GPX, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case RC_WRITE_STORAGE_DISPLAY_TRACK: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.e(TAG, "Result - Permission granted");
					// permission was granted, yay!
					displayTrack(contextMenuSelectedTrackid);
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "Permission not granted");
					Toast.makeText(this, R.string.storage_permission_for_display_track, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case RC_WRITE_PERMISSIONS_SHARE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.e(TAG, "Result - Permission granted");
					// permission was granted, yay!
					displayTrack(contextMenuSelectedTrackid);
					prepareAndShareTrack(contextMenuSelectedTrackid, this);
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "Permission not granted");
					Toast.makeText(this, R.string.storage_permission_for_share_track, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case RC_WRITE_PERMISSIONS_UPLOAD: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.e(TAG, "Result - Permission granted");
					// permission was granted, yay!
					uploadTrack(contextMenuSelectedTrackid);
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
					Log.w(TAG, "Permission not granted");
					Toast.makeText(this, R.string.storage_permission_for_upload_to_OSM, Toast.LENGTH_LONG).show();
				}
				break;
			}
			case RC_GPS_PERMISSION: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.i(TAG, "GPS Permission granted");
					tryStartTrackLogger(this.TrackLoggerStartIntent);
				} else {
					Log.i(TAG, "GPS Permission denied");
				}
				break;
			}
		}
	}
}
