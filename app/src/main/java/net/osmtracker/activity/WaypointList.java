package net.osmtracker.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.WaypointListAdapter;
import net.osmtracker.listener.ConfirmDeleteWaypointListener;
import net.osmtracker.listener.EditWaypointDialogOnClickListener;

/**
 * Activity that lists the previous waypoints tracked by the user.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class WaypointList extends ListActivity {

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


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor1 = ((CursorAdapter)getListAdapter()).getCursor();

		final DataHelper dataHelper = new DataHelper(l.getContext());

		LayoutInflater inflater = this.getLayoutInflater();

		final View edit_waypoint_dialog = inflater.inflate(R.layout.edit_waypoint_dialog,null);
		final EditText edit_waypoint_et_name = edit_waypoint_dialog.findViewById(R.id.edit_waypoint_et_name);

		Button button_edit_waypoint_ok = edit_waypoint_dialog.findViewById(R.id.edit_waypoint_button_ok);
		Button button_delete_waypoint = edit_waypoint_dialog.findViewById(R.id.edit_waypoint_button_delete);
		Button button_edit_waypoint_cancel = edit_waypoint_dialog.findViewById(R.id.edit_waypoint_button_cancel);

		String old_text = cursor1.getString(cursor1.getColumnIndex("name"));

		edit_waypoint_et_name.setText(old_text);
		edit_waypoint_et_name.setSelection(old_text.length());

		final long trackId = cursor1.getLong(cursor1.getColumnIndex("track_id"));
		final String uuid = cursor1.getString(cursor1.getColumnIndex("uuid"));
		final String name = edit_waypoint_et_name.getText().toString();
		final String link = cursor1.getString(cursor1.getColumnIndex("link"));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);

		AlertDialog alert = builder.create();

		button_edit_waypoint_ok.setOnClickListener(
			new EditWaypointDialogOnClickListener(alert, null) {
				@Override
				public void onClick(View view) {
					String new_text = edit_waypoint_et_name.getText().toString();
					dataHelper.updateWayPoint(trackId, uuid, new_text, link);
					this.alert.dismiss();
				}
			}
		);

		button_delete_waypoint.setOnClickListener(
			new EditWaypointDialogOnClickListener(alert, cursor1) {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder confirm_delete_dialog_builder = new AlertDialog.Builder(alert.getContext());
					confirm_delete_dialog_builder.setCancelable(true);
					confirm_delete_dialog_builder.setTitle(R.string.delete_waypoint_confirm_dialog_title);
					confirm_delete_dialog_builder.setPositiveButton(R.string.delete_waypoint_confirm_bt_ok, new ConfirmDeleteWaypointListener(alert) {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dataHelper.deleteWayPoint(uuid);
							cursor1.requery();
							alert.dismiss();
						}
					});
					// Cancel does just nothing, but the listener must be there, so the button is shown
					confirm_delete_dialog_builder.setNegativeButton(R.string.delete_waypoint_confirm_bt_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							alert.dismiss();
						}
					});
					confirm_delete_dialog_builder.create();
					confirm_delete_dialog_builder.show();
				}
			}
		);

		button_edit_waypoint_cancel.setOnClickListener(
				new EditWaypointDialogOnClickListener(alert, null) {
					@Override
					public void onClick(View view) {
						this.alert.dismiss();
					}
				}
		);


		alert.setView(edit_waypoint_dialog);
		alert.show();

		super.onListItemClick(l, v, position, id);
	}

}
