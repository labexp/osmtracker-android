package net.osmtracker.listener;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;

import java.util.List;

public class EditWaypointOnDeleteListener implements View.OnClickListener {
	private Cursor cursor;
	protected AlertDialog alert;

	protected EditWaypointOnDeleteListener(AlertDialog alert, Cursor cu) {
		this.cursor = cu;
		this.alert = alert;
	}

	// public void onClick(DialogInterface dialog, int which){};
	public void onClick(View view){}

}
