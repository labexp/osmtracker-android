package net.osmtracker.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.NoteListAdapter;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.listener.EditNoteDialogOnClickListener;
import net.osmtracker.listener.EditWaypointDialogOnClickListener;

/**
 * Activity that lists the previous notes tracked by the user.
 *
 */
public class NoteList extends ListActivity {

	private static final String TAG = NoteList.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView listView = getListView();
		listView.setFitsSystemWindows(true);
		listView.setClipToPadding(false);
		listView.setPadding(0, 48, 0, 0);

        registerForContextMenu(listView);
	}

	@Override
	protected void onResume() {
		long trackId = getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		
		Cursor cursor = getContentResolver().query(TrackContentProvider.notesUri(trackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " desc");
		startManagingCursor(cursor);
		setListAdapter(new NoteListAdapter(NoteList.this, cursor));
		
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
	 * Handles the selection of a note from the list and opens an edit dialog.
	 * This dialog allows the user to update the note name (text)
	 *
	 * @param l The ListView where the item was clicked.
	 * @param v The view that was clicked.
	 * @param position The position of the clicked item.
	 * @param id The ID of the clicked note.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = ((CursorAdapter) getListAdapter()).getCursor();
		final DataHelper dataHelper = new DataHelper(l.getContext());
		LayoutInflater inflater = this.getLayoutInflater();

		// Inflate the note edit dialog layout
		final View editNoteDialog = inflater.inflate(R.layout.edit_note_dialog, null);
		final EditText editNoteName = editNoteDialog.findViewById(R.id.edit_note_et_name);

		Button buttonUpdate = editNoteDialog.findViewById(R.id.edit_note_button_update);
		Button buttonDelete = editNoteDialog.findViewById(R.id.edit_note_button_delete);
		Button buttonOSMUpload = editNoteDialog.findViewById(R.id.edit_note_button_osm_upload);
		Button buttonCancel = editNoteDialog.findViewById(R.id.edit_note_button_cancel);

		// Retrieve existing note name
		String oldName = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
		editNoteName.setText(oldName);
		editNoteName.setSelection(oldName.length());

		// Retrieve waypoint details
		final long trackId = cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_TRACK_ID));
		final String uuid = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_UUID));

		//TODO Add visual element to represent if the note was uploaded to OSM

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();

		// Update note text
		buttonUpdate.setOnClickListener(new EditNoteDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				String newName = editNoteName.getText().toString();
				dataHelper.updateNote(trackId, uuid, newName);
				alert.dismiss();
			}
		});

		// Delete waypoint
		buttonDelete.setOnClickListener(new EditNoteDialogOnClickListener(alert, cursor) {
			@Override
			public void onClick(View view) {
				new AlertDialog.Builder(NoteList.this)
						.setTitle(getString(R.string.delete_note_confirm_dialog_title))
						.setMessage(getString(R.string.delete_note_confirm_dialog_msg))
						.setPositiveButton(getString(R.string.delete_note_confirm_bt_ok), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dataHelper.deleteNote(uuid);
								cursor.requery();
								alert.dismiss();
								dialog.dismiss();
							}
						})
						.setNegativeButton(getString(R.string.delete_note_confirm_bt_cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
			}
		});

		// Upload note text to OpenStreetMap
		buttonOSMUpload.setOnClickListener(new EditNoteDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				uploadNoteToOSM(cursor);
			}
		});

		// Cancel button
		buttonCancel.setOnClickListener(new EditWaypointDialogOnClickListener(alert, null) {
			@Override
			public void onClick(View view) {
				alert.dismiss();
			}
		});

		alert.setView(editNoteDialog);
		alert.show();

		super.onListItemClick(l, v, position, id);
	}

    // Where the menu items get defined
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.note_contextmenu, menu);
    }

    // What happens when a menu item is selected
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Cursor cursor = ((CursorAdapter) getListAdapter()).getCursor();
        if (!cursor.moveToPosition(info.position)) return super.onContextItemSelected(item);

        // Menu options when you long press on a note
        switch (item.getItemId()) {
            case R.id.notelist_contextmenu_osm_note_upload:
				uploadNoteToOSM(cursor);
                return true;
        }
        return super.onContextItemSelected(item);
    }

	/**
	 * Extracts note data from the cursor and launches the OSM Note Upload activity.
	 * @param cursor The cursor positioned at the selected note.
	 */
	private void uploadNoteToOSM(Cursor cursor) {
		long noteId = cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID));
		String noteText = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
		double lat = cursor.getDouble(cursor.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE));
		double lon = cursor.getDouble(cursor.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE));

		Intent intent = new Intent(this, OpenStreetMapNotesUpload.class);
		intent.putExtra("noteId", noteId);
		intent.putExtra("noteContent", noteText);
		intent.putExtra("appName", getString(R.string.app_name));
		intent.putExtra("latitude", lat);
		intent.putExtra("longitude", lon);

		// Retrieve app version number
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			intent.putExtra("version", pi.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			// Log error or ignore
		}

		startActivity(intent);
	}
}
