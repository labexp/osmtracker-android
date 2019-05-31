package net.osmtracker.listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;

public class ConfirmDeleteWaypointListener implements DialogInterface.OnClickListener {
	protected AlertDialog alert;

	protected ConfirmDeleteWaypointListener(AlertDialog alert) {
		this.alert = alert;
	}

	// public void onClick(DialogInterface dialog, int which){};
	public void onClick(DialogInterface dialog, int which){}

}
