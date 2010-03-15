package me.guillaumin.android.osmtracker.service.gps;

import java.io.IOException;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

/**
 * GPS logging service.
 * 
 * @author Nicolas Guillaumin
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
	 * Should we notify the user that we're working
	 * in background ?
	 */
	private boolean isNotifying = false;

	/**
	 * System notification id.
	 */
	private int notificationId = 0;
	
	/**
	 * Keeps track of time when asked to notify.
	 */
	private long notificationTimer = 0;
	
	/**
	 * Amount of time to wait before starting notifying the user of background activity.
	 */
	private final static int NOTIFICATION_WAIT_TIME_MS = 5000;
	
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
			Log.v(TAG, "Received intent " + intent.getAction());
			
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
			} else if (OSMTracker.INTENT_START_NOTIFY_BACKGROUND.equals(intent.getAction()) ) {
				isNotifying = true;
				notificationTimer = SystemClock.elapsedRealtime();
			} else if (OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND.equals(intent.getAction()) ) {
				isNotifying = false;
			} else if (OSMTracker.INTENT_NOTIFICATION_CLEARED.equals(intent.getAction()) ) {
				// User has cleared all the notification. Increments notification Id
				// in order to launch new notification next time
				notificationId++;
			}
		}
	};
	
	/**
	 * Binder for service interaction
	 */
	private final IBinder binder = new GPSLoggerBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind()");
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnBind()");
		// If we aren't currently tracking we can
		// stop ourself
		if (! isTracking ) {
			Log.v(TAG, "Service self-stopping");
			stopSelf();
		}
		
		// We don't want onRebind() to be called, so return false.
		return false;
	}

	/**
	 * Bind interface for service interaction
	 */
	public class GPSLoggerBinder extends Binder {

		/**
		 * Called by the activity when binding.
		 * Returns itself.
		 * @return the GPS Logger service
		 */
		public GPSLogger getService() {			
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
		filter.addAction(OSMTracker.INTENT_NOTIFICATION_CLEARED);
		filter.addAction(OSMTracker.INTENT_START_NOTIFY_BACKGROUND);
		filter.addAction(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND);
		registerReceiver(receiver, filter);

		// Register ourselves for location updates
		LocationManager lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSLogger.this);

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
			// Should not occur, as we check external storage
			// state before starting tracking.
			Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_externalstorage_not_writable), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Stops GPS Logging and save GPX file.
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.exportTrackAsGpx();
	}

	/**
	 * Getter for dataHelper
	 * @return the {@link DataHelper}
	 */
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
			if (isNotifying) {
				// Notify user that we're in background
				notifyBackgroundService();
			}
		}
		
	}

	/**
	 * Notifies the user that we're still tracking in background.
	 */
	private void notifyBackgroundService() {
		Log.v(TAG, "SystemClock is: " + SystemClock.elapsedRealtime() + ", timer is: " + notificationTimer);
		if (SystemClock.elapsedRealtime() - notificationTimer > NOTIFICATION_WAIT_TIME_MS) {
			NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification n = new Notification(R.drawable.icon_greyed_25x25, getResources().getString(R.string.notification_ticker_text), System.currentTimeMillis());
			
			Intent startTrackLogger = new Intent(this, TrackLogger.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, startTrackLogger, 0);
			PendingIntent deleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(OSMTracker.INTENT_NOTIFICATION_CLEARED), 0);
			n.deleteIntent = deleteIntent;
			n.flags = Notification.FLAG_AUTO_CANCEL;
			n.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.notification_title), getResources().getString(R.string.notification_text), contentIntent);
			
			nmgr.notify(notificationId, n);
		}
		
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		isGpsEnabled = false;
		if (isNotifying) {
			notifyBackgroundService();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		isGpsEnabled = true;
		if (isNotifying) {
			notifyBackgroundService();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Not interested in provider status			
	}

	/**
	 * Getter for gpsEnabled
	 * @return true if GPS is enabled, otherwise false.
	 */
	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}
	
	/**
	 * Setter for isTracking
	 * @return true if we're currently tracking, otherwise false.
	 */
	public boolean isTracking() {
		return isTracking;
	}

}
