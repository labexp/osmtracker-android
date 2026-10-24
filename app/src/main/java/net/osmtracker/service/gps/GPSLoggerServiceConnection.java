package net.osmtracker.service.gps;

import net.osmtracker.OSMTracker;
import net.osmtracker.service.gps.GPSLoggerConnectionListener;
import net.osmtracker.db.TrackContentProvider;

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
	 * Reference to client activity
	 */
	private GPSLoggerConnectionListener activity;
	
	public GPSLoggerServiceConnection(GPSLoggerConnectionListener tl) {
		activity = tl;
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
		activity.setGpsLogger(null);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		GPSLogger gpsLogger = ((GPSLogger.GPSLoggerBinder) service).getService();
		activity.setGpsLogger(gpsLogger);

		// If not already tracking, start tracking
		if (!gpsLogger.isTracking()) {
			Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
			intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, activity.getCurrentTrackId());
			intent.setPackage(activity.getPackageName());
			activity.sendBroadcast(intent);
		}
	}

}
