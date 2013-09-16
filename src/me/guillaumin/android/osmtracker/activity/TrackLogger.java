package me.guillaumin.android.osmtracker.activity;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.layout.GpsStatusRecord;
import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import me.guillaumin.android.osmtracker.service.gps.GPSLoggerServiceConnection;
import me.guillaumin.android.osmtracker.util.FileSystemUtils;
import me.guillaumin.android.osmtracker.util.ThemeValidator;
import me.guillaumin.android.osmtracker.view.TextNoteDialog;
import me.guillaumin.android.osmtracker.view.VoiceRecDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
	 * Request code for callback after the gallery was chosen by the user.
	 */
	private static final int REQCODE_GALLERY_CHOSEN = 1;

	/**
	 * Bundle state key for tracking flag.
	 */
	public static final String STATE_IS_TRACKING = "isTracking";
	
	/**
	 * Bundle state key button state.
	 */
	public static final String STATE_BUTTONS_ENABLED = "buttonsEnabled";

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
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences prefs = null;
	
	/**
	 * keeps track of current button status
	 */
	private boolean buttonsEnabled = false;
	
	/**
	 * constant for text note dialog
	 */
	public static final int DIALOG_TEXT_NOTE = 1;
	/**
	 * constant for voice recording dialog
	 */
	public static final int DIALOG_VOICE_RECORDING = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Get the track id to work with
		currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		Log.v(TAG, "Starting for track id " + currentTrackId);
			
		gpsLoggerServiceIntent = new Intent(this, GPSLogger.class);
		gpsLoggerServiceIntent.putExtra(Schema.COL_TRACK_ID, currentTrackId);

		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// get shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Set application theme according to user settings
		setTheme(getResources().getIdentifier(ThemeValidator.getValidTheme(prefs, getResources()), null, null));

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tracklogger);
		
		// set trackLogger to keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));

		// we'll restore previous button state, GPSStatusRecord will enable all buttons, as soon as there's a gps fix
		if(savedInstanceState != null){
			buttonsEnabled = savedInstanceState.getBoolean(STATE_BUTTONS_ENABLED, false);
		}
	}

	@Override
	protected void onResume() {

		setTitle(getResources().getString(R.string.tracklogger) + ": #" + currentTrackId);
		
		// set trackLogger to  keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
	
		// Fix to the user's preferred orientation (if any)  
		String preferredOrientation = prefs.getString(OSMTracker.Preferences.KEY_UI_ORIENTATION, 
				OSMTracker.Preferences.VAL_UI_ORIENTATION);
		if (preferredOrientation.equals(OSMTracker.Preferences.VAL_UI_ORIENTATION_PORTRAIT)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (preferredOrientation.equals(OSMTracker.Preferences.VAL_UI_ORIENTATION_LANDSCAPE)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
		
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
						Environment.getExternalStorageDirectory(),
						prefs.getString(
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

		setEnabledActionButtons(buttonsEnabled);
		if(!buttonsEnabled){
			Toast.makeText(this, R.string.tracklogger_waiting_gps, Toast.LENGTH_LONG).show();
		}

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
				unbindService(gpsLoggerConnection);
			}
		}

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the fact that we are currently tracking or not
		if(gpsLogger != null){
			outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		}
		outState.putBoolean(STATE_BUTTONS_ENABLED, buttonsEnabled);

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
	 * @param enabled true to enable, false to disable
	 */
	public void setEnabledActionButtons(boolean enabled) {
		if (mainLayout != null) {
			buttonsEnabled = enabled;
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

	// Manage options menu selections
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.tracklogger_menu_stoptracking:
			// Start / Stop tracking	
			if (gpsLogger.isTracking()) {
				Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
				sendBroadcast(intent);
				((GpsStatusRecord) findViewById(R.id.gpsStatus)).manageRecordingIndicator(false);
				finish();
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
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// API Level 3 doesn't support long presses, so we need to do this manually
			if((gpsLogger != null && gpsLogger.isTracking()) && (event.getEventTime() - event.getDownTime()) > OSMTracker.LONG_PRESS_TIME){
				// new long press of dpad center detected, start voice recording dialog
				this.showDialog(DIALOG_VOICE_RECORDING);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_HEADSETHOOK:
			if (gpsLogger != null && gpsLogger.isTracking()){
				this.showDialog(DIALOG_VOICE_RECORDING);
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Request a still picture from the camera application, saving the file in
	 * the current track directory
	 */
	public void requestStillImage() {
		if (gpsLogger.isTracking()) {
			final File imageFile = pushImageFile();
			if (null != imageFile) {

				
				final String pictureSource = prefs.getString(OSMTracker.Preferences.KEY_UI_PICTURE_SOURCE, 
						OSMTracker.Preferences.VAL_UI_PICTURE_SOURCE);
				if (OSMTracker.Preferences.VAL_UI_PICTURE_SOURCE_CAMERA.equals(pictureSource)) {
					startCamera(imageFile);
				} else if (OSMTracker.Preferences.VAL_UI_PICTURE_SOURCE_GALLERY.equals(pictureSource)) {
					startGallery();
				} else {
					// Let the user choose between using the camera
					// or selecting a picture from the gallery

					AlertDialog.Builder getImageFrom = new AlertDialog.Builder(TrackLogger.this);
					getImageFrom.setTitle("Select:");
					final CharSequence[] opsChars = { getString(R.string.tracklogger_camera), getString(R.string.tracklogger_gallery) };
					getImageFrom.setItems(opsChars, new android.content.DialogInterface.OnClickListener() {
	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								startCamera(imageFile);
							} else if (which == 1) {
								startGallery();
							}
							dialog.dismiss();
						}
					});
	
					getImageFrom.show();
				}
			} else {
				Toast.makeText(getBaseContext(), 
						getResources().getString(R.string.error_externalstorage_not_writable),
						Toast.LENGTH_SHORT).show();
			}
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
		case REQCODE_GALLERY_CHOSEN:
			if (resultCode == RESULT_OK) {
				// An image has been selected from the gallery, track the corresponding waypoint
				File imageFile = popImageFile();
				if (imageFile != null) {
					// Copy the file from the gallery
					Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
					c.moveToFirst();
					String f = c.getString(c.getColumnIndex(ImageColumns.DATA));
					c.close();
					Log.d(TAG, "Copying gallery file '"+f+"' into '"+imageFile+"'");
					FileSystemUtils.copyFile(imageFile.getParentFile(), new File(f), imageFile.getName());
					
					// Send an intent to inform service to track the waypoint.
					Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
					intent.putExtra(Schema.COL_TRACK_ID, currentTrackId);
					intent.putExtra(OSMTracker.INTENT_KEY_NAME, getResources().getString(R.string.wpt_stillimage));
					intent.putExtra(OSMTracker.INTENT_KEY_LINK, imageFile.getName());
					sendBroadcast(intent);
				}
			}
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
	 *				{@link GPSLogger} to set.
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
		File trackDir = DataHelper.getTrackDirectory(currentTrackId);

		// Create the track storage directory if it does not yet exist
		if (!trackDir.exists()) {
			if ( !trackDir.mkdirs() ) {
				Log.w(TAG, "Directory [" + trackDir.getAbsolutePath() + "] does not exist and cannot be created");
			}
		}

		// Ensure that this location can be written to 
		if (trackDir.exists() && trackDir.canWrite()) {
			currentImageFile = new File(trackDir, 
					DataHelper.FILENAME_FORMATTER.format(new Date()) + DataHelper.EXTENSION_JPG);			
		} else {
			Log.w(TAG, "The directory [" + trackDir.getAbsolutePath() + "] will not allow files to be created");
		}
		
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
		case DIALOG_VOICE_RECORDING:
			// create a new VoiceRegDialog
			return new VoiceRecDialog(this, currentTrackId);
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
	
	@Override
	protected void onNewIntent(Intent newIntent) {
		if (newIntent.getExtras() != null && newIntent.getExtras().containsKey(Schema.COL_TRACK_ID)) {
			currentTrackId = newIntent.getExtras().getLong(Schema.COL_TRACK_ID);
			setIntent(newIntent);
		}
		super.onNewIntent(newIntent);
	}

	public long getCurrentTrackId() {
		return this.currentTrackId;
	}
	
	/**
	 * Starts the camera app. to take a picture
	 * @param imageFile File to save the picture to
	 */
	private void startCamera(File imageFile) {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
		startActivityForResult(cameraIntent, REQCODE_IMAGE_CAPTURE);
	}
	
	/**
	 * Starts the gallery app. to choose a picture
	 */
	private void startGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, getString(R.string.tracklogger_choose_gallery_camera)), REQCODE_GALLERY_CHOSEN);
	}
	

}
