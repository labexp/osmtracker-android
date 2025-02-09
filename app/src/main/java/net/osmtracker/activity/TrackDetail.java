package net.osmtracker.activity;

import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.model.Track;
import net.osmtracker.gpx.ExportToStorageTask;
import net.osmtracker.util.MercatorProjection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Display details about one track.  Allow naming the track.
 * The track ID is passed into the Bundle via {@link TrackContentProvider.Schema#COL_TRACK_ID}.
 *
 * @author Jeremy D Monin <jdmonin@nand.net>
 *
 */
public class TrackDetail extends TrackDetailEditor implements AdapterView.OnItemClickListener {

	@SuppressWarnings("unused")
	private static final String TAG = TrackDetail.class.getSimpleName();

	final private int RC_WRITE_PERMISSIONS = 1;

	/**
	 * Key to bind the "key" of each item using SimpleListAdapter
	 */
	private static final String ITEM_KEY = "key";
	
	/**
	 * Key to bind the "value" of each item using SimpleListAdapter
	 */
	private static final String ITEM_VALUE = "value";
	
	/**
	 * Position of the waypoints counts in the list
	 */
	private static final int WP_COUNT_INDEX = 0;

	/** Does this track have any waypoints?  If true, underline Waypoint count in the list. */
	private boolean trackHasWaypoints = false;

	/**
	 * List with track info
	 */
	private ListView lv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.trackdetail, getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID));

		lv = (ListView) findViewById(R.id.trackdetail_list);

		final Button btnOk = (Button) findViewById(R.id.trackdetail_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
				finish();				
			}
		});
				
		final Button btnCancel = (Button) findViewById(R.id.trackdetail_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Just close the dialog
				finish();				
			}
		});
		
		// Do not show soft keyboard by default
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		
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
		
		if (! cursor.moveToFirst())	{
			// This shouldn't occur, it's here just in case.
			// So, don't make each language translate/localize it.
			Toast.makeText(this, "Track ID not found.", Toast.LENGTH_SHORT).show();
			cursor.close();
			finish();
			return;  // <--- Early return ---
		}

		// Bind WP count, TP count, start date, etc.
		// Fill name-field only if empty (in case changed by user/restored by onRestoreInstanceState) 
		Track t = Track.build(trackId, cursor, cr, true);

		bindTrack(t);		
		
		String from[] = new String[]{ITEM_KEY, ITEM_VALUE};
		int[] to = new int[] {R.id.trackdetail_item_key, R.id.trackdetail_item_value};
		
		// Waypoint count
		final int wpCount = t.getWpCount();
		trackHasWaypoints = (wpCount > 0);
		List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackmgr_waypoints_count));
		map.put(ITEM_VALUE, Integer.toString(wpCount));
		data.add(WP_COUNT_INDEX, map);
		
		// Trackpoint count
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackmgr_trackpoints_count));
		map.put(ITEM_VALUE, Integer.toString(t.getTpCount()));
		data.add(map);

		// Start date
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_startdate));
		map.put(ITEM_VALUE, t.getStartDateAsString());
		data.add(map);

		// End date
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_enddate));
		map.put(ITEM_VALUE, t.getEndDateAsString());
		data.add(map);

		// Start point
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_startloc));
		map.put(ITEM_VALUE, MercatorProjection.formatDegreesAsDMS(t.getStartLat(), true) + "  " + MercatorProjection.formatDegreesAsDMS(t.getStartLong(), false));
		data.add(map);

		// End point
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_endloc));
		map.put(ITEM_VALUE, MercatorProjection.formatDegreesAsDMS(t.getEndLat(), true) + "  " + MercatorProjection.formatDegreesAsDMS(t.getEndLong(), false));
		data.add(map);

		// OSM Upload date
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_osm_upload_date));
		if (cursor.isNull(cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE))) {
			map.put(ITEM_VALUE, getResources().getString(R.string.trackdetail_osm_upload_notyet));
		} else {
			map.put(ITEM_VALUE, DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE)))));
		}
		data.add(map);
		
		// Exported date. Should be the last item in order to be refreshed
		// if the user exports the track
		map = new HashMap<String, String>();
		map.put(ITEM_KEY, getResources().getString(R.string.trackdetail_exportdate));
		if (cursor.isNull(cursor.getColumnIndex(TrackContentProvider.Schema.COL_EXPORT_DATE))) {
			map.put(ITEM_VALUE, getResources().getString(R.string.trackdetail_export_notyet));
		} else {
			map.put(ITEM_VALUE, (DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_EXPORT_DATE))))));
		}
		data.add(map);
		
		cursor.close();
		
		TrackDetailSimpleAdapter adapter = new TrackDetailSimpleAdapter(data, from, to);
		lv.setAdapter(adapter);

		// Click on Waypoint count to see the track's WaypointList
		lv.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trackdetail_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		
		switch(item.getItemId()) {
		case R.id.trackdetail_menu_save:
			save();
			finish();	
			break;
		case R.id.trackdetail_menu_cancel:
			finish();
			break;
		case R.id.trackdetail_menu_display:
			
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
			} else {
				i = new Intent(this, DisplayTrack.class);
			}
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
			startActivity(i);	
			break;
		case R.id.trackdetail_menu_export:
			if (writeExternalStoragePermissionGranted()) {
				exportTrack();
			}
			break;
		case R.id.trackdetail_menu_osm_upload:
			i = new Intent(this, OpenStreetMapUpload.class);
			i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Checks if the external storage write permission is granted.
	 * If not, it requests the permission and may display a rationale dialog explaining why it is needed.
	 *
	 * <p>For devices running Android R (API level 30) and above, this permission is not required,
	 * so the method will return {@code true} immediately.</p>
	 *
	 * @return {@code true} if the write permission is already granted or not required (Android R+),
	 *         {@code false} otherwise.
	 */
	private boolean writeExternalStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return true;
		}
		else if (ContextCompat.checkSelfPermission(this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) {
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
							Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				new AlertDialog.Builder(this)
								.setTitle(R.string.permission_required)
								.setMessage(R.string.storage_permission_for_export_GPX)
								.setPositiveButton(R.string.acccept, (dialog, which) -> {
									// Request the permission again
									ActivityCompat.requestPermissions(this,
													new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
													RC_WRITE_PERMISSIONS);
								})
								.setNegativeButton(R.string.menu_cancel, (dialog, which) -> dialog.dismiss())
								.show();
			} else {
				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								RC_WRITE_PERMISSIONS);
			}
		}

		return ContextCompat.checkSelfPermission(this,
						Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}


	/**
	 * Invoke the export track task after external write permissions request.
	 *
	 */
	private void exportTrack(){
		new ExportToStorageTask(this, trackId).execute();
		// Pick last list item (Exported date) and update it
		SimpleAdapter adapter = ((SimpleAdapter) lv.getAdapter());
		@SuppressWarnings("unchecked")
		Map<String, String> data = (Map<String, String>) adapter.getItem(adapter.getCount() - 1);
		data.put(ITEM_VALUE, DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())));
		adapter.notifyDataSetChanged();
	}

	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case RC_WRITE_PERMISSIONS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay!
					exportTrack();

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					//TODO: add an informative message.
				}
				return;
			}
		}
	}
	
	/**
	 * Handle clicks on list items; for Waypoint count, show this track's list of waypoints ({@link WaypointList}).
	 * Ignore all other clicks.
	 * @param position  Item number in the list; this method assumes Waypoint count is position 0 (first item).
	 */
	public void onItemClick(AdapterView<?> parent, View view, final int position, final long rowid) {
		if (position != WP_COUNT_INDEX) {
			return;
		}

		Intent i = new Intent(this, WaypointList.class);
		i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
		startActivity(i);
	}
	
	/**
	 * Extend SimpleAdapter so we can underline the clickable Waypoint count.
	 * Always uses <tt>R.layout.trackdetail_item</tt> as its list item resource.
	 */
	private class TrackDetailSimpleAdapter extends SimpleAdapter
	{
		public TrackDetailSimpleAdapter
			(List<? extends Map<String, ?>> data, String[] from, int[] to)
		{
			super(TrackDetail.this, data, R.layout.trackdetail_item, from, to);
		}

		/**
		 * Get the layout for this list item. (<tt>trackdetail_item.xml</tt>)
		 * @param position  Item number in the list
		 */
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View v = super.getView(position, convertView, parent);
			if (! (v instanceof ViewGroup))
				return v;  // should not happen; v is trackdetail_item, a LinearLayout

			final boolean wantsUnderline = ((position == WP_COUNT_INDEX) && trackHasWaypoints);
			View vi = ((ViewGroup) v).findViewById(R.id.trackdetail_item_key);
			if ((vi != null) && (vi instanceof TextView))
			{
				final int flags = ((TextView) vi).getPaintFlags();
				if (wantsUnderline)
					((TextView) vi).setPaintFlags(flags | Paint.UNDERLINE_TEXT_FLAG);
				else
					((TextView) vi).setPaintFlags(flags & ~Paint.UNDERLINE_TEXT_FLAG);
			}
			return v;
		}

	}  // inner class TrackDetailSimpleAdapter

}  // public class TrackDetail
