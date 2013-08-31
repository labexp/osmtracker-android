package me.guillaumin.android.osmtracker.view;

import java.util.UUID;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

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
	
	/**
	 * Id of the track the dialog will add this waypoint to
	 */
	private long wayPointTrackId;
	
	private Context context;

	public TextNoteDialog(Context context, long trackId) {
		super(context);

		this.context = context;
		this.wayPointTrackId = trackId;
		
		// Text edit control for user input
		input = new EditText(context);

		// default settings
		this.setTitle(R.string.gpsstatus_record_textnote);
		this.setCancelable(true);
		this.setView(input);

		this.setButton(context.getResources().getString(android.R.string.ok),  new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Track waypoint with user input text
				Intent intent = new Intent(OSMTracker.INTENT_UPDATE_WP);
				intent.putExtra(Schema.COL_TRACK_ID, TextNoteDialog.this.wayPointTrackId);
				intent.putExtra(OSMTracker.INTENT_KEY_NAME, input.getText().toString());
				intent.putExtra(OSMTracker.INTENT_KEY_UUID, TextNoteDialog.this.wayPointUuid);
				TextNoteDialog.this.context.sendBroadcast(intent);
			}
		});
		
		this.setButton2(context.getResources().getString(android.R.string.cancel),  new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// cancel the dialog
				dialog.cancel();	
			}
		});
		
		this.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// delete the waypoint because user canceled this dialog
				Intent intent = new Intent(OSMTracker.INTENT_DELETE_WP);
				intent.putExtra(OSMTracker.INTENT_KEY_UUID, TextNoteDialog.this.wayPointUuid);
				TextNoteDialog.this.context.sendBroadcast(intent);
			}
		});
		
	}
	
	/**
	 * @link android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		if(wayPointUuid == null){
			// there is no UUID set for the waypoint we're working on
			// so we need to generate a UUID and track this point
			wayPointUuid = UUID.randomUUID().toString();
			Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
			intent.putExtra(Schema.COL_TRACK_ID, wayPointTrackId);
			intent.putExtra(OSMTracker.INTENT_KEY_UUID, wayPointUuid);
			intent.putExtra(OSMTracker.INTENT_KEY_NAME, context.getResources().getString(R.string.gpsstatus_record_textnote));
			context.sendBroadcast(intent);
		}
		
		super.onStart();
	}

	/**
	 * resets values of this dialog
	 * such as the input fields text and the waypoints uuid
	 */
	public void resetValues(){
		wayPointUuid = null;
		input.setText("");
	}

	/**
	 * restoring values from the savedInstaceState 
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		String text = savedInstanceState.getString(KEY_INPUT_TEXT);
		if(text != null){
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
