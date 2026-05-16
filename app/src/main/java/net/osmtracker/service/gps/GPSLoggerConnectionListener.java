package net.osmtracker.service.gps;

import android.app.Activity;

public abstract class GPSLoggerConnectionListener extends Activity {
	public abstract void setGpsLogger(GPSLogger l);

	public abstract long getCurrentTrackId();
}
