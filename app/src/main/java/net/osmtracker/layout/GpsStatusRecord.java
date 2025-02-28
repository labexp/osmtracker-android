package net.osmtracker.layout;

import java.text.DecimalFormat;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.activity.TrackLogger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Layout for the GPS Status image and misc action buttons.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class GpsStatusRecord extends LinearLayout implements LocationListener {
	
	private final static String TAG = GpsStatusRecord.class.getSimpleName();

	final private int REQUEST_CODE_GPS_PERMISSIONS = 1;

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

	/**
	 * Satellites count
	 */
	private int satCount = 0;

	/**
	 * Satellites used in fix count
	 */
	private int fixCount = 0;

	
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
		
		// Initialize waiting message (0 satellites so far)
		TextView tvAccuracy = (TextView) findViewById(R.id.gpsstatus_record_tvAccuracy);
		tvAccuracy.setText(getResources().getString(R.string.various_waiting_gps_fix)
				.replace("{0}", "0")
				.replace("{1}", "0"));

	}

	public void requestLocationUpdates(boolean request) {
		if (request) {
			if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
				lmgr.registerGnssStatusCallback(mStatusCallback);
			} else {
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						REQUEST_CODE_GPS_PERMISSIONS);
			}
		} else {
			lmgr.removeUpdates(this);
			lmgr.unregisterGnssStatusCallback(mStatusCallback);
		}
	}

	private GnssStatus.Callback mStatusCallback = new GnssStatus.Callback() {
		@Override
		public void onSatelliteStatusChanged(GnssStatus status) {
			satCount = status.getSatelliteCount();
			fixCount = 0;

			for (int i = 0; i < satCount; i++) {
				if (status.usedInFix(i)) {
					fixCount++;
				}
			}

			if (fixCount == 0) {
				TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);
				tvAccuracy.setText(getResources().getString(R.string.various_waiting_gps_fix)
						.replace("{0}", Long.toString(fixCount))
						.replace("{1}", Long.toString(satCount)));

				((ImageView) findViewById(R.id.gpsstatus_record_imgSatIndicator)).setImageResource(R.drawable.sat_indicator_unknown);
			}

			Log.v(TAG, "Found " + satCount + " satellites. " + fixCount + " used in fix.");
		}
	};

	@Override
	public void onLocationChanged(Location location) {
		// first of all we check if the time from the last used fix to the current fix is greater than the logging interval
		if ((lastGPSTimestampLocation + gpsLoggingInterval) < System.currentTimeMillis()) {
			lastGPSTimestampLocation = System.currentTimeMillis(); // save the time of this fix
			Log.v(TAG, "Location received " + location);
			if (!gpsActive) {
				gpsActive = true;
				// GPS activated, activate UI
				activity.onGpsEnabled();
				manageRecordingIndicator(true);
			}
			else if (gpsActive && !activity.getButtonsEnabled()) {
				activity.onGpsEnabled();
				manageRecordingIndicator(true);
			}

			TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);
			if (location.hasAccuracy()) {
				Log.d(TAG, "location accuracy: "+ ACCURACY_FORMAT.format(location.getAccuracy()));
				tvAccuracy.setText(getResources().getString(R.string.various_accuracy_with_sats)
						.replace("{0}", ACCURACY_FORMAT.format(location.getAccuracy()))
						.replace("{1}", getResources().getString(R.string.various_unit_meters))
						.replace("{2}", Long.toString(fixCount))
						.replace("{3}", Long.toString(satCount)));

				manageSatelliteStatusIndicator((int) location.getAccuracy());

			} else {
				Log.d(TAG, "location without accuracy");
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
		ImageView imgSatIndicator = findViewById(R.id.gpsstatus_record_imgSatIndicator);
		TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);

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
		ImageView recordStatus = findViewById(R.id.gpsstatus_record_animRec);
		if (isTracking) {
			recordStatus.setImageResource(R.drawable.record_red);
		} else {
			recordStatus.setImageResource(R.drawable.record_grey);
		}
	}

	/**
	 * Manages the state of the satellites status
	 * @param accuracy in meters, the smaller the number the better the accuracy.
	 */
	private void manageSatelliteStatusIndicator(int accuracy){
		ImageView imgSatIndicator = findViewById(R.id.gpsstatus_record_imgSatIndicator);

		int nbBars = accuracy / 4;

		if (nbBars == 0) {
			Log.v(TAG, "Will draw 5 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_5);
		} else if (nbBars == 1) {
			Log.v(TAG, "Will draw 4 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_4);
		} else if (nbBars == 2) {
			Log.v(TAG, "Will draw 3 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_3);
		} else if (nbBars == 3) {
			Log.v(TAG, "Will draw 2 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_2);
		} else if (nbBars == 4) {
			Log.v(TAG, "Will draw 1 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_1);
		} else {
			Log.v(TAG, "Will draw 0 bars.");
			imgSatIndicator.setImageResource(R.drawable.sat_indicator_0);
		}
	}

}
