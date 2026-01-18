package net.osmtracker.listener;

import android.app.AlertDialog;
import android.database.Cursor;
import android.view.View;

/**
 * Class that implements an OnClickListener to display an edit note dialog.
 */
public class EditNoteDialogOnClickListener implements View.OnClickListener {

	private Cursor cursor;

	protected AlertDialog alert;

	protected EditNoteDialogOnClickListener(AlertDialog alert, Cursor cu) {
		this.cursor = cu;   // Assigns the received cursor to the class attribute
		this.alert = alert; // Assigns the received alert to the class attribute
	}

	@Override
	public void onClick(View view) {
	}
}
