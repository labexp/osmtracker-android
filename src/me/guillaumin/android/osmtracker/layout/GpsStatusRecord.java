package me.guillaumin.android.osmtracker.layout;

import me.guillaumin.android.osmtracker.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Layout for the GPS Status image, Location status
 * image and record button.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GpsStatusRecord extends LinearLayout {
	
	public GpsStatusRecord(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.gpsstatus_record, this, true);
		
		// Disable by default the 2 buttons
		setButtonsEnabled(false);
	}
	
	/**
	 * Enables or disable the 2 buttons.
	 * @param enabled If true, enable the 2 buttons, otherwise disable them.
	 */
	public void setButtonsEnabled(boolean enabled) {
		findViewById(R.id.gpsstatus_record_btnVoiceRecord).setEnabled(enabled);
		findViewById(R.id.gpsstatus_record_toggleTrack).setEnabled(enabled);		
	}

}
