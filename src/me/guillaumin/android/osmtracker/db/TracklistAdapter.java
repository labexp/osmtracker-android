package me.guillaumin.android.osmtracker.db;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for track list in {@link me.guillaumin.android.osmtracker.activity.TrackManager Track Manager}.
 * For each row's contents, see <tt>tracklist_item.xml</tt>.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class TracklistAdapter extends CursorAdapter {

	public TracklistAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		bind(cursor, view, context);	
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup vg) {
		View view = LayoutInflater.from(vg.getContext()).inflate(R.layout.tracklist_item,
				vg, false);
		return view;
	}
	
	/**
	 * Do the binding between data and item view.
	 * 
	 * @param cursor
	 *				Cursor to pull data
	 * @param v
	 *				RelativeView representing one item
	 * @param context
	 *				Context, to get resources
	 * @return The relative view with data bound.
	 */
	private View bind(Cursor cursor, View v, Context context) {
		TextView vId = (TextView) v.findViewById(R.id.trackmgr_item_id);
		TextView vNameOrStartDate = (TextView) v.findViewById(R.id.trackmgr_item_nameordate);
		TextView vWps = (TextView) v.findViewById(R.id.trackmgr_item_wps);
		TextView vTps = (TextView) v.findViewById(R.id.trackmgr_item_tps);
		ImageView vStatus = (ImageView) v.findViewById(R.id.trackmgr_item_statusicon);
		ImageView vUploadStatus = (ImageView) v.findViewById(R.id.trackmgr_item_upload_statusicon);

		// Is track active ?
		int active = cursor.getInt(cursor.getColumnIndex(Schema.COL_ACTIVE));
		if (Schema.VAL_TRACK_ACTIVE == active) {
			// Yellow clock icon for Active
			vStatus.setImageResource(android.R.drawable.presence_away);
			vStatus.setVisibility(View.VISIBLE);
		} else if (cursor.isNull(cursor.getColumnIndex(Schema.COL_EXPORT_DATE))) {
			// Hide green circle icon: Track not yet exported
			vStatus.setVisibility(View.GONE);
		} else {
			// Show green circle icon (don't assume already visible with this drawable; may be a re-query)
			vStatus.setImageResource(android.R.drawable.presence_online);
			vStatus.setVisibility(View.VISIBLE);
		}
		
		// Upload status
		if (cursor.isNull(cursor.getColumnIndex(Schema.COL_OSM_UPLOAD_DATE))) {
			vUploadStatus.setVisibility(View.GONE);
		}		
		
		// Bind id
		long trackId = cursor.getLong(cursor.getColumnIndex(Schema.COL_ID));
		String strTrackId = Long.toString(trackId);
		vId.setText("#" + strTrackId);

		// Bind WP count, TP count, name
		Track t = Track.build(trackId, cursor, context.getContentResolver(), false);
		vTps.setText(Integer.toString(t.getTpCount()));
		vWps.setText(Integer.toString(t.getWpCount()));
		vNameOrStartDate.setText(t.getName());

		return v;
	}

}
