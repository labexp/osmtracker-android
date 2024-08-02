package net.osmtracker.listener;

import net.osmtracker.activity.TrackLogger;

import android.view.View;
import android.view.View.OnClickListener;

/**
 * Manages still image recording with camera app.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class StillImageOnClickListener implements OnClickListener {

	/**
	 * Parent activity
	 */
	TrackLogger activity;
	
	public StillImageOnClickListener(TrackLogger parent) {
		activity = parent;
	}
	
	@Override
	public void onClick(View v) {
		activity.requestStillImage();
	}
}
