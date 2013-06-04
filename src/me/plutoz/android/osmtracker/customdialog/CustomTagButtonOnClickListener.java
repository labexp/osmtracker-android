package me.plutoz.android.osmtracker.customdialog;

import me.guillaumin.android.osmtracker.R;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomTagButtonOnClickListener implements OnClickListener {
	
	CustomDialogSettings settings;
	long currentTrackId;
	
	
	public CustomTagButtonOnClickListener(CustomDialogSettings settings, long currentTrackId) {
		super();
		this.settings = settings;
		this.currentTrackId = currentTrackId;
	}

	@Override
	public void onClick(View v) {			
		CustomDialog cd = new CustomDialog(v.getContext(), settings, currentTrackId);		
		cd.show();
	}

}
