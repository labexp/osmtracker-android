package net.osmtracker.util;

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

	/**
	 * Displays a standard success alert dialog.
	 *
	 * @param context The application context.
	 * @param message The message to display in the dialog.
	 */
	public static void showSuccessDialog(Context context, int message) {
		if (context == null) {
			throw new IllegalArgumentException("Context cannot be null");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Success")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(message)
				.setCancelable(true)
				.setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());

		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
