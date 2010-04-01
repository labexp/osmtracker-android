package me.guillaumin.android.osmtracker.service.gps;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
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

		if (activity.getGpsLogger().isTracking()) {
			if (activity.getGpsLogger().isGpsEnabled()) {
				activity.setEnabledActionButtons(true);
			}
		} else {
			activity.setEnabledActionButtons(false);
		}
	}

}
