package me.guillaumin.android.osmtracker.service.gps;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.content.Context;
import android.content.ContextWrapper;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.util.Log;

public class GPSAndLocationListener implements Listener, LocationListener {

	private final static String TAG = GPSAndLocationListener.class.getSimpleName();

	/**
	 * Activity for GPS status updates. Set dynamically on binding.
	 */
	private TrackLogger activity;

	/**
	 * Maintains GPS status
	 */
	private GPSStatus gpsStatus = new GPSStatus();

	/**
	 * Database helper
	 */
	private DataHelper dataHelper;

	/**
	 * Last known location
	 */
	private Location lastLocation;
	
	/**
	 * Is it currently tracking ?
	 */
	private boolean tracking = false;

	public GPSAndLocationListener(DataHelper helper) {
		this.dataHelper = helper;
	}
	
	// Provided by GpsStatus.Listener
	@Override
	public void onGpsStatusChanged(int status) {
		Log.v(TAG, "Gps status has changed: " + status);
		// GPS sending status, so it's enabled
		gpsStatus.setEnabled(true);
		gpsStatus.setLastGpsStatus(status);

		updateUI();
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(TAG, "Location changed: " + location);
		lastLocation = location;
		
		// GPS sending location, so it's enabled
		gpsStatus.setEnabled(true);

		if (!gpsStatus.isFirstLocationReceived()) {
			gpsStatus.setFirstLocationReceived(true);
			updateUI();
		}

		// TODO Log location into DB
		if (tracking) {
			dataHelper.track(location);
		}

	}
	
	/**
	 * Tracks a way point.
	 * @param name Name of waypoint.
	 */
	public void trackWayPoint(String name) {
		if (lastLocation != null) {
			dataHelper.wayPoint(lastLocation, name);
		}
	}
	
	/**
	 * Tracks a way point with an associated link.
	 * @param name Name of waypoint
	 * @param link Associated link
	 */
	public void trackWayPoint(String name, String link) {
		if (lastLocation != null) {
			dataHelper.wayPoint(lastLocation, name, link);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " disabled");
		gpsStatus.setEnabled(false);
		gpsStatus.setLastProviderStatus(LocationProvider.OUT_OF_SERVICE);
		gpsStatus.setLastGpsStatus(GpsStatus.GPS_EVENT_STOPPED);
		updateUI();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " enabled");
		gpsStatus.setEnabled(true);
		gpsStatus.setLastProviderStatus(LocationProvider.AVAILABLE);
		updateUI();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, "Location provider " + provider + " status changed to: "
				+ status);
		gpsStatus.setLastProviderStatus(status);
		updateUI();
	}

	/**
	 * Updates UI with GPS Status, if an UI has bound the service.
	 */
	public void updateUI() {
		if (activity != null) {
			activity.updateUIAccordingtoGPS(gpsStatus);
		}
	}

	public GPSStatus getGpsStatus() {
		return gpsStatus;
	}

	public void setActivity(TrackLogger activity) {
		this.activity = activity;
	}

	public void setTracking(boolean tracking) {
		this.tracking = tracking;
	}
	
	public boolean isTracking() {
		return tracking;
	}
}
