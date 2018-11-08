package net.osmtracker.listener;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceManager;

import net.osmtracker.activity.TrackLogger;
import net.osmtracker.db.TrackContentProvider;

/**
 * Listener for standard waypoint tag button.
 * Sends an Intent to track waypoint. Waypoint name is the
 * label of the button.
 *
 * @author Nicolas Guillaumin
 *
 */
public class IncrementalWaypointOnclickListener implements OnClickListener {

    private long currentTrackId;
    private Context context;
    private final SharedPreferences prefs;

    public IncrementalWaypointOnclickListener(long trackId, Context context) {
        currentTrackId = trackId;
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onClick(View view) {
        Button button = (Button) view;
        Integer wpnum = Integer.parseInt(prefs.getString(OSMTracker.Preferences.KEY_INCREMENTAL_WAYPOINT_COUNTER,OSMTracker.Preferences.VAL_INCREMENTAL_WAYPOINT_COUNTER));
        String wptext = String.format(context.getString(R.string.incremental_waypoint_name_format), wpnum);
        // Send an intent to inform service to track the waypoint.
        Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
        intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
        intent.putExtra(OSMTracker.INTENT_KEY_NAME, wptext);
        view.getContext().sendBroadcast(intent);
        wpnum++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(OSMTracker.Preferences.KEY_INCREMENTAL_WAYPOINT_COUNTER,Integer.toString(wpnum));
        editor.commit();

        // Inform user that the waypoint was tracked
        Toast toast = Toast.makeText(view.getContext(), view.getContext().getResources().getString(R.string.tracklogger_tracked) + " " + wptext, Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.argb(200,255,0,0));
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
        toast.show();

    }

}
