package me.guillaumin.android.osmtracker.service.gps;

import java.io.IOException;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class GPSLogger extends Service {

	private static final String TAG = Service.class.getSimpleName();

	/**
	 * Unique id for notifications.
	 */
	private static int notificationId = 0;

	/**
	 * Database helper.
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
	public void onCreate() {
		Log.v(TAG, "Service is starting");
		// TODO make it compatible for 2.0 !
		// @see
		// http://android-developers.blogspot.com/2010/02/service-api-changes-starting-with.html
		// setForeground(false);
	}

	@Override
	public void onDestroy() {
		if (gpsListener != null) {
			if (gpsListener.isTracking()) {
				// Still tracking. Save data before destroy
				gpsListener.setTracking(false);
				dataHelper.exportTrackAsGpx();
			}

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
