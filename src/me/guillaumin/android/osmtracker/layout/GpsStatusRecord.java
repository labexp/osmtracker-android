package me.guillaumin.android.osmtracker.layout;

import java.text.DecimalFormat;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Layout for the GPS Status image and misc
 * action buttons.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class GpsStatusRecord extends LinearLayout implements Listener, LocationListener {
	
	private final static String TAG = GpsStatusRecord.class.getSimpleName();
	
	/**
	 * Formatter for accuracy display.
	 */
	private final static DecimalFormat ACCURACY_FORMAT = new DecimalFormat("0");
	
	/**
	 * Keeps matching between satellite indicator bars to draw, and numbers
	 * of satellites for each bars;
	 */
	private final static int[] SAT_INDICATOR_TRESHOLD = {2, 3, 4, 6, 8};
	
	/**
	 * Containing activity
	 */
	private TrackLogger activity;
	
	/**
	 * Reference to LocationManager
	 */
	private LocationManager lmgr;
	
	/**
	 * the timestamp of the last GPS fix we used
	 */
	private long lastGPSTimestampStatus = 0;

	/**
	 * the timestamp of the last GPS fix we used for location updates
	 */
	private long lastGPSTimestampLocation = 0;
	
	/**
	 * the interval (in ms) to log GPS fixes defined in the preferences
	 */
	private final long gpsLoggingInterval;
	
	/**
	 * Is GPS active ?
	 */
	private boolean gpsActive = false;

	public GpsStatusRecord(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.gpsstatus_record, this, true);

		//read the logging interval from preferences
		gpsLoggingInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;
		
		if (context instanceof TrackLogger) {
			activity = (TrackLogger) context;
			lmgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}		

	}
	
	public void requestLocationUpdates(boolean request) {
		if (request) {
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,	this);
			lmgr.addGpsStatusListener(this);
		} else {
			lmgr.removeUpdates(this);
			lmgr.removeGpsStatusListener(this);
		}
	}

	@Override
	public void onGpsStatusChanged(int event) {
		// Update GPS Status image according to event
		ImageView imgSatIndicator = (ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator);

		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_0);
			activity.onGpsEnabled();
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_unknown);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_off);
			activity.onGpsDisabled();
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			// first of all we check if the time from the last used fix to the current fix is greater than the logging interval
			if((event != GpsStatus.GPS_EVENT_SATELLITE_STATUS) || (lastGPSTimestampStatus + gpsLoggingInterval) < System.currentTimeMillis()){
				lastGPSTimestampStatus = System.currentTimeMillis(); // save the time of this fix

				GpsStatus status = lmgr.getGpsStatus(null);

				// Count active satellites
				int satCount = 0;
				for (@SuppressWarnings("unused") GpsSatellite sat:status.getSatellites()) {
					satCount++;
				}

				// Count how many bars should we draw
				int nbBars = 0;
				for (int i=0; i<SAT_INDICATOR_TRESHOLD.length; i++) {
					if (satCount >= SAT_INDICATOR_TRESHOLD[i]) {
						nbBars = i;
					}
				}
				Log.v(TAG, "Found " + satCount + " satellites. Will draw " + nbBars + " bars.");			
				imgSatIndicator.setImageResource(getResources().getIdentifier("drawable/sat_indicator_" + nbBars, null, OSMTracker.class.getPackage().getName()));
			}
			break;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		// first of all we check if the time from the last used fix to the current fix is greater than the logging interval
		if((lastGPSTimestampLocation + gpsLoggingInterval) < System.currentTimeMillis()){
			lastGPSTimestampLocation = System.currentTimeMillis(); // save the time of this fix
			Log.v(TAG, "Location received " + location);
			if (! gpsActive) {
				gpsActive = true;
				// GPS activated, activate UI
				activity.onGpsEnabled();
			}
			
			TextView tvAccuracy = (TextView) findViewById(R.id.gpsstatus_record_tvAccuracy);
			if (location.hasAccuracy()) {
				tvAccuracy.setText(getResources().getString(R.string.various_accuracy) + ": " + ACCURACY_FORMAT.format(location.getAccuracy()) + getResources().getString(R.string.various_unit_meters));
			} else {
				tvAccuracy.setText("");
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " disabled");
		gpsActive = false;
		((ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator)).setImageResource(R.drawable.sat_indicator_off);
		((TextView) findViewById(R.id.gpsstatus_record_tvAccuracy)).setText("");
		activity.onGpsDisabled();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " enabled");
		((ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator)).setImageResource(R.drawable.sat_indicator_unknown);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Update provider status image according to status
		Log.d(TAG, "Location provider " + provider + " status changed to: " + status);
		ImageView imgSatIndicator = (ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator);
		TextView tvAccuracy = (TextView) findViewById(R.id.gpsstatus_record_tvAccuracy);
		
		switch (status) {
		// Don't do anything for status AVAILABLE, as this event occurs frequently,
		// changing the graphics cause flickering .
		case LocationProvider.OUT_OF_SERVICE:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_off);
			tvAccuracy.setText("");
			gpsActive = false;
			activity.onGpsDisabled();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_unknown);
			tvAccuracy.setText("");
			gpsActive = false;
			break;
		}

	}
	
	/**
	 * Manages the state of the recording indicator, depending if we're tracking or not.
	 * @param isTracking true if the indicator must show that we're tracking, otherwise false
	 */
	public void manageRecordingIndicator(boolean isTracking) {
		ImageView recordStatus = (ImageView) findViewById(R.id.gpsstatus_record_animRec);
		if (isTracking) {
			recordStatus.setImageResource(R.drawable.record_red);
		} else {
			recordStatus.setImageResource(R.drawable.record_grey);
		}
	}

}
