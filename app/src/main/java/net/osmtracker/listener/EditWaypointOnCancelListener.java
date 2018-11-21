package net.osmtracker.listener;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;

import java.util.List;

public class EditWaypointOnCancelListener implements DialogInterface.OnClickListener {
    public Cursor cursor;

    public EditWaypointOnCancelListener(Cursor cu) {
        this.cursor = cu;
    }

    // public void onClick(DialogInterface dialog, int which){};
    public void onClick(DialogInterface dialog, int which){};

}
