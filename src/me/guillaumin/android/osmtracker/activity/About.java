package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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
	}

}
