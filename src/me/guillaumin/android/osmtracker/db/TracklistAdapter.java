package me.guillaumin.android.osmtracker.db;

import java.sql.Date;
import java.text.SimpleDateFormat;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Adapter for track list.
 * @author Nicolas Guillaumin
 *
 */
public class TracklistAdapter extends CursorAdapter {

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE dd MMM yyyy, HH:mm:ss");
	
	public TracklistAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		RelativeLayout rl = (RelativeLayout) view;
		bind(cursor, rl, context);	
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup vg) {
		RelativeLayout rl = (RelativeLayout) LayoutInflater.from(vg.getContext()).inflate(R.layout.tracklist_item,
				vg, false);
		return bind(cursor, rl, context);
	}
	
	/**
	 * Do the binding between data and item view.
	 * 
	 * @param cursor
	 *            Cursor to pull data
	 * @param rl
	 *            RelativeView representing one item
	 * @param context
	 *            Context, to get resources
	 * @return The relative view with data bound.
	 */
	private View bind(Cursor cursor, RelativeLayout rl, Context context) {
		TextView vStartDate = (TextView) rl.findViewById(R.id.tracklist_item_startdate);
		TextView vWps = (TextView) rl.findViewById(R.id.tracklist_item_waypoints);
		TextView vTps = (TextView) rl.findViewById(R.id.tracklist_item_trackpoints);

		String trackId = Long.toString(cursor.getLong(cursor.getColumnIndex(Schema.COL_ID)));

		// Bind start date
		long startDate = cursor.getLong(cursor.getColumnIndex(Schema.COL_START_DATE));
		vStartDate.setText("#" + trackId + ". " + DATE_FORMAT.format(new Date(startDate)));
		
		// Bind WP count
		Cursor wpCursor = context.getContentResolver().query(TrackContentProvider.CONTENT_URI_WAYPOINT,
				new String[]{Schema.COL_ID}, Schema.COL_TRACK_ID + " = ?",
				new String[]{trackId}, null);
		vWps.setText(context.getResources().getString(R.string.waypoints_count) + " " + Integer.toString(wpCursor.getCount()));
		wpCursor.close();

		// Bind TP count
		Cursor tpCursor = context.getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACKPOINT,
				new String[]{Schema.COL_ID}, Schema.COL_TRACK_ID + " = ?",
				new String[]{trackId}, null);
		vTps.setText(context.getResources().getString(R.string.trackpoints_count) + " " + Integer.toString(tpCursor.getCount()));
		tpCursor.close();

		return rl;
	}

}
