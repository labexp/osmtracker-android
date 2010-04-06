package me.guillaumin.android.osmtracker.listener;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 * Manages text note button.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class TextNoteOnClickListener implements OnClickListener {

	@Override
	public void onClick(final View v) {
		// Text edit control for user input
		final EditText input = new EditText(v.getContext());
	
		// Create a dialog for text input
		new AlertDialog.Builder(v.getContext())
		.setTitle(v.getResources().getString(R.string.gpsstatus_record_textnote))
		.setCancelable(true)
		.setView(input)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Track waypoint with user input text
				String value = input.getText().toString();
				Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
				intent.putExtra(OSMTracker.INTENT_KEY_NAME, value);
				v.getContext().sendBroadcast(intent);
			}
		}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();				
			}
		}).create().show();
	}

}
