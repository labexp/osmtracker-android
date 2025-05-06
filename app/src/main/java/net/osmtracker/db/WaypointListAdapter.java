package net.osmtracker.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.osmtracker.R;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

/**
 * Adapter for the waypoint list. Gets waypoints from database.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointListAdapter extends CursorAdapter {

	/**
	 * Date formatter
	 */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss 'UTC'");
	static {
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 *				Application context
	 * @param c
	 *				{@link Cursor} to data
	 */
	public WaypointListAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TableLayout tl = (TableLayout) view;
		bind(cursor, tl, context);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup vg) {
		TableLayout tl = (TableLayout) LayoutInflater.from(vg.getContext()).inflate(R.layout.waypointlist_item,
				vg, false);
		return bind(cursor, tl, context);
	}

	/**
	 * Do the binding between data and item view.
	 * 
	 * @param cursor
	 *				Cursor to pull data
	 * @param tl
	 *				RelativeView representing one item
	 * @param context
	 *				Context, to get resources
	 * @return The relative view with data bound.
	 */
	private View bind(Cursor cursor, TableLayout tl, Context context) {
		TextView vName = (TextView) tl.findViewById(R.id.wplist_item_name);
		TextView vLocation = (TextView) tl.findViewById(R.id.wplist_item_location);
		TextView vTimestamp = (TextView) tl.findViewById(R.id.wplist_item_timestamp);

		// Bind name
		String name = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
		vName.setText(name);

		// Bind location
		StringBuffer locationAsString = new StringBuffer();
		locationAsString.append(context.getResources().getString(R.string.wplist_latitude)
				+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE)));
		locationAsString.append(", " + context.getResources().getString(R.string.wplist_longitude)
				+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE)));
		if (!cursor.isNull(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION))) {
			locationAsString.append(", " + context.getResources().getString(R.string.wplist_elevation)
					+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ELEVATION)));
		}
		if (!cursor.isNull(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY))) {
			locationAsString.append(", " + context.getResources().getString(R.string.wplist_accuracy)
					+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ACCURACY)));
		}
		if (!cursor.isNull(cursor.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS))) {
			locationAsString.append(", " + context.getResources().getString(R.string.wplist_compass)
					+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS)));
			locationAsString.append(", " + context.getResources().getString(R.string.wplist_compass_accuracy)
					+ cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_COMPASS_ACCURACY)));
			
		
		}		
		vLocation.setText(locationAsString.toString());

		// Bind timestamp
		Date ts = new Date(cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_TIMESTAMP)));
		vTimestamp.setText(DATE_FORMATTER.format(ts));
		return tl;
	}

}
