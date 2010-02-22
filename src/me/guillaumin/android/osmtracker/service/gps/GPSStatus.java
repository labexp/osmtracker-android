package me.guillaumin.android.osmtracker.service.gps;

import android.location.GpsStatus;
import android.location.LocationProvider;

/**
 * Represents the GPS Status. Used to update the UI
 * according the status.
 *
 */
public class GPSStatus {
	/**
	 * Is GPS enabled ?
	 */
	private boolean enabled = false;
	
	/**
	 * Last GPSStatus.* status.
	 */
	private int lastGpsStatus = -1;
	
	/**
	 * Last LocationProvider.* status
	 */
	private int lastProviderStatus = -1;
	
	/**
	 * Has the first location event occured ?
	 */
	private boolean firstLocationReceived = false;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if(enabled == false) {
			// GPS disabled, unflag first location event
			firstLocationReceived = false;
		}
	}
	public int getLastGpsStatus() {
		return lastGpsStatus;
	}
	public void setLastGpsStatus(int lastGpsStatus) {
		this.lastGpsStatus = lastGpsStatus;
		switch(lastGpsStatus) {
		case GpsStatus.GPS_EVENT_STOPPED:
			// GPS disabled, unflag first location event
			firstLocationReceived = false;
			break;
		}
	}
	public int getLastProviderStatus() {
		return lastProviderStatus;
	}
	public void setLastProviderStatus(int lastProviderStatus) {
		this.lastProviderStatus = lastProviderStatus;
		switch(lastProviderStatus) {
		case LocationProvider.OUT_OF_SERVICE:
			// GPS disabled, unflag first location event
			firstLocationReceived = false;
			break;
		}
	}
	public boolean isFirstLocationReceived() {
		return firstLocationReceived;
	}
	public void setFirstLocationReceived(boolean firstLocationReceived) {
		this.firstLocationReceived = firstLocationReceived;
	}
}
