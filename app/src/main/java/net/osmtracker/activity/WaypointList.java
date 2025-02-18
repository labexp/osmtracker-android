package net.osmtracker.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import androidx.core.content.FileProvider;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.WaypointListAdapter;
import net.osmtracker.listener.EditWaypointDialogOnClickListener;

import java.io.File;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

	private static final String TAG = WaypointList.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView listView = getListView();
		listView.setFitsSystemWindows(true);
		listView.setClipToPadding(false);
		listView.setPadding(0, 48, 0, 0);
	}

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

		Button buttonPreview = editWaypointDialog.findViewById(R.id.edit_waypoint_button_preview);
		Button buttonUpdate = editWaypointDialog.findViewById(R.id.edit_waypoint_button_update);
		Button buttonDelete = editWaypointDialog.findViewById(R.id.edit_waypoint_button_delete);
		Button buttonCancel = editWaypointDialog.findViewById(R.id.edit_waypoint_button_cancel);

		// Retrieve existing waypoint name
		String oldName = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
		editWaypointName.setText(oldName);
		editWaypointName.setSelection(oldName.length());

		// Retrieve waypoint details
		final long trackId = cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_TRACK_ID));
		final String uuid = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_UUID));
		final String link = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_LINK));

		final String filePath = (link != null) ? DataHelper.getTrackDirectory(trackId, l.getContext()) + "/" + link : null;
		File file = (filePath != null) ? new File(filePath) : null;

		if (file != null && file.exists()) {
			try {
				if (isImageFile(filePath) || isAudioFile(filePath)) {
					buttonPreview.setVisibility(View.VISIBLE);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error handling file: " + filePath, e);
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();

		// Preview button
		buttonPreview.setOnClickListener(new EditWaypointDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				if (filePath != null) {
					File file = new File(filePath);
					Uri fileUri = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ?
							FileProvider.getUriForFile(getApplicationContext(), DataHelper.FILE_PROVIDER_AUTHORITY, file) :
							Uri.fromFile(file);

					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (isImageFile(filePath)) {
						intent.setDataAndType(fileUri, DataHelper.MIME_TYPE_IMAGE);
					} else if (isAudioFile(filePath)) {
						intent.setDataAndType(fileUri, DataHelper.MIME_TYPE_AUDIO);
					}

					if (intent.resolveActivity(getPackageManager()) != null) {
						startActivity(intent);
					}
				}
				alert.dismiss();
			}
		});

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
				new AlertDialog.Builder(WaypointList.this)
						.setTitle(getString(R.string.delete_waypoint_confirm_dialog_title))
						.setMessage(getString(R.string.delete_waypoint_confirm_dialog_msg))
						.setPositiveButton(getString(R.string.delete_waypoint_confirm_bt_ok), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dataHelper.deleteWayPoint(uuid, filePath);
								cursor.requery();
								alert.dismiss();
								dialog.dismiss();
							}
						})
						.setNegativeButton(getString(R.string.delete_waypoint_confirm_bt_cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
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
		return path.endsWith(DataHelper.EXTENSION_JPG);
	}

	/**
	 * Checks if a given file path corresponds to an audio file.
	 *
	 * @param path The file path.
	 * @return True if the file is an audio file, false otherwise.
	 */
	private boolean isAudioFile(String path) {
		return path.endsWith(DataHelper.EXTENSION_3GPP);
	}

}
