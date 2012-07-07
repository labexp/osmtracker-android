package me.guillaumin.android.osmtracker.service.gps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * GPS logging service.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPSLogger extends Service implements LocationListener,
   SharedPreferences.OnSharedPreferenceChangeListener {

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
	 * NMEA Logger 
	 */
	private NmeaLogger nmeaLogger;
	
	/**
	 * Wake lock that ensures that the CPU is running.
	 */
	private PowerManager.WakeLock wakeLock;

	/**
	 * Current Track ID
	 */
	private long currentTrackId;

	/**
	 * the timestamp of the last GPS fix we used
	 */
	private long lastGPSTimestamp = 0;
	
	/**
	 * the interval (in ms) to log GPS fixes defined in the preferences
	 */
	private long gpsLoggingInterval;

	/**
	 * Is NMEA logging enabled ?
	 */
	private boolean isRawNmeaLogEnabled;

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
					// because of the gps logging interval our last fix could be very old
					// so we'll request the last known location from the gps provider
					lastLocation = lmgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if(lastLocation != null){
						Long trackId = extras.getLong(Schema.COL_TRACK_ID);
						String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
						String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
						String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
						dataHelper.wayPoint(trackId, lastLocation, lastNbSatellites, name, link, uuid);
					}
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

	/**
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences preferences;
	
	/* NMEA Logger */
	public class NmeaLogger {
		public void activate() {};
		public void deactivate() {};
	}
	
	@TargetApi(5)
	public class NmeaV5Logger extends NmeaLogger {

		/** 
		 * NMEA listener
		 */

		private GpsStatus.NmeaListener nmeaListener;

		/** 
		 * NMEA log file
		 */
		File nmeaLogFile;

		/** 
		 * NMEA log file writer
		 */
		private BufferedWriter nmeaLog;

		private boolean isActive = false;

		private BroadcastReceiver mExternalStorageReceiver;

		private boolean mExternalStorageWriteable = false;

		NmeaV5Logger() {
			nmeaListener = new GpsStatus.NmeaListener() {
				public void onNmeaReceived(long timestamp, String nmea) {
					NmeaV5Logger.this.onNmeaReceived(timestamp, nmea);
				}
			};
		}

		private boolean openLog()
		{
			if (!mExternalStorageWriteable)
				return false;

			if (nmeaLog != null)
				return true;

			// Query for current track directory
			File trackDir = DataHelper.getTrackDirectory(currentTrackId);

			// Create the track storage directory if it does not yet exist
			if (!trackDir.exists()) {
				if ( !trackDir.mkdirs() ) {
					Log.w(TAG, "Directory [" + trackDir.getAbsolutePath() + "] does not exist and cannot be created");
					return false;
				}
			}

			// Ensure that this location can be written to 
			if (trackDir.exists() && trackDir.canWrite()) {
				nmeaLogFile = new File(trackDir,
						DataHelper.FILENAME_FORMATTER.format(new Date()) + DataHelper.EXTENSION_NMEA);
			} else {
				Log.w(TAG, "The directory [" + trackDir.getAbsolutePath() + "] will not allow files to be created");
				return false;
			}

			try {
				nmeaLog = new BufferedWriter(new FileWriter(nmeaLogFile, true));
			}catch (IOException e) {
				Log.w(TAG, "Failed to open NMEA log file " + nmeaLogFile  + ": " + e);
			}
			
			if (nmeaLog != null)
				Log.i(TAG, "Opened NMEA log file " + nmeaLogFile.getAbsolutePath());

			return nmeaLog != null;
		}

		private void closeLog()
		{
			if (nmeaLog == null)
				return;

			try {
				nmeaLog.close();
			} catch (IOException e) {
				Log.w(TAG, "closeLogfile() error " + e);
			}
			Log.i(TAG, "Closed NMEA log file " + nmeaLogFile.getAbsolutePath());
			nmeaLog = null;
			nmeaLogFile = null;
		}

		private void onNmeaReceived(long timestamp, String nmea) {
			if (!isTracking)
				return;

			if (!mExternalStorageWriteable)
				return;

			if ((nmeaLog == null)
					&& (openLog() == false))
				return;

			try {
				nmeaLog.write(nmea);
			} catch (IOException e) {
				Log.e(TAG, "nmeaLog.write() error " + e);
				closeLog();
			}
		}


		private void startWatchingExternalStorage()
		{
			mExternalStorageReceiver = new BroadcastReceiver() {
		        @Override
		        public void onReceive(Context context, Intent intent) {
		        	String action = intent.getAction();
		        	if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
		        		Log.i(TAG, "got an EJECT for " + intent.getData() );
		        		closeLog();
		        	}else
		        		Log.i(TAG, "Storage " + intent.getData());
		            updateExternalStorageState();
		        }
		    };
		    IntentFilter filter = new IntentFilter();
		    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		    filter.addAction(Intent.ACTION_MEDIA_EJECT);
		    filter.addDataScheme("file");
		    registerReceiver(mExternalStorageReceiver, filter);
		    updateExternalStorageState();
		}

		private void stopWatchingExternalStorage()
		{
			unregisterReceiver(mExternalStorageReceiver);
		}

		private void updateExternalStorageState()
		{
			String state = Environment.getExternalStorageState();
			mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(state);
		}

		@Override
		public void activate() {
			if (isActive)
				return;
			isActive = lmgr.addNmeaListener(nmeaListener);
			if (isActive)
				startWatchingExternalStorage();
		}

		@Override
		public void deactivate() {
			if (!isActive)
				return;
			stopWatchingExternalStorage();
			lmgr.removeNmeaListener(nmeaListener);
			closeLog();
			isActive = false;
		}
	}

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

		preferences = PreferenceManager.getDefaultSharedPreferences(
				this.getApplicationContext());

		//read the logging interval from preferences
		gpsLoggingInterval = Long.parseLong(preferences.getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;

		// read if raw NMEA log is enabled 
		isRawNmeaLogEnabled = preferences.getBoolean(OSMTracker.Preferences.KEY_GPS_LOG_RAW_NMEA,
				OSMTracker.Preferences.VAL_GPS_LOG_RAW_NMEA);

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

		// Register ourselves for preferences changes
		preferences.registerOnSharedPreferenceChangeListener(this);

		if (android.os.Build.VERSION.SDK_INT >= 5)
			nmeaLogger = new NmeaV5Logger();
		else
			nmeaLogger = new NmeaLogger();

		PowerManager pm = (PowerManager)getSystemService(
                Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OsmtrackerServiceLock");

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

		// Unregister preference change listener
		preferences.unregisterOnSharedPreferenceChangeListener(this);

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

		// Start NMEA logging
		if (isRawNmeaLogEnabled)
			nmeaLogger.activate();

		// Lock CPU power
		wakeLock.acquire();

		notifyBackgroundService();
	}

	/**
	 * Stops GPS Logging
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.stopTracking(currentTrackId);
		nmeaLogger.deactivate();
		wakeLock.release();
		stopNotifyBackgroundService();
		currentTrackId = -1;
		this.stopSelf();
	}

	@Override
	public void onLocationChanged(Location location) {		
		// We're receiving location, so GPS is enabled
		isGpsEnabled = true;
		
		// first of all we check if the time from the last used fix to the current fix is greater than the logging interval
		if((lastGPSTimestamp + gpsLoggingInterval) < System.currentTimeMillis()){
			lastGPSTimestamp = System.currentTimeMillis(); // save the time of this fix
		
			lastLocation = location;
			lastNbSatellites = countSatellites();
			
			if (isTracking) {
				dataHelper.track(currentTrackId, location);
			}
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
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL)) {
			gpsLoggingInterval = Long.parseLong(sharedPreferences.getString(
					OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;
		}else if (key.equals(OSMTracker.Preferences.KEY_GPS_LOG_RAW_NMEA)) {
			isRawNmeaLogEnabled = sharedPreferences.getBoolean(OSMTracker.Preferences.KEY_GPS_LOG_RAW_NMEA,
					OSMTracker.Preferences.VAL_GPS_LOG_RAW_NMEA);

			if (isTracking) {
				if (isRawNmeaLogEnabled)
					nmeaLogger.activate();
				else
					nmeaLogger.deactivate();
			}
		}
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
