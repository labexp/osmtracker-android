package net.osmtracker.activity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.app.Application;
import android.content.ComponentName;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowService;
import org.robolectric.shadows.ShadowLooper;

import androidx.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.service.gps.GPSLogger;
import net.osmtracker.service.gps.GPSLoggerServiceConnection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import org.mockito.InOrder;

import android.content.ContentProviderClient;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class StartStopOnMapTest {

	private DisplayTrackMap displayTrackMap;
	private Context context;
	private ShadowApplication shadowApplication;
	private ContentResolver resolver;

	private static final long TEST_TRACK_ID = 42L;

	private static final String TAG = "StartStopOnMapTest";

	private ImageView mockStartStopButton;
	private InOrder startStopButtonInOrder;

	private ShadowService shadowService;

	@Before
	public void setUp()
		throws NoSuchFieldException, IllegalAccessException,
		       NoSuchMethodException, InvocationTargetException
	{
		// Comment out to redirect log output to stdout:
		ShadowLog.stream = System.out;

		// application for creating activities, etc.
		Application app = RuntimeEnvironment.getApplication();
		context = app;

		// shadow application, with robolectric-specific
		// methods for finding out whether a service has been
		// started
		shadowApplication = Shadow.extract(app);

		resolver = context.getContentResolver();

		// Set up a default track for this test
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_START_DATE,
			   System.currentTimeMillis());
		values.put(TrackContentProvider.Schema.COL_ID,
			   TEST_TRACK_ID);
		var uri = resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		assertNotNull("Track should have been created", uri);

		// Set up default preferences
		PreferenceManager.getDefaultSharedPreferences(context)
			.edit()
			.putString(OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, "0")
			.putString(OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, "0")
			.commit();

		// Create activity with test track ID
		Intent intent = new Intent();
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID,
				TEST_TRACK_ID);
		displayTrackMap = Robolectric
			.buildActivity(DisplayTrackMap.class, intent)
			.create().get();

		// Create a mock start/stop button
		mockStartStopButton = mock(ImageView.class);
		setInternalState(displayTrackMap, "startStopButton", mockStartStopButton);
		startStopButtonInOrder = inOrder(mockStartStopButton);
	}

	@After
	public void tearDown() {
		try {
			ContentProviderClient client =
				RuntimeEnvironment.getApplication()
				.getContentResolver()
				.acquireContentProviderClient(TrackContentProvider.AUTHORITY);

			if (client != null) {
				client.release();  // Release the client
			}
		} catch (Exception e) {
			// Ignore
		}
	}

	@Test
	public void testToggleStartStop_WhenNotTracking_ShouldStartTracking()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {
		// Given: currently not tracking
		assertFalse("Initial state should be 'not tracking'",
			    isTracking(displayTrackMap));

		// When: toggle is called
		toggleStartStop(displayTrackMap);

		// Then: should have attempted to start the service
		Intent expectedIntent = shadowApplication.getNextStartedService();
		assertTrue("Service should have been started",
			   expectedIntent != null);
		assertTrue("Started service should be GPSLogger",
			   expectedIntent.getComponent().getClassName()
			   .contains("GPSLogger"));
		assertTrue("Intent should contain track ID",
			   expectedIntent
			   .getLongExtra(TrackContentProvider.Schema.COL_TRACK_ID, -1L) ==
			   TEST_TRACK_ID);

		// Now that we have established that the application
		// asked to start the service, actually do so

		// Indeed, with Robolectric, the shadowApplication
		// merely registers the *request* to start a service,
		// but does not actually start it. This needs to be
		// done "manually" here.
		var serviceController = Robolectric
			.buildService(GPSLogger.class, expectedIntent);
		GPSLogger service = serviceController.get();

		// Create the real binder from the service
		ComponentName componentName = new ComponentName(RuntimeEnvironment.getApplication(), GPSLogger.class);
		GPSLogger.GPSLoggerBinder binder = service.new GPSLoggerBinder();
		shadowApplication
		.setComponentNameAndServiceForBindService(componentName,
							  binder);
		serviceController.create();

		shadowService = Shadow.extract(service);

		// allow Android framework to process
		ShadowLooper.idleMainLooper();

		assertTrue("isTracking flag should be true after starting",
			   isTracking(displayTrackMap));
		assertFalse("Service should not yet have been stopped",
			   shadowService.isStoppedBySelf());

		// button should show "started" color:
		startStopButtonInOrder
			.verify(mockStartStopButton, atLeastOnce())
			.setImageResource(R.drawable.started);

		// Now try stop tracking
		toggleStartStop(displayTrackMap);
		ShadowLooper.idleMainLooper(100);
		assertTrue("Service should have been stopped",
			   shadowService.isStoppedBySelf());

		// now that we've established that the service
		// did initiated a stopSelf, help robolectric along, and
		// actually perform what this entails
		GPSLoggerServiceConnection cnx = (GPSLoggerServiceConnection)
			getInternalState(displayTrackMap,
					 "gpsLoggerConnection");
		cnx.onServiceDisconnected(componentName);

		assertFalse("isTracking flag should be false after stopping",
			   isTracking(displayTrackMap));
		// button should show "stopped" color:
		startStopButtonInOrder
			.verify(mockStartStopButton, atLeastOnce())
			.setImageResource(R.drawable.stopped);
	}

	@Test
	public void testIsTracking_WhenGpsLoggerIsNull_ShouldReturnFalse()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {
		// Given: gpsLogger is null
		setInternalState(displayTrackMap, "gpsLogger", null);

		// When: isTracking() is called
		boolean result = isTracking(displayTrackMap);

		// Then: should return false
		assertFalse("isTracking() should return false when gpsLogger is null", result);
	}

	@Test
	public void testSetGpsLogger_WhenNull_ShouldSetIsTrackingFalse()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {
		// Given: isTracking is true
		setInternalState(displayTrackMap, "isTracking", true);

		// When: setGpsLogger(null) is called
		displayTrackMap.setGpsLogger(null);

		// Then: isTracking should be set to false
		assertFalse("isTracking should be false when gpsLogger is set to null",
			    isTracking(displayTrackMap));
	}

	@Test
	public void testUpdateTrackingIcon_WhenButtonNull_ShouldNotCrash()
		throws NoSuchFieldException, NoSuchMethodException,
		       InvocationTargetException, IllegalAccessException
	{
		// Given: button is null
		setInternalState(displayTrackMap, "startStopButton", null);
		setInternalState(displayTrackMap, "isTracking", true);

		// When: updateTrackingIcon() is called
		// Then: should not crash
		updateTrackingIcon(displayTrackMap);
	}

	// Call private methods
	private void toggleStartStop(DisplayTrackMap displayTrackMap)
		throws NoSuchMethodException, InvocationTargetException,
		       IllegalAccessException
	{
		invokeInternalMethod(displayTrackMap, "toggleStartStop");
	}

	private boolean isTracking(DisplayTrackMap displayTrackMap)
		throws NoSuchMethodException, InvocationTargetException,
		       IllegalAccessException
	{
		return (Boolean) invokeInternalMethod(displayTrackMap,
						      "isTracking");
	}

	private void updateTrackingIcon(DisplayTrackMap displayTrackMap)
		throws NoSuchMethodException, InvocationTargetException,
		IllegalAccessException
	{
		invokeInternalMethod(displayTrackMap, "updateTrackingIcon");
	}

	// Helper methods
	private void setInternalState(Object target, String fieldName,
				      Object value)
		throws NoSuchFieldException, IllegalAccessException
	{
		Field field;
		if (target instanceof Class) {
			field = ((Class<?>) target).getDeclaredField(fieldName);
		} else {
			field = target.getClass().getDeclaredField(fieldName);
		}
		field.setAccessible(true);
		field.set(target instanceof Class ? null : target, value);
	}

	private Object getInternalState(Object target, String fieldName)
		throws NoSuchFieldException, IllegalAccessException {
		Field field;
		if (target instanceof Class) {
			field = ((Class<?>) target).getDeclaredField(fieldName);
		} else {
			field = target.getClass().getDeclaredField(fieldName);
		}
		field.setAccessible(true);
		return field.get(target instanceof Class ? null : target);
	}

	private Object invokeInternalMethod(Object target, String getterName)
		throws NoSuchMethodException, InvocationTargetException,
		       IllegalAccessException {
		Method getter = target.getClass()
			.getDeclaredMethod(getterName);
		getter.setAccessible(true);
		return getter.invoke(target);
	}
}
