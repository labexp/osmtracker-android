package net.osmtracker.listener;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import net.osmtracker.db.TrackContentProvider;

/**
 * Listener for standard waypoint tag button.
 * Sends an Intent to track waypoint. Waypoint name is the
 * label of the button.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class TagButtonOnClickListener implements OnClickListener {

	private long currentTrackId;
	
	public TagButtonOnClickListener(long trackId) {
		currentTrackId = trackId;
	}
	
	@Override
	public void onClick(View view) {
		Button button = (Button) view;
		String label = button.getText().toString().replaceAll("\n", " ");

		// Send an intent to inform service to track the waypoint.
		Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
		intent.putExtra(OSMTracker.INTENT_KEY_NAME, label);

		String packageName = view.getContext().getPackageName();
		intent.setPackage(packageName);

		view.getContext().sendBroadcast(intent);
		
		// Inform user that the waypoint was tracked
		Toast.makeText(view.getContext(), view.getContext().getResources().getString(R.string.tracklogger_tracked) + " " + label, Toast.LENGTH_SHORT).show();

	}

}
