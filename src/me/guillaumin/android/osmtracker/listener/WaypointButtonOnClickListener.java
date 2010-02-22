package me.guillaumin.android.osmtracker.listener;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.layout.DisablableTableLayout;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Manages clicks on way point buttons.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class WaypointButtonOnClickListener implements OnClickListener {

	private static final String TAG = WaypointButtonOnClickListener.class.getSimpleName();
	
	/**
	 * Root viewgroup containing all buttons
	 */
	private ViewGroup rootViewGroup;
	
	/**
	 * Reference to the back button to navigate
	 * between button pages.
	 */
	private Button backButton;
	
	/**
	 * Reference to activity.
	 */
	private TrackLogger activity;
	
	/**
	 * GPS Logger instance, to track waypoints.
	 */
	private GPSLogger gpsLogger;
	
	public WaypointButtonOnClickListener(ViewGroup vg, TrackLogger tl) {
		rootViewGroup = vg;
		activity = tl;
		backButton = (Button) rootViewGroup.findViewById(R.id.tracklogger_btnBack);
	}
	
	@Override
	public void onClick(View v) {
		Log.v(TAG, "Entering the BIG switch with view " + v);
		
		switch (v.getId()) {
		case R.id.tracklogger_main_btnMisc:
			changeButtons(R.layout.tracklogger_misc_buttons, true);
			break;
		case R.id.tracklogger_main_btnRestriction:
			changeButtons(R.layout.tracklogger_restriction_buttons, true);
			break;
		case R.id.tracklogger_main_btnCar:
			changeButtons(R.layout.tracklogger_car_buttons, true);
			break;
		case R.id.tracklogger_main_btnAmenity:
			changeButtons(R.layout.tracklogger_amenity_buttons, true);
			break;
		case R.id.tracklogger_main_btnAmenityMore:
			changeButtons(R.layout.tracklogger_amenitymore_buttons, true);
			break;
		case R.id.tracklogger_main_btnTourism:
			changeButtons(R.layout.tracklogger_tourism_buttons, true);
			break;
		case R.id.tracklogger_main_btnWay:
			changeButtons(R.layout.tracklogger_way_buttons, true);
			break;
		case R.id.tracklogger_main_btnTrack:
			changeButtons(R.layout.tracklogger_track_buttons, true);
			break;
		case R.id.tracklogger_main_btnLandUse:
		changeButtons(R.layout.tracklogger_landuse_buttons, true);
			break;
		case R.id.tracklogger_btnBack:
			// We're on a sub-page. Go back to main menu
			changeButtons(R.layout.tracklogger_main_buttons, false);
			break;
		default:
			// User clicked on a waypoint button
			// Get the label for the button
			Button wayPointButton = (Button) v;
			String label = wayPointButton.getText().toString();
			if (gpsLogger != null) {			
				gpsLogger.trackWayPoint(label);
			}
			
		}

	}
	
	private void changeButtons(int buttonResId, boolean enableBackButton) {
		rootViewGroup.removeViewAt(1);
		DisablableTableLayout tbl = (DisablableTableLayout) LayoutInflater.from(rootViewGroup.getContext()).inflate(buttonResId, rootViewGroup, false);
		tbl.setOnClickListenerForAllChild(this);
		rootViewGroup.addView(tbl);
		activity.setButtonTable(tbl);
		backButton.setEnabled(enableBackButton);
	}
	
	public void setGpsLogger(GPSLogger logger) {
		this.gpsLogger = logger;
	}

}
