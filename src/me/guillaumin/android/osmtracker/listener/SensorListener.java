package me.guillaumin.android.osmtracker.listener;

import java.text.DecimalFormat;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

public class SensorListener implements SensorEventListener {
	private static SensorManager sensorService;
	public float[] gravity = null;
	public float[] geomag = null;
	public float[] inR = new float[9];
	public float[] outR = new float[9];
	public float[] I = new float[9];
	public float[] orientVals = new float[3];
	public float orAzimuth, orPitch, orRoll;
	public float azimuth, pitch, roll;
	int accuracy, orAccuracy;
	static float rad2deg = 180.0f/3.141592653589793f;
	Activity activity;

	/**
	 * Formatter for heading display.
	 */
	private final static DecimalFormat HEADING_FORMAT = new DecimalFormat("0");

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		Log.v("SensorListener", "Accuracy changed: sensor:" + arg0 + ", accuracy: " + arg1);
	}

	public void onSensorChanged(SensorEvent event) {
		// If the sensor data is unreliable return
		// in fact I find that accuracy is always UNRELIABLE. So I skip this here
//	   if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
//		   //Log.w("GPSLogger", "sensor reliability " + event.accuracy);.
//		   return;
//	   }
//		 Log.v("GPSLogger", "sensor reliable");
		// Gets the value of the sensor that has been changed
		switch (event.sensor.getType()){  
	    case Sensor.TYPE_ACCELEROMETER:
		   
		   gravity = event.values.clone();
		   //Log.v("GPSLogger","gravity update " + gravity);
		   break;
	   case Sensor.TYPE_MAGNETIC_FIELD:
		   geomag = event.values.clone();
		   //Log.v("GPSLogger","geomag update " + geomag);
		   break;
       case Sensor.TYPE_ORIENTATION:
           orAzimuth = event.values[0];
           orPitch = event.values[1];
           orRoll = event.values[2];
           orAccuracy = event.accuracy;
           break;

	   }

//	   // If gravity and geomag have values then find rotation matrix
//	   if (gravity != null && geomag != null){
//
//		   // checks that the rotation matrix is found
//		   boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
//		   if (success){
//
//			    // Re-map coordinates so y-axis comes out of camera
//			    SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, 
//			    SensorManager.AXIS_Z, outR);
//
//			    // Finds the Azimuth and Pitch angles of the y-axis with 
//			    // magnetic north and the horizon respectively
//				SensorManager.getOrientation(outR, orientVals);
//				azimuth = orientVals[0]*rad2deg;
//				pitch = orientVals[1]*rad2deg;
//				roll = orientVals[2]*rad2deg;
//				//Log.v("GPSLogger","new azimuth: "+azimuth+", pitch: "+pitch+", roll: "+roll);
//
//				TextView tvAccuracy = (TextView) activity.findViewById(R.id.gpsstatus_record_tvHeading);
//				tvAccuracy.setText(""  + HEADING_FORMAT.format(orAzimuth) +  "["+orAccuracy+"])" + activity.getResources().getString(R.string.various_heading_unit));
//
////				tvAccuracy.setText("" + HEADING_FORMAT.format(azimuth) + "["+accuracy+"]" + " (" + HEADING_FORMAT.format(orAzimuth) +  "["+orAccuracy+"])" + activity.getResources().getString(R.string.various_heading_unit));
//		   }   
//	} else {
//			TextView tvAccuracy = (TextView) activity.findViewById(R.id.gpsstatus_record_tvHeading);
//			tvAccuracy.setText(activity.getResources().getString(R.string.various_heading_unknown));		   
//	   }
		TextView tvAccuracy = (TextView) activity.findViewById(R.id.gpsstatus_record_tvHeading);
		tvAccuracy.setText(""  + HEADING_FORMAT.format(orAzimuth) +  "["+orAccuracy+"]" + activity.getResources().getString(R.string.various_heading_unit));
	
	 }
	public boolean register(Activity activity){
		//register for Orientation updates
		this.activity = activity;
	    sensorService = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
	    
	    Sensor accelSens = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    Sensor magSens = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    Sensor orSens = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	    
	    if (accelSens != null && magSens != null) {
		    sensorService.registerListener(this, 
		    		accelSens,
		    		SensorManager.SENSOR_DELAY_NORMAL);

		    sensorService.registerListener(this, 
		    		magSens,
		    		SensorManager.SENSOR_DELAY_NORMAL);
		    
		    sensorService.registerListener(this, orSens, SensorManager.SENSOR_ORIENTATION);

		    Log.i("GPSLogger", "Registerered for magnetic and acceleration Sensor");
			Log.v("SensorListener","registerd!");
		    return true;

	    } else {
	    	Log.w("GPSLogger", "either magnetic or oritentation sensor not found");
	    	geomag = null;
	    	gravity = null;
	    	
	    	unregister();
	    	return false;
	    }

	}
	public void unregister() {
		// stop sensors TODO: is this good if sensors registration failed?
		if (sensorService != null) { 
			sensorService.unregisterListener(this);
			sensorService = null;
			Log.v("SensorListener","unregisterd");
		}
	}
};
