package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.util.ThemeValidator;
import me.guillaumin.android.osmtracker.view.DisplayTrackView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ViewGroup.LayoutParams;

/**
 * Displays current track in 2D view.
 *<P>
 * Used only if {@link OSMTracker.Preferences#KEY_UI_DISPLAYTRACK_OSM} is not true.
 * Otherwise {@link DisplayTrackMap} is used.
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
		setTheme(getResources().getIdentifier(ThemeValidator.getValidTheme(
				PreferenceManager.getDefaultSharedPreferences(this), getResources()), null, null));
		
		super.onCreate(savedInstanceState);
		
		// Create special view and displays it
		final long trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		DisplayTrackView dtv = new DisplayTrackView(this, trackId);
		dtv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		setTitle(getTitle() + ": #" + getIntent().getExtras().getLong(Schema.COL_TRACK_ID));
		setContentView(dtv);		

		// If this is the first time showing this activity,
		// wait for everything to initialize and then ask
		// the user if they'd rather see the OSM background.
		SharedPreferences dtPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (! dtPrefs.getBoolean(OSMTracker.Preferences.KEY_UI_ASKED_DISPLAYTRACK_OSM, false)) {
			dtPrefs.edit().putBoolean(OSMTracker.Preferences.KEY_UI_ASKED_DISPLAYTRACK_OSM, true).commit();
			dtv.post(new Runnable() {
				@Override
				public void run() {
					new AlertDialog.Builder(DisplayTrack.this)
						.setTitle(me.guillaumin.android.osmtracker.R.string.prefs_displaytrack_osm)
						.setMessage(me.guillaumin.android.osmtracker.R.string.prefs_displaytrack_osm_summary_ask)
						.setNegativeButton(android.R.string.no, null)
						.setPositiveButton(me.guillaumin.android.osmtracker.R.string.displaytrack_map, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								PreferenceManager.getDefaultSharedPreferences(DisplayTrack.this).edit()
									.putBoolean(OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, true).commit();
								Intent i = new Intent(DisplayTrack.this, DisplayTrackMap.class);
								i.putExtra(Schema.COL_TRACK_ID, trackId);
								startActivity(i);
								finish();  // DisplayTrackMap replaces our activity
							}
						})
						.show();
				}
			});
		}
	}	
	
}
