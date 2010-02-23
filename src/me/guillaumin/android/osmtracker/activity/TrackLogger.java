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
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

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

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tracklogger);

		// Display main buttons
		LayoutInflater.from(this).inflate(R.layout.tracklogger_main_buttons,
				(ViewGroup) findViewById(R.id.tracklogger_root), true);

		// Disable buttons until user clicks record
		buttonTable = (DisablableTableLayout) findViewById(R.id.tracklogger_tblMain);
		buttonTable.setEnabled(false);

		// Handler for buttons
		listener = new WaypointButtonOnClickListener((ViewGroup) findViewById(R.id.tracklogger_root), this);
		buttonTable.setOnClickListenerForAllChild(listener);
		((Button) findViewById(R.id.tracklogger_btnBack)).setOnClickListener(listener);

		// Inform user why buttons are disabled
		Toast.makeText(this, R.string.tracklogger_waiting_gps, Toast.LENGTH_LONG).show();

		// Register listeners
		((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack))
				.setOnCheckedChangeListener(new ToggleRecordOnCheckedChangeListener(this));
		((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setOnClickListener(new VoiceRecOnClickListener(
				this));

		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	protected void onResume() {
		// Bind to GPS service
		bindService(new Intent(this, GPSLogger.class), gpsLoggerConnection, BIND_AUTO_CREATE);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Ubind GPS service
		unbindService(gpsLoggerConnection);
		super.onPause();
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
