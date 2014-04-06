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
import android.preference.PreferenceManager;
import android.util.Log;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;

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
	private static final int NOTIFICATION_ID = 1;
	
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
	private long currentTrackId = -1;

	/**
	 * the timestamp of the last GPS fix we used
	 */
	private long lastGPSTimestamp = 0;
	
	/**
	 * the interval (in ms) to log GPS fixes defined in the preferences
	 */
	private long gpsLoggingInterval;
	
	/**
	 * sensors for magnetic orientation
	 */
	private static SensorManager sensorService;
	private float[] gravity = null;
	private float[] geomag = null;
	private float[] inR = new float[9];
	private float[] outR = new float[9];
	private float[] I = new float[9];
	private float[] orientVals = new float[3];
	private float azimuth, pitch, roll;
	static float rad2deg = 180.0f/3.141592653589793f;
	
	private SensorEventListener sensorListener = new SensorEventListener() {
		 @Override
		 public void onAccuracyChanged(Sensor arg0, int arg1) {}

		 @Override
		 public void onSensorChanged(SensorEvent event) {
//			 Log.v("GPSLogger","got sensor event! sensor: " + event.sensor.getType() + 
//					 ", accuracy: " + event.accuracy + 
//					 ",value: "+ event.values[0] + ", " + event.values[1] + ", " + event.values[2] );
		   // If the sensor data is unreliable return
			 // in fact I find that accuracy is always UNRELIABLE. So I skip this here
//		   if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
//			   //Log.w("GPSLogger", "sensor reliability " + event.accuracy);.
//			   return;
//		   }
//			 Log.v("GPSLogger", "sensor reliable");
		   // Gets the value of the sensor that has been changed
		   switch (event.sensor.getType()){  
		   case Sensor.TYPE_ACCELEROMETER:
			   
			   gravity = event.values.clone();
			   //Log.v("GPSLogger","gravity update " + gravity);
			   break;
		   case Sensor.TYPE_MAGNETIC_FIELD:
			   geomag = event.values.clone();
			   //Log.v("GPSLogger","geomag update " + geomag);
			   break;
		   }

		   // If gravity and geomag have values then find rotation matrix
		   if (gravity != null && geomag != null){

			   // checks that the rotation matrix is found
			   boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
			   if (success){

				    // Re-map coordinates so y-axis comes out of camera
				    SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, 
				    SensorManager.AXIS_Z, outR);

				    // Finds the Azimuth and Pitch angles of the y-axis with 
				    // magnetic north and the horizon respectively
					SensorManager.getOrientation(outR, orientVals);
					azimuth = orientVals[0]*rad2deg;
					pitch = orientVals[1]*rad2deg;
					roll = orientVals[2]*rad2deg;
					Log.v("GPSLogger","new azimuth: "+azimuth+", pitch: "+pitch+", roll: "+roll);

//		     // Displays a pop up message with the azimuth and inclination angles
//		     String endl = System.getProperty("line.separator");
//		     Toast.makeText(getBaseContext(), 
//		       "Rotation:" +
//		       outR[0] + " " + outR[1] + " " + outR[2] + endl +
//		       outR[4] + " " + outR[5] + " " + outR[6] + endl +
//		       outR[8] + " " + outR[9] + " " + outR[10] + endl +endl +
//		       "Azimuth: " + azimuth + " degrees" + endl + 
//		       "Pitch: " + pitch + " degrees" + endl +
//		       "Roll: " + roll + " degrees", 
//		       Toast.LENGTH_LONG).show();
//		    } /*else
//		     Toast.makeText(getBaseContext(), 
//		       "Get Rotation Matrix Failed", Toast.LENGTH_LONG).show();*/
			   }   
		   }
		
		 }
	};
	
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
						
						// get orientation
						float a;
						if (geomag == null && gravity == null) {
							a = 360; //360 signals invalid angle
						} else {
							a = azimuth;
						}
						dataHelper.wayPoint(trackId, lastLocation, lastNbSatellites, name, link, uuid, a);
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

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Service onBind()");
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "Service onUnbind()");
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
		Log.v(TAG, "Service onCreate()");
		dataHelper = new DataHelper(this);

		//read the logging interval from preferences
		gpsLoggingInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;
		
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
		
		//register for Orientation updates
	    sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    
	    Sensor accelSens = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    Sensor magSens = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    
	    if (accelSens != null && magSens != null) {
		    sensorService.registerListener(sensorListener, 
		    		accelSens,
		    		SensorManager.SENSOR_DELAY_NORMAL);

		    sensorService.registerListener(sensorListener, 
		    		magSens,
		    		SensorManager.SENSOR_DELAY_NORMAL);

		    Log.i("GPSLogger", "Registerered for magnetic and acceleration Sensor");

	    } else {
	    	Log.e("GPSLogger", "either magnetic or oritentation sensor not found");
	    	geomag = null;
	    	gravity = null;
	    }
		
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "Service onStartCommand(-,"+flags+","+startId+")");
		startForeground(NOTIFICATION_ID, getNotification());
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "Service onDestroy()");
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
		
		// stop sensors TODO: is this good if sensors registration failed?
		sensorService.unregisterListener(sensorListener);

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	private void startTracking(long trackId) {
		currentTrackId = trackId;
		Log.v(TAG, "Starting track logging for track #" + trackId);
		// Refresh notification with correct Track ID
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nmgr.notify(NOTIFICATION_ID, getNotification());
		isTracking = true;
	}

	/**
	 * Stops GPS Logging
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.stopTracking(currentTrackId);
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
	 * Builds the notification to display when tracking in background.
	 */
	private Notification getNotification() {
		Notification n = new Notification(R.drawable.ic_stat_track, getResources().getString(R.string.notification_ticker_text), System.currentTimeMillis());
			
		Intent startTrackLogger = new Intent(this, TrackLogger.class);
		startTrackLogger.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, startTrackLogger, PendingIntent.FLAG_UPDATE_CURRENT);
		n.flags = Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		n.setLatestEventInfo(
				getApplicationContext(),
				getResources().getString(R.string.notification_title).replace("{0}", (currentTrackId > -1) ? Long.toString(currentTrackId) : "?"),
				getResources().getString(R.string.notification_text),
				contentIntent);
		return n;
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
