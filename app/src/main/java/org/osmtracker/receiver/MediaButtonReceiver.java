package org.osmtracker.receiver;

import org.osmtracker.activity.TrackLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaButtonReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent intent2 = new Intent(context, TrackLogger.class);
		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(intent2.putExtra("mediaButton", Boolean.TRUE));
	}

}
