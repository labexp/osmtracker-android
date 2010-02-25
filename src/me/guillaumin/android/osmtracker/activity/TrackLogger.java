package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.layout.DisablableTableLayout;
import me.guillaumin.android.osmtracker.listener.ToggleRecordOnCheckedChangeListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
import me.guillaumin.android.osmtracker.listener.WaypointButtonOnClickListener;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracklogger);

		// Try to restore previous state
		boolean previousStateIsTracking = false;
		int previousStateButtonTableId = R.layout.tracklogger_main_buttons;
		if (savedInstanceState != null) {
			Log.d(TAG, "Restoring previous state: " + savedInstanceState);
			previousStateIsTracking = savedInstanceState.getBoolean(STATE_IS_TRACKING, false);
			previousStateButtonTableId = savedInstanceState
					.getInt(STATE_BUTTON_PAGE, R.layout.tracklogger_main_buttons);
		}

		// Display main buttons
		buttonTable = (DisablableTableLayout) LayoutInflater.from(this).inflate(previousStateButtonTableId,
				(ViewGroup) findViewById(R.id.tracklogger_root), false);
		((ViewGroup) findViewById(R.id.tracklogger_root)).addView(buttonTable);

		// Handler for buttons
		listener = new WaypointButtonOnClickListener((ViewGroup) findViewById(R.id.tracklogger_root), this);
		buttonTable.setOnClickListenerForAllChild(listener);
		((Button) findViewById(R.id.tracklogger_btnBack)).setOnClickListener(listener);

		// Register listeners
		trackToggle = ((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack));
		trackToggle.setOnCheckedChangeListener(new ToggleRecordOnCheckedChangeListener(this));
		((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setOnClickListener(new VoiceRecOnClickListener(
				this));

		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

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
		// Save the current displayed button page
		outState.putInt(STATE_BUTTON_PAGE, buttonTable.getId());
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
		}
		return super.onOptionsItemSelected(item);
	}

	public GPSLogger getGpsLogger() {
		return gpsLogger;
	}

	public void setButtonTable(DisablableTableLayout buttonTable) {
		this.buttonTable = buttonTable;
	}

}
