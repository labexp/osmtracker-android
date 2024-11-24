package net.osmtracker.listener;

import java.text.DecimalFormat;

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

/**
 * Listener for sensors. In particular for the acceleration and magnetic sensors to provide compass
 * heading.
 * Register the listener with your context using the register/unregister functions
 * most recent reading from the sensor is always available from azimuth, pitch, roll, accuracy and valid fields
 * 
 * @author Christoph Gohle
 *
 */
public class SensorListener implements SensorEventListener {

	private SensorManager sensorService;

	/**
	 * value of the last accelerometer sensor event
	 */
	private float[] gravity = null;
	
	/**
	 * accuracy of the last accelerometer sensor event
	 */
	private int gravAccuracy = 0;
	
	/**
	 * value of the last magnetic field sensor event
	 */
	private float[] geomag = null;
	
	/**
	 * accuracy of the last magnetic field sensor event
	 */
	private int magAccuracy = 0;
	
	/**
	 * Azimuth, pitch and roll (as in SensorManager.SENSOR_ACCURACY_* of the last sensor event) 
	 */
	private float azimuth, pitch, roll;
	
	/**
	 * Accuracy (as in SensorManager.SENSOR_ACCURACY_* of the last sensor event
	 */
	private int accuracy;
	
	/**
	 * true if current azimuth, pitch and roll are valid (from the last sensor event)
	 */
	private boolean valid = false;
	
	/**
	 * conversion from rad to degrees
	 */
	public static final float RAD_TO_DEG = 180.0f/3.141592653589793f;

	private float[] inR = new float[9];
	private float[] outR = new float[9];
	private float[] I = new float[9];
	private float[] orientVals = new float[3];
	
	private Activity activity = null;
	private Context context = null;
	

	/**
	 * Formatter for heading display.
	 */
	private final static DecimalFormat HEADING_FORMAT = new DecimalFormat("0");

	/**
	 * TAG for this class
	 */
	private static final String TAG = SensorListener.class.getSimpleName();
	
