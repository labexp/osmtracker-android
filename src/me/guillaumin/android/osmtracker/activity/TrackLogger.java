package me.guillaumin.android.osmtracker.activity;

import java.io.File;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.DisablableTableLayout;
import me.guillaumin.android.osmtracker.listener.StillImageOnClickListener;
import me.guillaumin.android.osmtracker.listener.ToggleRecordOnCheckedChangeListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
import me.guillaumin.android.osmtracker.listener.WaypointButtonOnClickListener;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

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
	private static final String STATE_IS_TRACKING = "isTracking";

	/**
	 * Bundle state key for current displayed button page.
	 */
	private static final String STATE_BUTTON_PAGE = "buttonPage";

	/**
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

	/**
	 * Toggle for start/stop tracking
	 */
	ToggleButton trackToggle = null;

	/**
	 * View handling the button grid.
	 */
	private DisablableTableLayout buttonTable;

	/**
	 * Listener managing the waypoint buttons.
	 */
	WaypointButtonOnClickListener listener;

	/**
	 * Handles the bind to the GPS Logger service
	 */
	private ServiceConnection gpsLoggerConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			gpsLogger = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsLogger = ((GPSLogger.GPSLoggerBinder) service).getService();

			// Restore UI state according to tracking state
			if (gpsLogger.isTracking()) {
				trackToggle.setEnabled(true);
				trackToggle.setChecked(true);
				if (gpsLogger.isGpsEnabled()) {
					setEnabledActionButtons(true);
				}

			} else {
				setEnabledActionButtons(false);
				trackToggle.setChecked(false);
				// We don't manage the enabled state of the toggle here
				// as it must be set according to GPS status, and not
				// tracking status
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
				OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
		setTheme(getResources().getIdentifier(theme, null, null));
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracklogger);
		
		// Try to restore previous state
		boolean previousStateIsTracking = false;
		if (savedInstanceState != null) {
			Log.d(TAG, "Restoring previous state: " + savedInstanceState);
			previousStateIsTracking = savedInstanceState.getBoolean(STATE_IS_TRACKING, false);
		}

		// Display main buttons
		buttonTable = (DisablableTableLayout) LayoutInflater.from(this).inflate(R.layout.tracklogger_main_buttons,
				(ViewGroup) findViewById(R.id.tracklogger_root), false);
		((ViewGroup) findViewById(R.id.tracklogger_root)).addView(buttonTable);

		// Handler for buttons
		listener = new WaypointButtonOnClickListener((ViewGroup) findViewById(R.id.tracklogger_root), this);
		buttonTable.setOnClickListenerForAllChild(listener);

		// Register listeners
		trackToggle = ((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack));
		trackToggle.setOnCheckedChangeListener(new ToggleRecordOnCheckedChangeListener(this));
		((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setOnClickListener(new VoiceRecOnClickListener(
				this));
		((Button) findViewById(R.id.gpsstatus_record_btnStillImage)).setOnClickListener(new StillImageOnClickListener(
				this));

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

		// The user could come from the settings screen and change the Legacy
		// Back button option.
		boolean useLegacyBackButton = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				OSMTracker.Preferences.KEY_UI_LEGACYBACK, OSMTracker.Preferences.VAL_UI_LEGACYBACK);
		Button backButton = (Button) findViewById(R.id.tracklogger_btnBack);
		if (useLegacyBackButton) {
			if (backButton == null) {
				// Add soft button "back" to the upper bar
				LinearLayout upperLayout = (LinearLayout) findViewById(R.id.tracklogger_upperLayout);
				backButton = (Button) LayoutInflater.from(this).inflate(R.layout.tracklogger_back_button, upperLayout,
						false);
				upperLayout.addView(backButton);
				backButton.setOnClickListener(listener);
				listener.setBackButton(backButton);

				// Enable button if we're on a subpage
				if (buttonTable != null && R.id.tracklogger_tblMain != buttonTable.getId()) {
					backButton.setEnabled(true);
				}
			}
		} else {
			// Be sure to remove button if present
			if (backButton != null) {
				LinearLayout upperLayout = (LinearLayout) findViewById(R.id.tracklogger_upperLayout);
				upperLayout.removeView(backButton);
				backButton.setOnClickListener(null);
				listener.setBackButton(null);
			}
		}

		// Start GPS Logger service
		startService(new Intent(this, GPSLogger.class));

		// Bind to GPS service.
		// We can't use BIND_AUTO_CREATE here, because when we'll ubound
		// later, we want to keep the service alive in background
		bindService(new Intent(this, GPSLogger.class), gpsLoggerConnection, 0);
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "Activity pausing");
		// Ubind GPS service
		unbindService(gpsLoggerConnection);
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "Saving instance state");
		// Save the fact that we are currently tracking or not
		outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when GPS is disabled
	 */
	public void onGpsDisabled() {
		// GPS disabled. Grey all.
		setEnabledActionButtons(false);

		// If we are currently tracking, don't grey the track toggle,
		// allowing the user to stop tracking
		ToggleButton toggle = ((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack));
		if (!toggle.isChecked()) {
			toggle.setEnabled(false);
		}
	}

	/**
	 * Called when GPS is enabled
	 */
	public void onGpsEnabled() {
		// Buttons can be enabled
		ToggleButton toggle = ((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack));
		toggle.setEnabled(true);

		if (toggle.isChecked()) {
			// Currently tracking, activate buttons
			setEnabledActionButtons(true);
		}

	}

	/**
	 * Enable buttons associated to tracking
	 */
	public void setEnabledActionButtons(boolean enabled) {
		buttonTable.setEnabled(enabled);
		((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setEnabled(enabled);
		((Button) findViewById(R.id.gpsstatus_record_btnStillImage)).setEnabled(enabled);
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
		switch (item.getItemId()) {
		case R.id.tracklogger_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		case R.id.tracklogger_menu_waypointlist:
			// Start Waypoint list activity
			startActivity(new Intent(this, WaypointList.class));
			break;
		case R.id.tracklogger_menu_about:
			// Start About activity
			startActivity(new Intent(this, About.class));
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
				// Check if user is using legacy soft button or not
				boolean useLegacyBackButton = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
						OSMTracker.Preferences.KEY_UI_LEGACYBACK, OSMTracker.Preferences.VAL_UI_LEGACYBACK);
				if (!useLegacyBackButton) {
					// User is not using legacy back button, so we override the
					// default device
					// backbutton behaviour.
					if (buttonTable != null && R.id.tracklogger_tblMain != buttonTable.getId()) {
						listener.changeButtons(R.layout.tracklogger_main_buttons, false);
						return true;
					}
				}
			}
			break;
		case KeyEvent.KEYCODE_CAMERA:
			if (gpsLogger.isTracking()) {
				requestStillImage();
				return true;
			} // else standard behavior
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
			File imageFile = gpsLogger.getDataHelper().pushImageFile();
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
				File imageFile = gpsLogger.getDataHelper().popImageFile();
				Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
				intent.putExtra(OSMTracker.INTENT_KEY_NAME, getResources().getString(R.string.wpt_stillimage));
				intent.putExtra(OSMTracker.INTENT_KEY_LINK, imageFile.getName());
				sendBroadcast(intent);
				break;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public GPSLogger getGpsLogger() {
		return gpsLogger;
	}

	public void setButtonTable(DisablableTableLayout buttonTable) {
		this.buttonTable = buttonTable;
	}

}
