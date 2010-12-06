package me.guillaumin.android.osmtracker.activity;

import java.io.File;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.layout.GpsStatusRecord;
import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import me.guillaumin.android.osmtracker.service.gps.GPSLoggerServiceConnection;
import me.guillaumin.android.osmtracker.view.TextNoteDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * Main track logger activity. Communicate with the GPS service to display GPS
 * status, and allow user to record waypoints.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackLogger extends Activity {

	private static final String TAG = TrackLogger.class.getSimpleName();

	/**
	 * Request code for callback after the camera application had taken a
	 * picture for us.
	 */
	private static final int REQCODE_IMAGE_CAPTURE = 0;

	/**
	 * Bundle state key for tracking flag.
	 */
	public static final String STATE_IS_TRACKING = "isTracking";

	/**
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

	/**
	 * GPS Logger service intent, to be used in start/stopService();
	 */
	private Intent gpsLoggerServiceIntent;

	/**
	 * Main button layout
	 */
	private UserDefinedLayout mainLayout;

	/**
	 * Flag to check GPS status at startup. Is cleared after the first
	 * displaying of GPS status dialog, to prevent the dialog to display if user
	 * goes to settings/about/other screen.
	 */
	private boolean checkGPSFlag = true;
	
	/**
	 * Keeps track of the image file when taking a picture.
	 */
	private File currentImageFile;
	
	/**
	 * Keeps track of the current track id.
	 */
	private long currentTrackId;

	/**
	 * Handles the bind to the GPS Logger service
	 */
	private ServiceConnection gpsLoggerConnection = new GPSLoggerServiceConnection(this);
	
	/**
	 * constant for text note dialog
	 */
	public static final int DIALOG_TEXT_NOTE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Get the track id to work with
		currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		Log.v(TAG, "Starting for track id " + currentTrackId);
		
		gpsLoggerServiceIntent = new Intent(this, GPSLogger.class);
		gpsLoggerServiceIntent.putExtra(Schema.COL_TRACK_ID, currentTrackId);

		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// Set application theme according to user settings
		String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
				OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
		setTheme(getResources().getIdentifier(theme, null, null));

		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracklogger);

		// Try to restore previous state
		boolean previousStateIsTracking = false;
		if (savedInstanceState != null) {
			previousStateIsTracking = savedInstanceState.getBoolean(STATE_IS_TRACKING, false);
		}

		// Restore previous UI state
		if (previousStateIsTracking) {
			setEnabledActionButtons(true);
		} else {
			// Disable buttons until user starts tracking
			setEnabledActionButtons(false);
			// Inform user why buttons are disabled
			Toast.makeText(this, R.string.tracklogger_waiting_gps, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onResume() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Try to inflate the buttons layout
		try {
			String userLayout = prefs.getString(
					OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT);
			if (OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT.equals(userLayout)) {
				// Using default buttons layout
				mainLayout = new UserDefinedLayout(this, currentTrackId, null);
			} else {
				// Using user buttons layout
				File layoutFile = new File(
						Environment.getExternalStorageDirectory().getPath()
						+ prefs.getString(
								OSMTracker.Preferences.KEY_STORAGE_DIR,
								OSMTracker.Preferences.VAL_STORAGE_DIR)
						+ File.separator + Preferences.LAYOUTS_SUBDIR
						+ File.separator + userLayout);
				mainLayout = new UserDefinedLayout(this, currentTrackId, layoutFile);
			}
			
			((ViewGroup) findViewById(R.id.tracklogger_root)).removeAllViews();
			((ViewGroup) findViewById(R.id.tracklogger_root)).addView(mainLayout);
			
		} catch (Exception e) {
			Log.e(TAG, "Error while inflating UserDefinedLayout", e);
			Toast.makeText(this, R.string.error_userlayout_parsing, Toast.LENGTH_SHORT).show();
		}
		
		// Check GPS status
		if (checkGPSFlag
				&& prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_CHECKSTARTUP,
						OSMTracker.Preferences.VAL_GPS_CHECKSTARTUP)) {
			checkGPSProvider();
		}

		// Register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(true);

		// Start GPS Logger service
		startService(gpsLoggerServiceIntent);

		// Bind to GPS service.
		// We can't use BIND_AUTO_CREATE here, because when we'll ubound
		// later, we want to keep the service alive in background
		bindService(gpsLoggerServiceIntent, gpsLoggerConnection, 0);
		
		super.onResume();
	}

	private void checkGPSProvider() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			// GPS isn't enabled. Offer user to go enable it
			new AlertDialog.Builder(this)
					.setTitle(R.string.tracklogger_gps_disabled)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(getResources().getString(R.string.tracklogger_gps_disabled_hint))
					.setCancelable(true).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).create().show();
			checkGPSFlag = false;
		}
	}

	@Override
	protected void onPause() {
		
		// Un-register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(false);

		if (gpsLogger != null) {
			if (!gpsLogger.isTracking()) {
				Log.v(TAG, "Service is not tracking, trying to stopService()");
				unbindService(gpsLoggerConnection);
				stopService(gpsLoggerServiceIntent);
			} else {
				// Tell service to notify user of background activity
				sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
				unbindService(gpsLoggerConnection);
			}
		}

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the fact that we are currently tracking or not
		if (gpsLogger != null) {
			outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		}

		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when GPS is disabled
	 */
	public void onGpsDisabled() {
		// GPS disabled. Grey all.
		setEnabledActionButtons(false);
	}

	/**
	 * Called when GPS is enabled
	 */
	public void onGpsEnabled() {
		// Buttons can be enabled
		if (gpsLogger != null && gpsLogger.isTracking()) {
			setEnabledActionButtons(true);
		}
	}

	/**
	 * Enable buttons associated to tracking
	 * 
	 * @param enabled
	 *            true to enable, false to disable
	 */
	public void setEnabledActionButtons(boolean enabled) {
		if (mainLayout != null) {
			mainLayout.setEnabled(enabled);
		}
	}

	// Create options menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklogger_menu, menu);
		return true;
	}

	// Display options menu
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.tracklogger_menu_startstoptracking);

		// Change first item according to current tracking status
		if (gpsLogger != null && gpsLogger.isTracking()) {
			// We're tracking, item = "stop & save"
			item.setIcon(android.R.drawable.ic_menu_save);
			item.setTitle(getResources().getString(R.string.menu_stoptracking));
			item.setTitleCondensed(getResources().getString(R.string.menu_stoptracking));
		} else {
			// We're not tracking, item = "start tracking"
			item.setIcon(android.R.drawable.ic_menu_edit);
			item.setTitle(getResources().getString(R.string.menu_starttracking));
			item.setTitleCondensed(getResources().getString(R.string.menu_starttracking));
			if (gpsLogger == null) {
				item.setEnabled(false);
			} else {
				item.setEnabled(gpsLogger.isGpsEnabled());
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	// Manage options menu selections
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.tracklogger_menu_startstoptracking:
			// Start / Stop tracking	
			if (gpsLogger.isTracking()) {
				Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
				sendBroadcast(intent);
				setEnabledActionButtons(false);
				((GpsStatusRecord) findViewById(R.id.gpsStatus)).manageRecordingIndicator(false);
				finish();
			} else {
				Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
				sendBroadcast(intent);
				setEnabledActionButtons(true);
				((GpsStatusRecord) findViewById(R.id.gpsStatus)).manageRecordingIndicator(true);
			}			
			break;
		case R.id.tracklogger_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		case R.id.tracklogger_menu_waypointlist:
			// Start Waypoint list activity
			i = new Intent(this, WaypointList.class);
			i.putExtra(Schema.COL_TRACK_ID, currentTrackId);
			startActivity(i);
			break;
		case R.id.tracklogger_menu_about:
			// Start About activity
			startActivity(new Intent(this, About.class));
			break;
		case R.id.tracklogger_menu_displaytrack:
			// Start display track activity, with or without OSM background
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
			} else {
				i = new Intent(this, DisplayTrack.class);
			}
			i.putExtra(Schema.COL_TRACK_ID, currentTrackId);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// Manage back button if we are on a sub-page
			if (event.getRepeatCount() == 0) {
				if (mainLayout != null && mainLayout.getStackSize() > 1) {
					mainLayout.pop();
					return true;
				}
			}
			break;
		case KeyEvent.KEYCODE_CAMERA:
			if (gpsLogger.isTracking()) {
				requestStillImage();
				return true;
			}
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Request a still picture from the camera application, saving the file in
	 * the current track directory
	 */
	public void requestStillImage() {
		if (gpsLogger.isTracking()) {
			File imageFile = pushImageFile();
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
			startActivityForResult(cameraIntent, TrackLogger.REQCODE_IMAGE_CAPTURE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(TAG, "Activity result: " + requestCode + ", resultCode=" + resultCode + ", Intent=" + data);
		switch (requestCode) {
		case REQCODE_IMAGE_CAPTURE:
			if (resultCode == RESULT_OK) {
				// A still image has been captured, track the corresponding waypoint
				// Send an intent to inform service to track the waypoint.
				File imageFile = popImageFile();
				if (imageFile != null) {
					Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
					intent.putExtra(Schema.COL_TRACK_ID, currentTrackId);
					intent.putExtra(OSMTracker.INTENT_KEY_NAME, getResources().getString(R.string.wpt_stillimage));
					intent.putExtra(OSMTracker.INTENT_KEY_LINK, imageFile.getName());
					sendBroadcast(intent);
				}			
			}
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Getter for gpsLogger
	 * 
	 * @return Activity {@link GPSLogger}
	 */
	public GPSLogger getGpsLogger() {
		return gpsLogger;
	}

	/**
	 * Setter for gpsLogger
	 * 
	 * @param l
	 *            {@link GPSLogger} to set.
	 */
	public void setGpsLogger(GPSLogger l) {
		this.gpsLogger = l;
	}
	
	/**
	 * Gets a File for storing an image in the current track dir
	 * and stores it in a class variable.
	 * 
	 * @return A File pointing to an image file inside the current track directory
	 */
	public File pushImageFile() {
		currentImageFile = null;

		// Query for current track directory
		File trackDir = DataHelper.getTrackDir(getContentResolver(), currentTrackId);
		currentImageFile = new File(trackDir, DataHelper.FILENAME_FORMATTER.format(new Date()) + DataHelper.EXTENSION_JPG);

		return currentImageFile;
	}
	
	/**
	 * @return The current image file, and clear the internal variable.
	 */
	public File popImageFile() {
		File imageFile = currentImageFile;
		currentImageFile = null;
		return imageFile;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case DIALOG_TEXT_NOTE:
			// create a new TextNoteDialog
			return new TextNoteDialog(this, currentTrackId);
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id){
		case DIALOG_TEXT_NOTE:
			// we need to reset Values like uuid of the dialog,
			// otherwise we would overwrite an existing waypoint
			((TextNoteDialog)dialog).resetValues();
			break;
		}
		super.onPrepareDialog(id, dialog);
	}
	
	

}
