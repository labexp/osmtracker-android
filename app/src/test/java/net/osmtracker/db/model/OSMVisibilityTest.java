package net.osmtracker.db.model;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import net.osmtracker.R;
import net.osmtracker.db.model.Track;

@RunWith(RobolectricTestRunner.class)
//Min SDK of the App
@Config(sdk = 25)
public class OSMVisibilityTest {

	private Context context;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
	}


	@Test
	public void testEnumMappingToName() {
		// Database stores the .name() of the enum
		Assert.assertEquals("Private" , Track.OSMVisibility.Private.name());
		Assert.assertEquals("Public" , Track.OSMVisibility.Public.name());
		Assert.assertEquals("Trackable" , Track.OSMVisibility.Trackable.name());
		Assert.assertEquals("Identifiable", Track.OSMVisibility.Identifiable.name());
	}

	@Test
	public void testFromPosition() {
		// Verifies the Spinner index mapping (0 -> Private, 3 -> Identifiable)
		Assert.assertEquals(Track.OSMVisibility.Private, Track.OSMVisibility.fromPosition(0));
		Assert.assertEquals(Track.OSMVisibility.Public, Track.OSMVisibility.fromPosition(1));
		Assert.assertEquals(Track.OSMVisibility.Trackable, Track.OSMVisibility.fromPosition(2));
		Assert.assertEquals(Track.OSMVisibility.Identifiable, Track.OSMVisibility.fromPosition(3));
	}

	@Test
	public void testResourceIdsMapToCorrectStrings() {
		Assert.assertEquals(context.getString(R.string.osm_visibility_private),
				context.getString(Track.OSMVisibility.Private.resId));
		Assert.assertEquals(context.getString(R.string.osm_visibility_public),
				context.getString(Track.OSMVisibility.Public.resId));
		Assert.assertEquals(context.getString(R.string.osm_visibility_trackable),
				context.getString(Track.OSMVisibility.Trackable.resId));
		Assert.assertEquals(context.getString(R.string.osm_visibility_identifiable),
				context.getString(Track.OSMVisibility.Identifiable.resId));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFromPosition_Invalid() {
		// Verifies that an invalid index throws the expected exception
		Track.OSMVisibility.fromPosition(99);
	}
}
