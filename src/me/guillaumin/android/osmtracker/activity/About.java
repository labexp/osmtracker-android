package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Simply display the about screen.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class About extends Activity {

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
		
		((Button) findViewById(R.id.about_debug_info_button)).setOnClickListener(
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
									dialog.cancel();									
								}
							})
							.create().show();						
					}
				}
		);
	}
	
	private String getDebugInfo() {
		return "Environment.getExternalStorageDirectory: '"
				+ Environment.getExternalStorageDirectory().getAbsolutePath() + "'\n"
			+ "Environment.getExternalStorageState: '"
				+ Environment.getExternalStorageState() + "'\n"
			+ "Can write to external storage: "
				+ Boolean.toString(Environment.getExternalStorageDirectory().canWrite()) + "\n"
		;
	}

}
