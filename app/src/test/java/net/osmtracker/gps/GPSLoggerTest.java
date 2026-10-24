package net.osmtracker.gps;

import java.lang.reflect.Field;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;
import net.osmtracker.service.gps.GPSLogger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyFloat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class GPSLoggerTest {

	private GPSLogger gpsLogger;
	private DataHelper mockDataHelper;
	private Context context;

	@Before
	public void setUp() throws NoSuchFieldException, IllegalAccessException {
		context = RuntimeEnvironment.getApplication();
		// Set up default preferences
		PreferenceManager.getDefaultSharedPreferences(context)
			.edit()
			.putString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, "0")
			.putString(OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, "0")
			.putBoolean(OSMTracker.Preferences.KEY_OUTPUT_ACCURACY, true)
			.commit();

		gpsLogger = new GPSLogger();
		mockDataHelper = mock(DataHelper.class);
		setInternalState(gpsLogger, "dataHelper", mockDataHelper);
		setInternalState(gpsLogger, "isTracking", true);
		setInternalState(gpsLogger, "gpsLoggingDistGtAccuracy", true);
	}

	@Test
	public void shouldLogWhenDistanceGreaterThanAccuracy() {
		Log.i("GPSLoggerTest", "shouldLogWhenDistanceGreaterThanAccuracy");
		gpsLogger.onLocationChanged(createLocation(0.0, 0.0, 5.0f));
		verify(mockDataHelper, times(1)).track(anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong());

		// Second location: distance (~10m) > accuracy (5m), should log
		gpsLogger.onLocationChanged(createLocation(0.00009, 0.0, 5.0f));

		verify(mockDataHelper, times(2)).track(anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong());
	}

	@Test
	public void shouldNotLogWhenDistanceLessThanAccuracy() {
		Log.i("GPSLoggerTest", "shouldNotLogWhenDistanceGreaterThanAccuracy");
		gpsLogger.onLocationChanged(createLocation(0.0, 0.0, 15.0f));
		verify(mockDataHelper, times(1)).track(anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong());

		// Second location: distance (~5m) < accuracy (15m), should NOT log
		gpsLogger.onLocationChanged(createLocation(0.000045, 0.0, 15.0f));
		verify(mockDataHelper, times(1)).track(anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong()); // there should be no additional invocation
	}

	private Location createLocation(double latitude, double longitude, float accuracy) {
		Location location = new Location("test");
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		location.setAccuracy(accuracy);
		location.setTime(System.currentTimeMillis());
		return location;
	}

	private void setInternalState(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
		Field field;
		if (target instanceof Class) {
			field = ((Class<?>) target).getDeclaredField(fieldName);
		} else {
			field = target.getClass().getDeclaredField(fieldName);
		}
		field.setAccessible(true);
		field.set(target instanceof Class ? null : target, value);
	}
}
