package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.view.DisplayTrackView;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ViewGroup.LayoutParams;

/**
 * Displays current track in 2D view.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class DisplayTrack extends Activity {

	@SuppressWarnings("unused")
	private static final String TAG = DisplayTrack.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set application theme according to user settings
		String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
				OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
		setTheme(getResources().getIdentifier(theme, null, null));
		
		super.onCreate(savedInstanceState);
		
		// Create special view and displays it
		DisplayTrackView dtv = new DisplayTrackView(this, getIntent().getExtras().getLong(Schema.COL_TRACK_ID));
		dtv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		setTitle(getTitle() + ": #" + getIntent().getExtras().getLong(Schema.COL_TRACK_ID));
		setContentView(dtv);		
	}	
	
}
