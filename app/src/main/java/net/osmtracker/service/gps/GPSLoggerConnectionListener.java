package net.osmtracker.service.gps;

import android.content.Intent;
import android.app.Activity;

public interface GPSLoggerConnectionListener {
	void setGpsLogger(GPSLogger l);

	long getCurrentTrackId();

	/* Stuff supplied by Activity / AppCompatActivity */
	String getPackageName();

	void sendBroadcast(Intent intent);
}
