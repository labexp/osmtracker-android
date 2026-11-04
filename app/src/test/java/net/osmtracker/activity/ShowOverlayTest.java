package net.osmtracker.activity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View.MeasureSpec;
import static android.view.View.MeasureSpec.makeMeasureSpec;

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

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.overlay.Polylines;

import org.osmdroid.util.GeoPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.any;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ShowOverlayTest {

	private DisplayTrackMap displayTrackMap;
	private Context context;
	private ContentResolver resolver;

	private static final long TEST_TRACK_ID = 42L;

	private static final long TEST_OTHER_TRACK_ID = 137L;

	private static final long TEST_YET_OTHER_TRACK_ID = 23L;

	private static final long TEST_NO_SUCH_TRACK_ID = 404L;

	private static final String TAG = "ShowOverlayTest";

	private DataHelper dataHelper;

	private SharedPreferences overlays;
	private Polylines mockOverlay;

	@Before
	public void setUp()
		throws NoSuchFieldException, IllegalAccessException,
		       NoSuchMethodException, InvocationTargetException
	{
		// Comment out to redirect log output to stdout:
		// ShadowLog.stream = System.out;

		// application for creating activities, etc.
		context = RuntimeEnvironment.getApplication();

		resolver = context.getContentResolver();
		dataHelper = new DataHelper(context);

		// Set up default tracks for this test
		createTrack(TEST_TRACK_ID);
		createTrack(TEST_OTHER_TRACK_ID);
		createTrack(TEST_YET_OTHER_TRACK_ID);

		track(TEST_TRACK_ID, 49.61599, 6.12489);
		track(TEST_TRACK_ID, 49.615952, 6.125534);

		track(TEST_OTHER_TRACK_ID, 49.615591, 6.124982);
		track(TEST_OTHER_TRACK_ID, 49.61518,  6.124606);

		track(TEST_YET_OTHER_TRACK_ID, 49.615591, 6.124982);

		// overlay preferences
		overlays = context
			.getSharedPreferences(DisplayTrackMap.OVERLAY_PREFS_FILE,
					      Context.MODE_PRIVATE);
		overlays.edit().clear().apply();

		// Create activity with test track ID
		Intent intent = new Intent();
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID,
				TEST_TRACK_ID);
		displayTrackMap = Robolectric
			.buildActivity(DisplayTrackMap.class, intent)
			.create().get();

		// mock the overlay layer
		// make sure it has originally been set:
		assertNotNull(getInternalState(displayTrackMap, "polylinesOverlaid"));
		mockOverlay = mock(Polylines.class);
		setInternalState(displayTrackMap, "polylinesOverlaid",
				 mockOverlay);
	}

	@After
	public void tearDown() {
		// Close all databases opened by SQLiteOpenHelper
		dataHelper = null;
		resolver = null;
		System.gc();
		SQLiteDatabase.releaseMemory();		
	}

	@Test
	public void testOtherTrackShown()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {

		// Set up other track as overlay of this one
		// Preferences...
		overlays.edit()
			.putLong(String.valueOf(TEST_TRACK_ID),
				 TEST_OTHER_TRACK_ID)
			.apply();

		// onResume should trigger actual drawing of trace
		// this is actually what happens if user goes into
		// "Overlay other" menu, changes something and goes
		// back
		Log.i(TAG, "Calling onResume");
		displayTrackMap.onResume();
		Log.i(TAG, "Done calling onResume");

		/* Verify that drawing on overlay has been ordered */
		verify(mockOverlay, atLeastOnce())
		    .addPoint(any(GeoPoint.class));
	}

	/* if no overlay, other track should *not* be shown */
	@Test
	public void testOtherTrackNotShown()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {

		// onResume should trigger actual drawing of track
		// this is actually what happens if user goes into
		// "Overlay other" menu, changes something and goes
		// back
		displayTrackMap.onResume();

		/* Verify that drawing on overlay has not been called */
		verify(mockOverlay, never()).addPoint(any(GeoPoint.class));
	}

	/* Picked track should be shown in red, tracks that are not selected
	 * should not be shown. Click on another track should chose that one */
	@Test
	public void testSelectedTrackShownAndTrackChange()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {

		String me = String.valueOf(TEST_TRACK_ID);
		overlays.edit()
			.putLong(me, TEST_OTHER_TRACK_ID)
			.apply();

		Intent intent = new Intent();
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID,
				TEST_TRACK_ID);
		TrackPicker tp = Robolectric
			.buildActivity(TrackPicker.class, intent)
			.create()
			.start()
			.resume()
			.get();

		RecyclerView rv = tp.findViewById(R.id.recyclerview);

		ensureAllVisible(rv);

		checkBackgroundColor(rv, TEST_TRACK_ID, false);
		checkBackgroundColor(rv, TEST_OTHER_TRACK_ID, true);
		checkBackgroundColor(rv, TEST_YET_OTHER_TRACK_ID, false);

		// click on item, and make sure it shows up as such in
		// preferences
		clickOnItem(rv, TEST_YET_OTHER_TRACK_ID);

		assertEquals(overlays.getLong(me, 0), TEST_YET_OTHER_TRACK_ID);
	}

	/* Picked track should be shown in red, tracks that are not selected
	 * should not be shown. Clicking on alread selected track should
	 * unselect it */
	@Test
	public void testSelectedTrackShownAndUnselected()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {

		String me = String.valueOf(TEST_TRACK_ID);
		overlays.edit()
			.putLong(me, TEST_YET_OTHER_TRACK_ID)
			.apply();

		Intent intent = new Intent();
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID,
				TEST_TRACK_ID);
		TrackPicker tp = Robolectric
			.buildActivity(TrackPicker.class, intent)
			.create()
			.start()
			.resume()
			.get();

		RecyclerView rv = tp.findViewById(R.id.recyclerview);

		ensureAllVisible(rv);

		checkBackgroundColor(rv, TEST_TRACK_ID, false);
		checkBackgroundColor(rv, TEST_OTHER_TRACK_ID, false);
		checkBackgroundColor(rv, TEST_YET_OTHER_TRACK_ID, true);

		// click on already selected item, and make sure everything
		// is unselected in preferences (represented by 0)
		clickOnItem(rv, TEST_YET_OTHER_TRACK_ID);

		assertEquals(overlays.getLong(me, 0), 0);
	}

	/* Track picker should not crash if overlay is a non-existant
	 * track (may happen if previously selected has been
	 * deleted. There is no foreign key constraint that is
	 * protecting us here */
	@Test
	public void testSelectedTrackNonExistant()
	    throws NoSuchFieldException, NoSuchMethodException,
		   InvocationTargetException, IllegalAccessException {

		String me = String.valueOf(TEST_TRACK_ID);
		overlays.edit()
			.putLong(me, TEST_NO_SUCH_TRACK_ID)
			.apply();

		Intent intent = new Intent();
		intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID,
				TEST_TRACK_ID);
		TrackPicker tp = Robolectric
			.buildActivity(TrackPicker.class, intent)
			.create()
			.start()
			.resume()
			.get();

		RecyclerView rv = tp.findViewById(R.id.recyclerview);

		ensureAllVisible(rv);

		checkBackgroundColor(rv, TEST_TRACK_ID, false);
		checkBackgroundColor(rv, TEST_OTHER_TRACK_ID, false);
		checkBackgroundColor(rv, TEST_YET_OTHER_TRACK_ID, false);

		// we should still be able to select a track
		clickOnItem(rv, TEST_YET_OTHER_TRACK_ID);

		assertEquals(overlays.getLong(me, 0), TEST_YET_OTHER_TRACK_ID);
	}

	// Helper methods

	// Make the screen tall enough so all items render, or else we cannot
	// test them
	private void ensureAllVisible(RecyclerView rv) {
		rv.measure(makeMeasureSpec(500, MeasureSpec.EXACTLY),
			   makeMeasureSpec(2000, MeasureSpec.EXACTLY));
		rv.layout(0, 0, 500, 2000);
	}

	private View findItemByTrackId(RecyclerView recyclerView,
				       long targetTrackId) {
		String targetTrackStr = String.valueOf(targetTrackId);
		for (int i = 0; i < recyclerView.getChildCount(); i++) {
			View child = recyclerView.getChildAt(i);
			TextView trackIdView = child
				.findViewById(R.id.trackmgr_item_id);

			if (trackIdView != null) {
				String trackId = trackIdView
					.getText().toString();
				if (trackId.equals(targetTrackStr))
					return child;
			}
		}
		return null;
	}

	private void clickOnItem(RecyclerView recyclerView,
				 long trackId) {
		View child = findItemByTrackId(recyclerView, trackId);
		assertNotNull("Item "+trackId+" not found", child);
		child.performClick();
	}

	private void checkBackgroundColor(RecyclerView recyclerView,
					  long trackId, boolean expectedRed) {
		RecyclerView.Adapter adapter = recyclerView.getAdapter();

		// Find the item with specific trackId
		View child = findItemByTrackId(recyclerView, trackId);
		assertNotNull("Item "+trackId+" not found", child);
		int itemPosition = recyclerView.getChildLayoutPosition(child);

		// Get the ViewHolder
		RecyclerView.ViewHolder holder =
			adapter.createViewHolder(recyclerView, 0);
		adapter.bindViewHolder(holder, itemPosition);

		// and verify background color
		int expectedColor = Color.RED;
		var color = getBackgroundColor(holder.itemView);
		if(expectedRed) {
			assertEquals(expectedColor, color);
		} else {
			assertNotEquals(expectedColor, color);
		}
	}

	private int getBackgroundColor(View view) {
		var background = view.getBackground();
		if (background instanceof ColorDrawable cd) {
			return cd.getColor();
		}
		return -1;
	}

	private void createTrack(long trackId) {
		ContentValues values = new ContentValues();
		values.put(TrackContentProvider.Schema.COL_START_DATE,
			   System.currentTimeMillis());
		values.put(TrackContentProvider.Schema.COL_ID, trackId);
		assertNotNull("Track should have been created",
			      resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values));
	}

	private void track(long trackId, double latitude, double longitude) {
		Location loc = new Location("gps");
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		loc.setTime(System.currentTimeMillis());

		// Initialize extras to avoid NullPointerException if
		// logic accesses location extras
		loc.setExtras(new Bundle());

		dataHelper.track(trackId, loc, 0.0f, 0, 0.0f, 1);
	}

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
