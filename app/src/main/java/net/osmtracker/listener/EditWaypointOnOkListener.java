package net.osmtracker.listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;

public class EditWaypointOnOkListener implements View.OnClickListener {
	protected AlertDialog alert;

	protected EditWaypointOnOkListener(AlertDialog al) {
		this.alert = al;
	}

	// public void onClick(DialogInterface dialog, int which){};
	public void onClick(View view){}

}