	/**
	 * use ORIENTATION sensor type as default
	 */
	private final static boolean USE_ORIENTATION_AS_DEFAULT = true;
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.v(TAG, "Accuracy changed: sensor:" + sensor + ", accuracy: " + accuracy);
	}

	public void onSensorChanged(SensorEvent event) {
		boolean update_rotation = false;

		switch (event.sensor.getType()){  
	
			case Sensor.TYPE_ACCELEROMETER:
			   
			    gravity = event.values.clone();
			    gravAccuracy = event.accuracy;
			    Log.v(TAG, "gravitation sensor accurcay: "+gravAccuracy);
			    update_rotation = true;
			    break;
		    case Sensor.TYPE_MAGNETIC_FIELD:
			    geomag = event.values.clone();
			    magAccuracy = event.accuracy;
			    Log.v(TAG, "magnetic sensor accurcay: "+magAccuracy);
			    update_rotation = true;
			    break;
	        case Sensor.TYPE_ORIENTATION:
	            azimuth = event.values[0];
	            pitch = event.values[1];
	            roll = event.values[2];
	            accuracy = event.accuracy;
	            valid = true;
	            break;
	    }
		
		if (update_rotation) {
			valid = calcOrientation();
		} else {
			// case for orientation event: already done
		}
		Log.v(TAG,"new azimuth:  "+azimuth+", pitch: "+pitch+", roll: "+roll+", accuracy: "+accuracy+", valid: "+valid);			
		
		if (activity!=null) {
			TextView tvHeading = (TextView) activity.findViewById(R.id.gpsstatus_record_tvHeading);
			if (tvHeading != null) {
				if (valid) {
					int color = Color.RED;
					switch (accuracy) {
					case SensorManager.SENSOR_STATUS_UNRELIABLE:
						color = Color.RED;
						break;
					case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
						color = Color.MAGENTA;
						break;
					case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
						color = Color.YELLOW;
						break;
					case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
						color = Color.GREEN;
						break;
					}
					tvHeading.setTextColor(color);
					tvHeading.setText(activity.getResources().getString(R.string.various_heading_display)
							.replace("{0}", HEADING_FORMAT.format(azimuth)));
				} else {
					tvHeading.setTextColor(Color.GRAY);
					tvHeading.setText(activity.getResources().getString(R.string.various_heading_unknown));
				}
			}
		}
	}

	private boolean calcOrientation() {
		// If gravity and geomag have values then find rotation matrix
		boolean success = false;
		if (gravity != null && geomag != null){
	
			// checks that the rotation matrix is found
			success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
			if (success){
			    // Re-map coordinates so y-axis comes out of camera
				SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, 
				SensorManager.AXIS_Z, outR);
				
				// Finds the Azimuth and Pitch angles of the y-axis with 
				// magnetic north and the horizon respectively
				SensorManager.getOrientation(outR, orientVals);
				azimuth = orientVals[0]*RAD_TO_DEG;
				pitch = orientVals[1]*RAD_TO_DEG;
				roll = orientVals[2]*RAD_TO_DEG;
				if (magAccuracy<gravAccuracy) {
					accuracy = magAccuracy;
				} else {
					accuracy = gravAccuracy;
				}
								
		   	} else {
		   	}
		}
		return success;
	}
	
	/**
	 * register the listener with default orientation sensors 
	 * @param activity activity that will be updated from this listener
	 * @return true on success
	 */
	public boolean register(Activity activity) {
		this.activity = activity;
		return register(activity, USE_ORIENTATION_AS_DEFAULT);
	}
	
	/**
	 *  register the listener with default orientatin sensor
	 * @param 
	 * @return
	 */
	public boolean register(Context context){
		return register(context, USE_ORIENTATION_AS_DEFAULT);
	}

	/**
	 * register the listener with orientation sensors 
	 * @param context context that will be used to obtain the SensorManager
	 * @param use_orientation use (deprecated) orientation sensor if true. Otherwise use the getOrientation method
	 * @return true on success
	 */
	public boolean register(Context context, boolean use_orientation) {
		//register for Orientation updates
		this.context = context;
		boolean result;
	    sensorService = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	    
	    if (!use_orientation) {
		    Sensor accelSens = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		    Sensor magSens = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		    
		    if (accelSens != null && magSens != null) {
			    sensorService.registerListener(this, 
			    		accelSens,
			    		SensorManager.SENSOR_DELAY_NORMAL);
	
			    sensorService.registerListener(this, 
			    		magSens,
			    		SensorManager.SENSOR_DELAY_NORMAL);
			    Log.i(TAG, "Registerered for magnetic, acceleration Sensor");
			    result = true;
		    } else {
		    	Log.w(TAG, "either magnetic or gravitation sensor not found");
		    	geomag = null;
		    	gravity = null;
		    	unregister();
		    	result = false;
		    }
	    } else {		    
		    Sensor orSens = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		    if (orSens!=null){
		    	sensorService.registerListener(this, orSens, SensorManager.SENSOR_DELAY_NORMAL);
	
			    Log.i(TAG, "Registerered for orientation Sensor");
			    result = true;
		    } else {
		    	Log.w(TAG, "Orientation sensor not found");
		    	unregister();
		    	result = false;
		    }
	    }
	    return result;
	}

	public void unregister() {
		// stop sensors TODO: is this good if sensors registration failed?
		if (sensorService != null) { 
			sensorService.unregisterListener(this);
			sensorService = null;
			Log.v(TAG,"unregisterd");
		}
	}
	
	public float getAzimuth() {
		if (valid) {
			return azimuth;
		} else {
			return DataHelper.AZIMUTH_INVALID;
		}
	}
	
	public int getAccuracy() {
		return accuracy;
	}
};
