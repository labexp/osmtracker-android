package me.guillaumin.android.osmtracker.listener;

import me.guillaumin.android.osmtracker.activity.OSMTracker;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Manages the toggling of the track logging control.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class ToggleRecordOnCheckedChangeListener implements OnCheckedChangeListener {

	/**
	 * Reference to the activity
	 */
	private TrackLogger activity;

	public ToggleRecordOnCheckedChangeListener(TrackLogger parent) {
		activity = parent;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// The tracking state of GPSLogger should always be checked
		// We can be already tracking, this occurs when the UI re-binds to the
		// service, if the user has quit application, but the service was still
		// tracking
		// in background
		// In this case the check state has been set programmatically, and the
		// event
		// should be ignored.
		if (isChecked && !activity.getGpsLogger().isTracking()) {
			// Start track logging
			Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
			activity.sendBroadcast(intent);
			// Enable button grid
			activity.setEnabledActionButtons(true);

		} else if (!isChecked && activity.getGpsLogger().isTracking()) {
			// Disable button grid
			activity.setEnabledActionButtons(false);

			// If GPS is unavailable, grey toggle too
			if (activity.getGpsLogger().isGpsEnabled() == false) {
				buttonView.setEnabled(false);
			}

			// Stop tracking
			Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
			activity.sendBroadcast(intent);
		}
	}
}
