package net.osmtracker.activity;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DatabaseHelper;
import net.osmtracker.db.ExportDatabaseTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

/**
 * Simply display the about screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class About extends Activity {

	public static final int DIALOG_EXPORT_DB = 0;
	public static final int DIALOG_EXPORT_DB_COMPLETED = 1;

	private ProgressDialog exportDbProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		// Retrieve app. version number
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			((TextView) findViewById(R.id.about_version)).setText(pi.versionName);
		} catch (NameNotFoundException nnfe) { 
			// Should not occur
		}

		findViewById(R.id.about_debug_info_button).setOnClickListener(
				new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						new AlertDialog.Builder(v.getContext())
							.setTitle(R.string.about_debug_info)
							.setMessage(getDebugInfo())
							.setCancelable(true)
							.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.create().show();						
					}
				}
		);

		findViewById(R.id.about_export_db_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View view) {
						showDialog(DIALOG_EXPORT_DB);

						File dbFile = getDatabasePath(DatabaseHelper.DB_NAME);
						File targetFolder = new File(
								Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
								//Environment.getExternalStorageDirectory(),
								PreferenceManager.getDefaultSharedPreferences(About.this).getString(
										OSMTracker.Preferences.KEY_STORAGE_DIR,
										OSMTracker.Preferences.VAL_STORAGE_DIR));

						new ExportDatabaseTask(About.this, targetFolder)
								.execute(dbFile);
					}
				}
		);
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch(id) {
			case DIALOG_EXPORT_DB:
				exportDbProgressDialog = new ProgressDialog(this);
				exportDbProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				exportDbProgressDialog.setIndeterminate(false);
				exportDbProgressDialog.setProgress(0);
				exportDbProgressDialog.setMax(100);
				exportDbProgressDialog.setCancelable(false);
				exportDbProgressDialog.setMessage(getResources().getString(R.string.about_exporting_db));
				exportDbProgressDialog.show();
				return exportDbProgressDialog;
			case DIALOG_EXPORT_DB_COMPLETED:
				new AlertDialog.Builder(this)
						.setTitle(R.string.about_export_db)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setMessage(getString(R.string.about_export_db_result, args.getString("result")))
						.setCancelable(true)
						.setNeutralButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
						})
						.create()
						.show();
		}

		return null;
	}

	public ProgressDialog getExportDbProgressDialog() {
		return exportDbProgressDialog;
	}

	private String getDebugInfo() {
		File externalStorageDir = this.getExternalFilesDir(null);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String exportDirectoryNameInPreferences = preferences.getString(
				OSMTracker.Preferences.KEY_STORAGE_DIR,	OSMTracker.Preferences.VAL_STORAGE_DIR);
		File baseExportDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				exportDirectoryNameInPreferences);
		return "External Storage Directory: '" + externalStorageDir + "'\n"
				+ "External Storage State: '"  + Environment.getExternalStorageState() + "'\n"
				+ "Can write to external storage: "
				+ Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) + "\n"
				+ "Export External Public Storage Directory: '"
				+ baseExportDirectory + "'\n";
	}

}
