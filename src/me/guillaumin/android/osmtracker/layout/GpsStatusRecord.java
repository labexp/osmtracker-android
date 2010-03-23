package me.guillaumin.android.osmtracker.layout;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.listener.StillImageOnClickListener;
import me.guillaumin.android.osmtracker.listener.TextNoteOnClickListener;
import me.guillaumin.android.osmtracker.listener.ToggleRecordOnCheckedChangeListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

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

		if (context instanceof TrackLogger) {
			activity = (TrackLogger) context;
			// Register listeners
			((ToggleButton) findViewById(R.id.gpsstatus_record_toggleTrack)).setOnCheckedChangeListener(new ToggleRecordOnCheckedChangeListener(activity));;
			((Button) findViewById(R.id.gpsstatus_record_btnVoiceRecord)).setOnClickListener(new VoiceRecOnClickListener(activity));
			((Button) findViewById(R.id.gpsstatus_record_btnStillImage)).setOnClickListener(new StillImageOnClickListener(activity));
			((Button) findViewById(R.id.gpsstatus_record_btnTextNote)).setOnClickListener(new TextNoteOnClickListener(activity));
			
		}
		
		// Disable by default the buttons
		findViewById(R.id.gpsstatus_record_toggleTrack).setEnabled(false);
		setButtonsEnabled(false);
		
		lmgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

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
	 *            If true, enable the buttons, otherwise disable them.
	 */
	public void setButtonsEnabled(boolean enabled) {
		findViewById(R.id.gpsstatus_record_btnVoiceRecord).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_btnStillImage).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_btnTextNote).setEnabled(enabled);
		
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
		gpsActive = false;
		((ImageView) findViewById(R.id.gpsstatus_record_imgLocationStatus)).setImageResource(R.drawable.satellite_off);
		((ImageView) findViewById(R.id.gpsstatus_record_imgGpsStatus)).setImageResource(R.drawable.ledgrey_32x32);
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
			gpsActive = false;
			activity.onGpsDisabled();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			imgProviderStatus.setImageResource(R.drawable.satellite_unknown);
			gpsActive = false;
			break;
		}

	}

}
