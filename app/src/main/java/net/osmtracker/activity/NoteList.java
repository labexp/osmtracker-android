package net.osmtracker.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmtracker.R;
import net.osmtracker.adapter.NoteAdapter;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;

import java.util.Objects;

public class NoteList extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

	private static final String TAG = NoteList.class.getSimpleName();

	private NoteAdapter adapter;
	private long trackId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notelist);

		trackId = Objects.requireNonNull(getIntent().getExtras())
				.getLong(TrackContentProvider.Schema.COL_TRACK_ID);

		RecyclerView recyclerView = findViewById(R.id.notelist_rv);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		adapter = new NoteAdapter(this);
		recyclerView.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshData();
	}

	private void refreshData() {

		Cursor cursor = getContentResolver().query(
				TrackContentProvider.notesUri(trackId),null, null, null,
				TrackContentProvider.Schema.COL_TIMESTAMP + " desc"
		);
		adapter.swapCursor(cursor);
	}


	@Override
	public void onNoteClick(long trackId, long noteId, String uuid, String name) {
		final DataHelper dataHelper = new DataHelper(this);
		LayoutInflater inflater = getLayoutInflater();

		// Inflate the note edit dialog layout
		final View editNoteDialog = inflater.inflate(R.layout.edit_note_dialog, null);
		final EditText editNoteName = editNoteDialog.findViewById(R.id.edit_note_et_name);

		Button buttonUpdate = editNoteDialog.findViewById(R.id.edit_note_button_update);
		Button buttonDelete = editNoteDialog.findViewById(R.id.edit_note_button_delete);
		Button buttonOSMUpload = editNoteDialog.findViewById(R.id.edit_note_button_osm_upload);
		Button buttonCancel = editNoteDialog.findViewById(R.id.edit_note_button_cancel);

		// Set existing note name
		editNoteName.setText(name);
		editNoteName.setSelection(name.length());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		final AlertDialog alert = builder.create();

		// Update note text
		buttonUpdate.setOnClickListener(v -> {
			String newName = editNoteName.getText().toString();
			dataHelper.updateNote(trackId, uuid, newName);
			refreshData();
			alert.dismiss();
		});

		// Delete note
		buttonDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
				.setTitle(R.string.delete_note_confirm_dialog_title)
				.setMessage(R.string.delete_note_confirm_dialog_msg)
				.setPositiveButton(R.string.delete_note_confirm_bt_ok, (dialog, which) -> {
					dataHelper.deleteNote(uuid);
					refreshData();
					alert.dismiss();
				})
				.setNegativeButton(R.string.delete_note_confirm_bt_cancel,
						(dialog, which) -> dialog.dismiss())
				.show());

		// Upload note text to OpenStreetMap
		buttonOSMUpload.setOnClickListener(v -> {
			uploadNoteToOSM(noteId);
			alert.dismiss();
		});

		// Cancel button
		buttonCancel.setOnClickListener(v -> alert.dismiss());

		alert.setView(editNoteDialog);
		alert.show();
	}

	/**
	 * Extracts note data from DB and launches the OSM Note Upload activity.
	 */
	private void uploadNoteToOSM(long noteId) {
		// Query the specific note to get latest Lat/Lon
		Cursor cursor = getContentResolver().query(
				TrackContentProvider.noteUri(noteId),
				null,null,null,null);

		if (cursor != null && cursor.moveToFirst()) {
			String noteText = cursor.getString(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_NAME));
			double lat = cursor.getDouble(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LATITUDE));
			double lon = cursor.getDouble(
					cursor.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LONGITUDE));

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
				// Ignore
				Log.d(TAG, "Package name not found", e);
			}

			cursor.close();
			startActivity(intent);
		}
	}
}
