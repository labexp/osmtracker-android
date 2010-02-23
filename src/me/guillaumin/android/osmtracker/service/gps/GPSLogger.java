package me.guillaumin.android.osmtracker.service.gps;

import java.io.IOException;

import me.guillaumin.android.osmtracker.activity.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * GPS logging service. Dialogs with {@link TrackLogger} activity
 * for UI, and with the {@link GPSAndLocationListener} for location.
 * @author nicolas
 *
 */
public class GPSLogger extends Service implements LocationListener {

	private static final String TAG = GPSLogger.class.getSimpleName();

	/**
	 * Data helper.
	 */
	private DataHelper dataHelper = new DataHelper(this);

	/**
	 * Are we currently tracking ?
	 */
	private boolean isTracking = false;
	
	/**
	 * Is GPS enabled ?
	 */
	private boolean isGpsEnabled = false;
	
	/**
	 * Last known location
	 */
	private Location lastLocation;

	/**
	 * Receives Intent for way point tracking, and stop/start logging.
	 */
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (OSMTracker.INTENT_TRACK_WP.equals(intent.getAction())) {
				// Track a way point
				Bundle extras = intent.getExtras();
				if (extras != null) {
					String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					if (link != null) {
						dataHelper.wayPoint(lastLocation, name, link);
					} else {
						dataHelper.wayPoint(lastLocation, name);
					}
				}
			} else if (OSMTracker.INTENT_START_TRACKING.equals(intent.getAction()) ) {
				startTracking();
			} else if (OSMTracker.INTENT_STOP_TRACKING.equals(intent.getAction()) ) {
				stopTrackingAndSave();
			}
		}
	};
	
	/**
	 * Binder for service interaction
	 */
	private final IBinder binder = new GPSLoggerBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		// If we aren't currently tracking we can
		// stop ourself
		if (! isTracking ) {
			Log.v(TAG, "Service self-stopping");
			stopSelf();
		}
		
		return super.onUnbind(intent);
	}

	/**
	 * Bind interface for service interaction
	 */
	public class GPSLoggerBinder extends Binder {

		/**
		 * Called by the activity when binding.
		 * Returns itself, and register the location listener.
		 * @return the GPS Logger service
		 */
		public GPSLogger getService() {
			// Register ourselves for location updates
			LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSLogger.this);
			
			return GPSLogger.this;
		}
	}
	
	@Override
	public void onCreate() {
		Log.v(TAG, "Service creating");
		
		// Register our broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(OSMTracker.INTENT_TRACK_WP);
		filter.addAction(OSMTracker.INTENT_START_TRACKING);
		filter.addAction(OSMTracker.INTENT_STOP_TRACKING);
		registerReceiver(receiver, filter);
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "Service destroying");
		if (isTracking) {
			// If we're currently tracking, save user data.
			stopTrackingAndSave();
		}

		// Unregister listener
		LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr.removeUpdates(this);
		
		// Unregister broadcast receiver
		unregisterReceiver(receiver);

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	private void startTracking() {
		Log.v(TAG, "Starting track logging");

		try {
			dataHelper.createNewTrack();
			isTracking = true;
		} catch (IOException ioe ) {
			// TODO Manage exception, display a Toast to inform user
			throw new RuntimeException("Unmanaged Exception", ioe);
		}
	}

	/**
	 * Stops GPS Logging and save GPX file.
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.exportTrackAsGpx();
	}

	public DataHelper getDataHelper() {
		return dataHelper;
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(TAG, "Location changed: " + location);	
		// We're receiving location, so GPS is enabled
		isGpsEnabled = true;
		
		lastLocation = location;
		if (isTracking) {
			dataHelper.track(location);
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		isGpsEnabled = false;	
	}

	@Override
	public void onProviderEnabled(String provider) {
		isGpsEnabled = true;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Not interested in provider status			
	}

	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}
	
	public boolean isTracking() {
		return isTracking;
	}	

}
