package net.osmtracker.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;

import java.util.UUID;

public class TextNoteDialog extends AlertDialog {
	
	/**
	 * bundle key for text of input field
	 */
	private static final String KEY_INPUT_TEXT = "INPUT_TEXT";

	/**
	 * bundle key for waypoint uuid
	 */
	private static final String KEY_WAYPOINT_UUID = "WAYPOINT_UUID";
	
	/**
	 * bundle key for waypoints track id
	 */
	private static final String KEY_WAYPOINT_TRACKID = "WAYPOINT_TRACKID";
	
	/**
	 * the input box displayed in the dialog
	 */
	EditText input = null;
	
	/**
	 * Unique identifier of the waypoint this dialog working on
	 */
	private String wayPointUuid = null;

	// Unique identifier of the note this dialog working on
	private String noteUuid = null;

	/**
	 * Id of the track the dialog will add this waypoint to
	 */
	private long wayPointTrackId;

	/**
	 * Id of the track the dialog will add this OSM Text note to
	 */
	private long noteTrackId;

	private Context context;

	public TextNoteDialog(Context context, long trackId) {
		super(context);

		this.context = context;
		this.wayPointTrackId = trackId;
		this.noteTrackId = trackId;

		// Text edit control for user input
		input = new EditText(context);

		// default settings
		this.setTitle(R.string.gpsstatus_record_textnote);
		this.setCancelable(true);
		this.setView(input);

		this.setButton(DialogInterface.BUTTON_POSITIVE,
				context.getString(android.R.string.ok),
				(dialog, which) -> {


			// CHECK Prefs to select if note is added as trackpoint, note or both
			String noteText = input.getText().toString();

			// Track waypoint with user input text
			Intent intent = new Intent(OSMTracker.INTENT_UPDATE_WP);
			intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, wayPointTrackId);
			intent.putExtra(OSMTracker.INTENT_KEY_NAME, input.getText().toString());
			intent.putExtra(OSMTracker.INTENT_KEY_UUID, TextNoteDialog.this.wayPointUuid);
			intent.setPackage(getContext().getPackageName());
			context.sendBroadcast(intent);

			// For testing, this will be both (tp and note) case pref.
			Intent noteIntent = new Intent(OSMTracker.INTENT_UPDATE_NOTE);
			noteIntent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, noteTrackId);
			noteIntent.putExtra(OSMTracker.INTENT_KEY_NAME, noteText);
			noteIntent.putExtra(OSMTracker.INTENT_KEY_UUID, wayPointUuid);
			noteIntent.setPackage(getContext().getPackageName());
			context.sendBroadcast(noteIntent);

		});
		
		this.setButton(DialogInterface.BUTTON_NEGATIVE,
				context.getResources().getString(android.R.string.cancel),
				(dialog, which) -> {
					// cancel the dialog
					dialog.cancel();
				});
		
		this.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// delete the waypoint because user canceled this dialog
				Intent intent = new Intent(OSMTracker.INTENT_DELETE_WP);
				intent.putExtra(OSMTracker.INTENT_KEY_UUID, TextNoteDialog.this.wayPointUuid);
				intent.setPackage(getContext().getPackageName());
				context.sendBroadcast(intent);
			}
		});
		
	}
	
	/**
	 * @link android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		if (wayPointUuid == null) {
			// there is no UUID set for the waypoint we're working on
			// so we need to generate a UUID and track this point
			wayPointUuid = UUID.randomUUID().toString();
			Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
			intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, wayPointTrackId);
			intent.putExtra(OSMTracker.INTENT_KEY_UUID, wayPointUuid);
			intent.putExtra(OSMTracker.INTENT_KEY_NAME, context.getResources().getString(R.string.gpsstatus_record_textnote));
			intent.setPackage(getContext().getPackageName());
			context.sendBroadcast(intent);
		}

		if (noteUuid == null) {

			// there is no UUID set for the note we're working on
			// so we need to generate a UUID and track this note
			noteUuid = UUID.randomUUID().toString();
			Intent noteIntent = new Intent(OSMTracker.INTENT_TRACK_NOTE);
			noteIntent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, noteTrackId);
			noteIntent.putExtra(OSMTracker.INTENT_KEY_UUID, noteUuid);
			noteIntent.putExtra(OSMTracker.INTENT_KEY_NAME, context.getResources().getString(R.string.gpsstatus_record_textnote));
			noteIntent.setPackage(getContext().getPackageName());
			context.sendBroadcast(noteIntent);


		}

		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		
		super.onStart();
	}

	/**
	 * resets values of this dialog
	 * such as the input fields text and the waypoints uuid
	 */
	public void resetValues() {
		wayPointUuid = null;
		input.setText("");
	}

	/**
	 * restoring values from the savedInstanceState
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		String text = savedInstanceState.getString(KEY_INPUT_TEXT);
		if (text != null) {
			input.setText(text);
		}
		wayPointUuid = savedInstanceState.getString(KEY_WAYPOINT_UUID);
		wayPointTrackId = savedInstanceState.getLong(KEY_WAYPOINT_TRACKID);
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * save values to bundle that we'll need later
	 */
	@Override
	public Bundle onSaveInstanceState() {
		Bundle extras = super.onSaveInstanceState();
		extras.putString(KEY_INPUT_TEXT, input.getText().toString());
		extras.putLong(KEY_WAYPOINT_TRACKID, wayPointTrackId);
		extras.putString(KEY_WAYPOINT_UUID, wayPointUuid);
		return extras;
	}
}
