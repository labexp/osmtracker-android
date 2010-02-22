package me.guillaumin.android.osmtracker.listener;

import java.io.IOException;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
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
			
			activity.getGpsLogger().stopTracking();			
		}
	}

}
