package net.osmtracker.layout;

import java.text.DecimalFormat;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.activity.TrackLogger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.activity.TrackLogger;

import java.text.DecimalFormat;

public class GpsStatusRecord extends LinearLayout implements LocationListener {

	private final static String TAG = GpsStatusRecord.class.getSimpleName();

	final private int REQUEST_CODE_GPS_PERMISSIONS = 1;

	private final static DecimalFormat ACCURACY_FORMAT = new DecimalFormat("0");

	private final static int[] SAT_INDICATOR_TRESHOLD = {2, 3, 4, 6, 8};

	private TrackLogger activity;
	private LocationManager lmgr;
	private long lastGPSTimestampStatus = 0;
	private long lastGPSTimestampLocation = 0;
	private final long gpsLoggingInterval;
	private boolean gpsActive = false;
	private int satCount = 0;
	private int fixCount = 0;

	public GpsStatusRecord(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.gpsstatus_record, this, true);

		gpsLoggingInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(context).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;

		if (context instanceof TrackLogger) {
			activity = (TrackLogger) context;
			lmgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}

		TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);
		tvAccuracy.setText(getResources().getString(R.string.various_waiting_gps_fix)
				.replace("{0}", "0")
				.replace("{1}", "0"));
	}

	public void requestLocationUpdates(boolean request) {
		if (request) {
			if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			} else {
				ActivityCompat.requestPermissions((Activity) activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						REQUEST_CODE_GPS_PERMISSIONS);
			}
		} else {
			lmgr.removeUpdates(this);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if ((lastGPSTimestampLocation + gpsLoggingInterval) < System.currentTimeMillis()) {
			lastGPSTimestampLocation = System.currentTimeMillis();
			Log.v(TAG, "Location received " + location);
			if (!gpsActive) {
				gpsActive = true;
				activity.onGpsEnabled();
			}

			TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);
			if (location.hasAccuracy()) {
				tvAccuracy.setText(getResources().getString(R.string.various_accuracy_with_sats)
						.replace("{0}", ACCURACY_FORMAT.format(location.getAccuracy()))
						.replace("{1}", getResources().getString(R.string.various_unit_meters))
						.replace("{2}", Long.toString(fixCount))
						.replace("{3}", Long.toString(satCount)));
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
		Log.d(TAG, "Location provider " + provider + " status changed to: " + status);
		ImageView imgSatIndicator = findViewById(R.id.gpsstatus_record_imgSatIndicator);
		TextView tvAccuracy = findViewById(R.id.gpsstatus_record_tvAccuracy);

		switch (status) {
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

	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_GPS_PERMISSIONS:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					requestLocationUpdates(true);
				} else {
					requestLocationUpdates(false);
				}
				break;
		}
	}

	public void manageRecordingIndicator(boolean isTracking) {
		ImageView recordStatus = findViewById(R.id.gpsstatus_record_animRec);
		if (isTracking) {
			recordStatus.setImageResource(R.drawable.record_red);
		} else {
			recordStatus.setImageResource(R.drawable.record_grey);
		}
	}

}
