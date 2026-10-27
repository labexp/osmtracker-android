package net.osmtracker.service.gps;

import android.view.View;
import android.content.Intent;
import android.app.Activity;

public interface GPSLoggerConnectionListener {
	void setGpsLogger(GPSLogger l);

	long getCurrentTrackId();

	default void setEnabledActionButtons(boolean enabled) { }

	GPSLogger getGpsLogger();
	
	/* Stuff supplied by Activity / AppCompatActivity */
	String getPackageName();

	void sendBroadcast(Intent intent);

	<T extends View> T findViewById (int id);
}
