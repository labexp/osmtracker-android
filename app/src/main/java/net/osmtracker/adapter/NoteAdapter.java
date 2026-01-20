package net.osmtracker.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

	public static final SimpleDateFormat DATE_FORMATTER =
			new SimpleDateFormat("HH:mm:ss 'UTC'", Locale.ROOT);
	static {
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private Cursor cursor;
	private final OnNoteClickListener listener;

	public interface OnNoteClickListener {
		void onNoteClick(long id, long noteId, String uuid, String name);
	}

	public NoteAdapter(OnNoteClickListener listener) {
		this.listener = listener;
	}

	public void swapCursor(Cursor newCursor) {
		if (cursor == newCursor) return;
		if (cursor != null) cursor.close();
		cursor = newCursor;
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(
				parent.getContext()).inflate(R.layout.notelist_item, parent, false);
		return new NoteViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
		if (cursor.moveToPosition(position)) {
			Context context = holder.itemView.getContext();

			// Bind name
			String name = cursor.getString(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_NAME));
			holder.tvName.setText(name);

			// Upload status
			if (cursor.isNull(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE))) {
				holder.ivUploadStatus.setVisibility(View.GONE);
			} else {
				holder.ivUploadStatus.setImageResource(android.R.drawable.stat_sys_upload_done);
				holder.ivUploadStatus.setVisibility(View.VISIBLE);
			}

			//Bind Location (Latitude/Longitude)
			String lat = cursor.getString(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LATITUDE));
			String lon = cursor.getString(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LONGITUDE));

			String locationStr = context.getString(R.string.wplist_latitude) + lat + ", " +
					context.getString(R.string.wplist_longitude) + lon;
			holder.tvLocation.setText(locationStr);

			// Bind Timestamp
			Date ts = new Date(cursor.getLong(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_TIMESTAMP)));
			holder.tvTimestamp.setText(DATE_FORMATTER.format(ts));

			// Setup Click Listener
			String uuid = cursor.getString(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_UUID));
			long trackId = cursor.getLong(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_TRACK_ID));
			long noteId =  cursor.getLong(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_ID));
			holder.itemView.setOnClickListener(
					v -> listener.onNoteClick(trackId, noteId, uuid, name));
		}
	}

	@Override
	public int getItemCount() {
		return (cursor == null) ? 0 : cursor.getCount();
	}

	public static class NoteViewHolder extends RecyclerView.ViewHolder {
		TextView tvName;
		ImageView ivUploadStatus;
		TextView tvLocation;
		TextView tvTimestamp;


		public NoteViewHolder(View v) {
			super(v);
			tvName = v.findViewById(R.id.notelist_item_name);
			ivUploadStatus = v.findViewById(R.id.notelist_item_upload_status_icon);
			tvLocation = v.findViewById(R.id.notelist_item_location);
			tvTimestamp = v.findViewById(R.id.notelist_item_timestamp);
		}
	}
}