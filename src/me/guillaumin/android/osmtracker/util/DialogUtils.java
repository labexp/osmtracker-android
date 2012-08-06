package me.guillaumin.android.osmtracker.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {

	/**
	 * Display a standard alert dialog 
	 * @param ctx
	 * @param msg
	 */
	public static void showErrorDialog(Context ctx, CharSequence msg) {
		new AlertDialog.Builder(ctx)
			.setTitle(android.R.string.dialog_alert_title)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(msg)
			.setCancelable(true)
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create().show();
	}
}
