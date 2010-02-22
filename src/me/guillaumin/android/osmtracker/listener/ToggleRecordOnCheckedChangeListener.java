package me.guillaumin.android.osmtracker.listener;

import java.io.IOException;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.widget.CompoundButton;
import android.widget.Toast;
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
		if(isChecked) {
			try {
				// Start track logging
				activity.getGpsLogger().startTracking();
				// Enable button grid
				activity.setEnabledActionButtons(true);
				
			} catch (IOException ioe) {
				// Exception occured in DataHelper
				Toast.makeText(activity, ioe.getMessage(), Toast.LENGTH_LONG).show();
				buttonView.setChecked(false);	
			}
		} else {
			// Disable button grid
			activity.setEnabledActionButtons(false);
			
			// If GPS is unavailable, grey toggle too
			if (activity.getGpsLogger().getGpsListener().getGpsStatus().isEnabled() == false) {
				buttonView.setEnabled(false);
			}
			
			// Stop tracking
			activity.getGpsLogger().stopTracking();			
		}
	}

}
