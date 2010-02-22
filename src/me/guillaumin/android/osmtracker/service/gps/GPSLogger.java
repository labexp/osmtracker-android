package me.guillaumin.android.osmtracker.service.gps;

import java.io.IOException;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * GPS logging service. Dialogs with {@link TrackLogger} activity
 * for UI, and with the {@link GPSAndLocationListener} for location.
 * @author nicolas
 *
 */
public class GPSLogger extends Service {

	private static final String TAG = Service.class.getSimpleName();

	/**
	 * Data helper.
	 */
	private DataHelper dataHelper = new DataHelper(this);

	/**
	 * GPS Listener
	 */
	private GPSAndLocationListener gpsListener;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		gpsListener.setActivity(null);
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
			gpsListener = new GPSAndLocationListener(dataHelper);
			gpsListener.setActivity(a);

			LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lmgr.addGpsStatusListener(gpsListener);
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);

			gpsListener.updateUI();
			return GPSLogger.this;
		}
	}

	@Override
	public void onDestroy() {
		if (gpsListener != null) {
			if (gpsListener.isTracking()) {
				// Still tracking. Save data before destroy
				gpsListener.setTracking(false);
				dataHelper.exportTrackAsGpx();
			}

			// Unregister listener
			LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lmgr.removeGpsStatusListener(gpsListener);
			lmgr.removeUpdates(gpsListener);
		}

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	public void startTracking() throws IOException {
		Log.v(TAG, "Starting track logging");

		dataHelper.createNewTrack();
		gpsListener.setTracking(true);

	}

	/**
	 * Stops GPS Logging and save GPX file.
	 */
	public void stopTracking() {
		gpsListener.setTracking(false);
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
		gpsListener.trackWayPoint(name);
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
		gpsListener.trackWayPoint(name, link);
	}

	public GPSAndLocationListener getGpsListener() {
		return gpsListener;
	}

	public DataHelper getDataHelper() {
		return dataHelper;
	}

}
