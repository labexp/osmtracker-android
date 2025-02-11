package net.osmtracker.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.WaypointListAdapter;
import net.osmtracker.listener.EditWaypointDialogOnClickListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

	private static final String TAG = "Waypoint List" ;
	private static final Logger log = LoggerFactory.getLogger(WaypointList.class);

	@Override
	protected void onResume() {
		Long trackId = getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		
		Cursor cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " desc");
		startManagingCursor(cursor);
		setListAdapter(new WaypointListAdapter(WaypointList.this, cursor));
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		if (adapter != null) {
			// Properly close the adapter cursor
			Cursor cursor = adapter.getCursor();
			stopManagingCursor(cursor);
			cursor.close();
			setListAdapter(null);
		}

		super.onPause();
	}

	/**
	 * Handles the selection of a waypoint from the list and opens an edit dialog.
	 * This dialog allows the user to update the waypoint's name and preview attached files (images or audio).
	 *
	 * @param l The ListView where the item was clicked.
	 * @param v The view that was clicked.
	 * @param position The position of the clicked item.
	 * @param id The ID of the clicked waypoint.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = ((CursorAdapter) getListAdapter()).getCursor();
		final DataHelper dataHelper = new DataHelper(l.getContext());
		LayoutInflater inflater = this.getLayoutInflater();

		// Inflate the waypoint edit dialog layout
		final View editWaypointDialog = inflater.inflate(R.layout.edit_waypoint_dialog, null);
		final EditText editWaypointName = editWaypointDialog.findViewById(R.id.edit_waypoint_et_name);
		final ImageView waypointPreviewImage = editWaypointDialog.findViewById(R.id.waypoint_preview_image);
		final Button waypointPlayAudio = editWaypointDialog.findViewById(R.id.waypoint_play_audio);

		Button buttonUpdate = editWaypointDialog.findViewById(R.id.edit_waypoint_button_update);
		Button buttonDelete = editWaypointDialog.findViewById(R.id.edit_waypoint_button_delete);
		Button buttonCancel = editWaypointDialog.findViewById(R.id.edit_waypoint_button_cancel);

		// Retrieve existing waypoint name
		String oldName = cursor.getString(cursor.getColumnIndex("name"));
		editWaypointName.setText(oldName);
		editWaypointName.setSelection(oldName.length());

		// Retrieve waypoint details
		final long trackId = cursor.getLong(cursor.getColumnIndex("track_id"));
		final String uuid = cursor.getString(cursor.getColumnIndex("uuid"));
		final String link = cursor.getString(cursor.getColumnIndex("link"));

		Log.d(TAG, "***********************\n" + link);
		final String filePath = (link != null) ? DataHelper.getTrackDirectory(trackId, l.getContext()) + "/" + link : null;
		File file = (filePath != null) ? new File(filePath) : null;

		if (file != null && file.exists()) {
			try {
				if (isImageFile(filePath)) {
					waypointPreviewImage.setVisibility(View.VISIBLE);
					waypointPreviewImage.setImageURI(Uri.fromFile(file));
				} else if (isAudioFile(filePath)) {
					waypointPlayAudio.setVisibility(View.VISIBLE);
					waypointPlayAudio.setOnClickListener(v1 -> playAudio(filePath));
				}
			} catch (Exception e) {
				Log.e(TAG, "Error handling file: " + filePath, e);
			}
		}


		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();

		// Update waypoint name
		buttonUpdate.setOnClickListener(new EditWaypointDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				String newName = editWaypointName.getText().toString();
				dataHelper.updateWayPoint(trackId, uuid, newName, link);
				alert.dismiss();
			}
		});

		// Delete waypoint
		buttonDelete.setOnClickListener(new EditWaypointDialogOnClickListener(alert, cursor) {
			@Override
			public void onClick(View view) {
				dataHelper.deleteWayPoint(uuid, filePath);
				cursor.requery();
				alert.dismiss();
			}
		});

		// Cancel button
		buttonCancel.setOnClickListener(new EditWaypointDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				alert.dismiss();
			}
		});

		alert.setView(editWaypointDialog);
		alert.show();

		super.onListItemClick(l, v, position, id);
	}

	/**
	 * Checks if a given file path corresponds to an image.
	 *
	 * @param path The file path.
	 * @return True if the file is an image, false otherwise.
	 */
	private boolean isImageFile(String path) {
		String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
		String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
		return Arrays.asList(imageExtensions).contains(ext);
	}

	/**
	 * Checks if a given file path corresponds to an audio file.
	 *
	 * @param path The file path.
	 * @return True if the file is an audio file, false otherwise.
	 */
	private boolean isAudioFile(String path) {
		return path.endsWith(".3gpp") || path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".ogg");
	}

	/**
	 * Plays an audio file using MediaPlayer.
	 *
	 * @param filePath The path of the audio file.
	 */
	private void playAudio(String filePath) {
		MediaPlayer mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(filePath);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
