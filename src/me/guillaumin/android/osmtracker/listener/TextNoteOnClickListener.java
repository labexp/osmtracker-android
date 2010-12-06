package me.guillaumin.android.osmtracker.listener;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Manages text note button.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class TextNoteOnClickListener implements OnClickListener {

	
	private TrackLogger tl;
	
	public TextNoteOnClickListener(TrackLogger trackLogger) {
		tl = trackLogger;
	}
	
	@Override
	public void onClick(final View v) {
		// let the TrackLogger activity open and control the dialog
		tl.showDialog(TrackLogger.DIALOG_TEXT_NOTE);
	}

}
