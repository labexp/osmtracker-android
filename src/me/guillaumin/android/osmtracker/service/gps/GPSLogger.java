package me.guillaumin.android.osmtracker.service.gps;

import java.io.IOException;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

	private static final String TAG = Service.class.getSimpleName();

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

	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	/**
	 * Binder for service interaction
	 */
	private final IBinder binder = new GPSLoggerBinder();

	/**
	 * Bind interface for service interaction
	 */
	public class GPSLoggerBinder extends Binder {

		/**
		 * Called by the activity when binding.
		 * Returns itself, and register the location listener.
		 * @param a The TrackLogger activity, for UI updates.
		 * @return the GPS Logger service
		 */
		public GPSLogger getService(TrackLogger a) {
			// Register ourselves for location updates
			LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSLogger.this);
			
			return GPSLogger.this;
		}
	}

	@Override
	public void onDestroy() {
		if (isTracking) {
			isTracking = false;
			dataHelper.exportTrackAsGpx();
		}

		// Unregister listener
		LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr.removeUpdates(this);

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	public void startTracking() throws IOException {
		Log.v(TAG, "Starting track logging");

		dataHelper.createNewTrack();
		isTracking = true;
	}

	/**
	 * Stops GPS Logging and save GPX file.
	 */
	public void stopTracking() {
		isTracking = false;
		dataHelper.exportTrackAsGpx();
	}

	/**
	 * Track a way point.
	 * 
	 * @param name
	 *            Name of waypoint.
	 */
	public void trackWayPoint(String name) {
		Log.v(TAG, "Tracking waypoint with name: " + name);
		dataHelper.wayPoint(lastLocation, name);
	}

	/**
	 * Track a way point with an associated link.
	 * 
	 * @param name
	 *            Name of waypoint.
	 * @param link
	 *            Associated link
	 */
	public void trackWayPoint(String name, String link) {
		Log.v(TAG, "Tracking waypoint with name: " + name + ", link: " + link);
		dataHelper.wayPoint(lastLocation, name, link);
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
