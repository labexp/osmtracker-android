package me.guillaumin.android.osmtracker.service.gps;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

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
	private DataHelper dataHelper;

	/**
	 * Are we currently tracking ?
	 */
	private boolean isTracking = false;
	
	/**
	 * Is GPS enabled ?
	 */
	private boolean isGpsEnabled = false;
	
	/**
	 * System notification id.
	 */
	private static final int NOTIFICATION_ID = 0;
	
	/**
	 * Last known location
	 */
	private Location lastLocation;
	
	/**
	 * Last number of satellites used in fix.
	 */
	private int lastNbSatellites;
	
	/**
	 * LocationManager
	 */
	private LocationManager lmgr;
	
	/**
	 * Current Track ID
	 */
	private long currentTrackId;

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
					Long trackId = extras.getLong(Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					dataHelper.wayPoint(trackId, lastLocation, lastNbSatellites, name, link, uuid);
				}
			} else if (OSMTracker.INTENT_UPDATE_WP.equals(intent.getAction())) {
				// Update an existing waypoint
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					dataHelper.updateWayPoint(trackId, uuid, name, link);
				}
			} else if (OSMTracker.INTENT_DELETE_WP.equals(intent.getAction())) {
				// Delete an existing waypoint
				Bundle extras = intent.getExtras();
				if (extras != null) {
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					dataHelper.deleteWayPoint(uuid);
				}
			} else if (OSMTracker.INTENT_START_TRACKING.equals(intent.getAction()) ) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(Schema.COL_TRACK_ID);
					startTracking(trackId);
				}
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
		// stop ourselves
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
		dataHelper = new DataHelper(this);
		
		// Register our broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(OSMTracker.INTENT_TRACK_WP);
		filter.addAction(OSMTracker.INTENT_UPDATE_WP);
		filter.addAction(OSMTracker.INTENT_DELETE_WP);
		filter.addAction(OSMTracker.INTENT_START_TRACKING);
		filter.addAction(OSMTracker.INTENT_STOP_TRACKING);
		registerReceiver(receiver, filter);

		// Register ourselves for location updates
		lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		if (isTracking) {
			// If we're currently tracking, save user data.
			stopTrackingAndSave();
		}

		// Unregister listener
		lmgr.removeUpdates(this);
		
		// Unregister broadcast receiver
		unregisterReceiver(receiver);
		
		// Cancel any existing notification
		stopNotifyBackgroundService();

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	private void startTracking(long trackId) {
		currentTrackId = trackId;
		Log.v(TAG, "Starting track logging for track #" + trackId);
		isTracking = true;
		notifyBackgroundService();
	}

	/**
	 * Stops GPS Logging
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.stopTracking(currentTrackId);
		stopNotifyBackgroundService();
		currentTrackId = -1;
	}

	@Override
	public void onLocationChanged(Location location) {		
		// We're receiving location, so GPS is enabled
		isGpsEnabled = true;
				
		lastLocation = location;
		lastNbSatellites = countSatellites();
		
		if (isTracking) {
			dataHelper.track(currentTrackId, location);
		}
		
	}

	/**
	 * Counts number of satellites used in last fix.
	 * @return The number of satellites
	 */
	private int countSatellites() {
		int count = 0;
		GpsStatus status = lmgr.getGpsStatus(null);
		for(GpsSatellite sat:status.getSatellites()) {
			if (sat.usedInFix()) {
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Notifies the user that we're still tracking in background.
	 */
	private void notifyBackgroundService() {
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.icon_greyed_25x25, getResources().getString(R.string.notification_ticker_text), System.currentTimeMillis());
			
		Intent startTrackLogger = new Intent(this, TrackLogger.class);
		startTrackLogger.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, startTrackLogger, PendingIntent.FLAG_UPDATE_CURRENT);
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		n.setLatestEventInfo(
				getApplicationContext(),
				getResources().getString(R.string.notification_title).replace("{0}", Long.toString(currentTrackId)),
				getResources().getString(R.string.notification_text),
				contentIntent);
			
		nmgr.notify(NOTIFICATION_ID, n);
	}
	
	/**
	 * Stops notifying the user that we're tracking in the background
	 */
	private void stopNotifyBackgroundService() {
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nmgr.cancel(NOTIFICATION_ID);
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
