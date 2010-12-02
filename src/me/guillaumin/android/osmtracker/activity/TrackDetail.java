package me.guillaumin.android.osmtracker.activity;

import java.sql.Date;
import java.text.DateFormat;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
import me.guillaumin.android.osmtracker.gpx.ExportTrackTask;
import me.guillaumin.android.osmtracker.util.MercatorProjection;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Display details about one track.  Allow naming the track.
 * The track ID is passed into the Bundle via {@link Schema#COL_TRACK_ID}.
 *
 * @author Jeremy D Monin <jdmonin@nand.net>
 *
 */
public class TrackDetail extends Activity {

	@SuppressWarnings("unused")
	private static final String TAG = TrackDetail.class.getSimpleName();

	/** Keeps track of our track id. */
	private long trackId;

	/** Name of the track, or starting date if none */
	private String trackNameInDB = null;

	private EditText etName;
	private TextView vWps, vTps;
	private TextView vStartDate, vEndDate;
	private TextView vStartLoc, vEndLoc;
	private TextView vExportDate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get the track id to work with
		trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);

		setContentView(R.layout.trackdetail);
        setTitle(getTitle() + ": #" + trackId);

		etName = (EditText) findViewById(R.id.trackdetail_item_name);
		vWps = (TextView) findViewById(R.id.trackdetail_item_wps);
		vTps = (TextView) findViewById(R.id.trackdetail_item_tps);
		vStartDate = (TextView) findViewById(R.id.trackdetail_item_startdate);
		vEndDate = (TextView) findViewById(R.id.trackdetail_item_enddate);
		vStartLoc = (TextView) findViewById(R.id.trackdetail_item_startloc);
		vEndLoc = (TextView) findViewById(R.id.trackdetail_item_endloc);
		vExportDate = (TextView) findViewById(R.id.trackdetail_item_exportdate);
		
		final Button btnOk = (Button) findViewById(R.id.trackdetail_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/** Save changes to db (if any), then finish. */
				
				// Save name field, if changed, to db.
				// String class required for equals to work, and for trim().
				String enteredName = etName.getText().toString().trim();
				if ((enteredName.length() > 0) && (! enteredName.equals(trackNameInDB))) {
					DataHelper.setTrackName(trackId, enteredName, getContentResolver());
				}

				// All done
				finish();				
			}
		});
				
		// further work is done in onResume.
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// Query the track values
		ContentResolver cr = getContentResolver();
		Cursor cursor = cr.query(
			ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
			null, null, null, null);
		if (! cursor.moveToFirst())
		{
			// This shouldn't occur, it's here just in case.
			// So, don't make each language translate/localize it.
			Toast.makeText(this, "Track ID not found.", Toast.LENGTH_SHORT).show();
			cursor.close();
			finish();
			return;  // <--- Early return ---
		}

		// Bind WP count, TP count, start date, etc.
		// Fill name-field only if empty (in case changed by user/restored by onRestoreInstanceState) 
		Track t = Track.build(trackId, cursor, cr);
		vTps.setText(Integer.toString(t.getTpCount()));
		vWps.setText(Integer.toString(t.getWpCount()));
		if (etName.length() == 0) {
			trackNameInDB = t.getName();
			etName.setText(trackNameInDB);
		}
		vStartDate.setText(t.getStartDateAsString());
		vEndDate.setText(t.getEndDateAsString());
		vStartLoc.setText(MercatorProjection.formatDegreesAsDMS(t.getStartLat(), true) + "  " + MercatorProjection.formatDegreesAsDMS(t.getStartLong(), false));
		vEndLoc.setText(MercatorProjection.formatDegreesAsDMS(t.getEndLat(), true) + "  " + MercatorProjection.formatDegreesAsDMS(t.getEndLong(), false));
		final int exportCol = cursor.getColumnIndex(Schema.COL_EXPORT_DATE);
		if (cursor.isNull(exportCol)) {
			vExportDate.setText(R.string.trackdetail_export_notyet);
		} else {
			vExportDate.setText
			    (DateFormat.getDateTimeInstance().format
			        (new Date(cursor.getLong(exportCol))));
		}

		cursor.close();
		
		// Tell service to stop notifying user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_STOP_NOTIFY_BACKGROUND));

	}
	
	@Override
	protected void onPause() {
		// Tell service to notify user of background activity
		sendBroadcast(new Intent(OSMTracker.INTENT_START_NOTIFY_BACKGROUND));
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trackdetail_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.trackdetail_menu_save:
			String enteredName = etName.getText().toString().trim();
			if ((enteredName.length() > 0) && (! enteredName.equals(trackNameInDB))) {
				DataHelper.setTrackName(trackId, enteredName, getContentResolver());
			}

			// All done
			finish();	
			break;
		case R.id.trackdetail_menu_cancel:
			finish();
			break;
		case R.id.trackdetail_menu_display:
			Intent i;
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
			} else {
				i = new Intent(this, DisplayTrack.class);
			}
			i.putExtra(Schema.COL_TRACK_ID, trackId);
			startActivity(i);	
			break;
		case R.id.trackdetail_menu_export:
			new ExportTrackTask(this, trackId).execute();
			vExportDate.setText(DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	

}  // public class TrackDetail
