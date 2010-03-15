package me.guillaumin.android.osmtracker.service.gps;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.ToggleButton;
/**
 * Handles the bind to the GPS Logger service
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPSLoggerServiceConnection implements ServiceConnection {

	/**
	 * Reference to TrackLogger activity
	 */
	private TrackLogger activity;
	
	public GPSLoggerServiceConnection(TrackLogger tl) {
		activity = tl;
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
		activity.setGpsLogger(null);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		
		activity.setGpsLogger( ((GPSLogger.GPSLoggerBinder) service).getService());

		// Prevent service from notifying user
		activity.sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));

		ToggleButton trackToggle = ((ToggleButton) activity.findViewById(R.id.gpsstatus_record_toggleTrack));
		
		// Restore UI state according to tracking state
		if (activity.getGpsLogger().isTracking()) {
			trackToggle.setEnabled(true);
			trackToggle.setChecked(true);
			if (activity.getGpsLogger().isGpsEnabled()) {
				activity.setEnabledActionButtons(true);
			}

		} else {
			activity.setEnabledActionButtons(false);
			trackToggle.setChecked(false);
			// We don't manage the enabled state of the toggle here
			// as it must be set according to GPS status, and not
			// tracking status
		}
	}

}
