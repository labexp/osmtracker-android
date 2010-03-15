package me.guillaumin.android.osmtracker.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import me.guillaumin.android.osmtracker.R;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Adapter for the waypoint list.
 * Gets waypoints from database.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class WaypointListAdapter extends CursorAdapter {

	/**
	 * Date formatter
	 */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss 'UTC'");
	
	/**
	 * Constructor.
	 * @param context Applcation context
	 * @param c {@link Cursor} to data
	 */
	public WaypointListAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		RelativeLayout rl = (RelativeLayout) view;
		bind(cursor, rl, context);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup vg) {
		RelativeLayout rl = (RelativeLayout) LayoutInflater.from(vg.getContext()).inflate(R.layout.waypointlist_item, vg, false);
		return bind(cursor, rl, context);
	}
	
	/**
	 * Do the binding between data and item view.
	 * @param cursor Cursor to pull data
	 * @param rl RelativeView representing one item
	 * @param context Context, to get resources
	 * @return The relative view with data bound.
	 */
	private View bind(Cursor cursor, RelativeLayout rl, Context context) {
		TextView vName = (TextView) rl.findViewById(R.id.wplist_item_name);
		TextView vLocation = (TextView) rl.findViewById(R.id.wplist_item_location);
		TextView vTimestamp = (TextView) rl.findViewById(R.id.wplist_item_timestamp);
		
		// Bind name
		String name = cursor.getString(cursor.getColumnIndex(DataHelper.Schema.COL_NAME));
		// Strip \n like in "Place of\nworship"
		vName.setText(name);
		
		// Bind location
		StringBuffer locationAsString = new StringBuffer();
		locationAsString.append(context.getResources().getString(R.string.wplist_latitude) + cursor.getString(cursor.getColumnIndex(DataHelper.Schema.COL_LATITUDE)));
		locationAsString.append(", " + context.getResources().getString(R.string.wplist_longitude) + cursor.getString(cursor.getColumnIndex(DataHelper.Schema.COL_LONGITUDE)));
		locationAsString.append(", " + context.getResources().getString(R.string.wplist_elevation) + cursor.getString(cursor.getColumnIndex(DataHelper.Schema.COL_ELEVATION)));
		vLocation.setText(locationAsString.toString());
		
		// Bind timestamp
		Date ts = new Date(cursor.getLong(cursor.getColumnIndex(DataHelper.Schema.COL_TIMESTAMP)));
		vTimestamp.setText(DATE_FORMATTER.format(ts));
		
		return rl;
	}
	
}
