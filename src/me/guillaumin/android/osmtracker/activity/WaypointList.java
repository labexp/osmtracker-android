package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.WaypointListAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

	/**
	 * Dialog for editing waypoint name when clicked in list.
	 * See {@link #onPrepareDialog(int, android.app.Dialog, android.os.Bundle)}
	 */
	private static final int DIALOG_WP_NAME = 1;

	/**
	 * Bundle state key for waypoint name during edit.
	 */
	public static final String STATE_WP_NAME = "wpName";

	/**
	 * Bundle state key for long waypoint id during edit.
	 */
	public static final String STATE_WP_ID = "wpId";

	/**
	 * Track ID containing these waypoints, from intent extras {@link Schema#COL_TRACK_ID}
	 */
	private long trackId;

	/**
	 * Waypoint ID when editing WP name in {@link #DIALOG_WP_NAME}.
	 */
	private transient long wpId;

	/**
	 * Saved WP name in {@link #DIALOG_WP_NAME}.  Edited in {@link #wpEdit}.
	 */
	private transient String wpName;

	/**
	 * EditText for WP name in {@link #DIALOG_WP_NAME}, from {@link #wpName}.
	 * Null until {@link #onCreateDialog(int)} is called.
	 * Updated in {@link #onPrepareDialog(int, Dialog)}.
	 */
	private transient EditText wpEdit;

	@Override
	protected void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		if ((savedState != null) && savedState.containsKey(STATE_WP_NAME)) {
			wpName = savedState.getString(STATE_WP_NAME);
			wpId = savedState.getLong(STATE_WP_ID);
		}
	}

	@Override
	protected void onResume() {
		trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		
		Cursor cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
				null, null, null, Schema.COL_TIMESTAMP + " asc");
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
	 * After editing or deleting a waypoint, create a new cursor to refresh the displayed list item data.
	 */
	private void requery() {
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		Cursor cursor = adapter.getCursor();
		stopManagingCursor(cursor);
		cursor.close();
		cursor = getContentResolver().query(TrackContentProvider.waypointsUri(trackId),
			null, null, null, Schema.COL_TIMESTAMP + " asc");
		startManagingCursor(cursor);
		adapter.changeCursor(cursor);
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick (ListView l, View v, final int position, final long id) {
		Cursor c = ((CursorAdapter) l.getAdapter()).getCursor();
		if (c.moveToPosition(position)) {
			wpId = c.getLong(c.getColumnIndex(Schema.COL_ID));
			wpName = c.getString(c.getColumnIndex(Schema.COL_NAME));
			showDialog(DIALOG_WP_NAME);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		final Dialog dia;
		switch (id) {

		case DIALOG_WP_NAME:
			{
				AlertDialog.Builder wpnameBuilder = new AlertDialog.Builder(this);
				wpnameBuilder.setTitle(R.string.wplist_wp_name);
				if (wpEdit == null)
					wpEdit = new EditText(this);
				// EditText wpEdit contents are set from wpName field in onPrepareDialog
				wpnameBuilder.setView(wpEdit);

				wpnameBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						wpName = wpEdit.getText().toString().trim();
						if (wpName.length() == 0)  // TODO delete if 0 length?
							return;

						DataHelper dataHelper = new DataHelper(WaypointList.this);
						dataHelper.updateWayPoint(trackId, wpId, wpName);

						// Need a new cursor to refresh the displayed list item data
						requery();
					}
				});

				wpnameBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { }
				});

				dia = wpnameBuilder.create();
				break;
			}

		default:
			dia = null;
		}

		return dia;
	}

	/**
	 * On older android versions, update {@link #wpEdit} from {@link #wpName} before showing {@link #DIALOG_WP_NAME}.
	 */
	protected void onPrepareDialog(final int id, Dialog dialog) {
		onPrepareDialog(id, dialog, null);
	}

	/**
	 * Update {@link #wpEdit} from {@link #wpName} before showing {@link #DIALOG_WP_NAME}.
	 */
	@Override
	protected void onPrepareDialog(final int id, Dialog dialog, Bundle unused) {
		if ((id != DIALOG_WP_NAME) || (wpEdit == null))
		{
			super.onPrepareDialog(id, dialog, unused);
			return;
		}

		if (wpName != null)
			wpEdit.setText(wpName);
		else
			wpEdit.setText("");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		// Save waypoint name if editing
		if (wpName != null) {
			if (wpEdit != null)
				wpName = wpEdit.getText().toString().trim();
			outState.putString(STATE_WP_NAME, wpName);
			outState.putLong(STATE_WP_ID, wpId);
		}
		else if (outState.containsKey(STATE_WP_NAME)) {
			outState.remove(STATE_WP_NAME);
		}

		super.onSaveInstanceState(outState);
	}

}
