package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import me.guillaumin.android.osmtracker.util.MercatorProjection;
import me.guillaumin.android.osmtracker.view.DisplayTrackView;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

/**
 * Displays current track in 2D view.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class DisplayTrack extends Activity {

	private static final String TAG = DisplayTrack.class.getSimpleName();
	
	/**
	 * View to display track on.
	 */
	private DisplayTrackView dtv;
	
	/**
	 * GPS Logger service.
	 */
	private GPSLogger gpsLogger;
	
	/**
	 * Service connection to the GPS logger service.
	 */
	private ServiceConnection gpsLoggerConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			gpsLogger = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			gpsLogger = ((GPSLogger.GPSLoggerBinder) service).getService();
			double[][] coords = populateCoords(gpsLogger.getDataHelper().getTrackpointsCursor());
			if (coords != null && coords.length > 0) {
				// Sets coordinates to draw to the view
				dtv.setCoords(coords);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set application theme according to user settings
		String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
				OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
		setTheme(getResources().getIdentifier(theme, null, null));
		
		super.onCreate(savedInstanceState);
		
		// Create special view and displays it
		dtv = new DisplayTrackView(this);
		dtv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		setContentView(dtv);
		
		
	}
	
	/**
	 * Populate coordinates from a cursor to current track Database
	 * @param c Cursor on trackpoint table
	 * @return An array of double[lon, lat]
	 */
	public double[][] populateCoords(Cursor c) {
		double[][] out = new double[c.getCount()][2];
		int i=0;
		
		while (!c.isAfterLast() ) {
			out[i][MercatorProjection.LONGITUDE] = c.getDouble(c.getColumnIndex(DataHelper.Schema.COL_LONGITUDE));
			out[i][MercatorProjection.LATITUDE] = c.getDouble(c.getColumnIndex(DataHelper.Schema.COL_LATITUDE));
			i++;
			c.moveToNext();
		}
		c.close();
		
		Log.v(TAG, "Extracted " + out.length + " points from DB.");
		
		return out;
	}
	
	@Override
	protected void onResume() {
		// Bind to GPS service
		bindService(new Intent(this, GPSLogger.class), gpsLoggerConnection, 0);
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Unbind GPS service
		unbindService(gpsLoggerConnection);
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		super.onPause();
	}
	
}
