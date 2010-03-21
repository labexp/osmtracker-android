package me.guillaumin.android.osmtracker.layout;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Layout for the GPS Status image, Location status image and record button.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class GpsStatusRecord extends LinearLayout implements Listener, LocationListener {
	
	private final static String TAG = GpsStatusRecord.class.getSimpleName();
	
	/**
	 * Containing activity
	 */
	private TrackLogger activity;
	
	/**
	 * Reference to LocationManager
	 */
	private LocationManager lmgr;
	
	/**
	 * Is GPS active ?
	 */
	private boolean gpsActive = false;

	public GpsStatusRecord(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.gpsstatus_record, this, true);

		// Disable by default the buttons
		setButtonsEnabled(false);
		
		lmgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		if ( context instanceof TrackLogger) {
			activity = (TrackLogger) context;
		}

	}
	
	public void requestLocationUpdates(boolean request) {
		if (request) {
			lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			lmgr.addGpsStatusListener(this);
		} else {
			lmgr.removeUpdates(this);
			lmgr.removeGpsStatusListener(this);
		}
	}

	/**
	 * Enables or disable the buttons.
	 * 
	 * @param enabled
	 *            If true, enable the 2 buttons, otherwise disable them.
	 */
	public void setButtonsEnabled(boolean enabled) {
		findViewById(R.id.gpsstatus_record_btnVoiceRecord).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_btnStillImage).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_toggleTrack).setEnabled(enabled);
	}

	@Override
	public void onGpsStatusChanged(int event) {
		// Update GPS Status image according to event
		ImageView imgGpsStatus = (ImageView) findViewById(R.id.gpsstatus_record_imgGpsStatus);

		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			imgGpsStatus.setImageResource(R.drawable.ledgreen_32x32);
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			imgGpsStatus.setImageResource(R.drawable.ledorange_32x32);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			imgGpsStatus.setImageResource(R.drawable.ledred_32x32);
			break;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (! gpsActive) {
			gpsActive = true;
			// GPS activated, activate UI
			activity.onGpsEnabled();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " disabled");
		ImageView imgProviderStatus = (ImageView) findViewById(R.id.gpsstatus_record_imgLocationStatus);
		imgProviderStatus.setImageResource(R.drawable.satellite_off);
		activity.onGpsDisabled();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Location provider " + provider + " enabled");
		ImageView imgProviderStatus = (ImageView) findViewById(R.id.gpsstatus_record_imgLocationStatus);
		imgProviderStatus.setImageResource(R.drawable.satellite);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Update provider status image according to status
		Log.d(TAG, "Location provider " + provider + " status changed to: " + status);
		ImageView imgProviderStatus = (ImageView) findViewById(R.id.gpsstatus_record_imgLocationStatus);
		
		switch (status) {
		case LocationProvider.AVAILABLE:
			imgProviderStatus.setImageResource(R.drawable.satellite);
			break;
		case LocationProvider.OUT_OF_SERVICE:
			imgProviderStatus.setImageResource(R.drawable.satellite_off);
			activity.onGpsDisabled();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			imgProviderStatus.setImageResource(R.drawable.satellite_unknown);
			break;
		}

	}

}
