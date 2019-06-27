package net.osmtracker.test.activity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import junit.framework.Assert;

import net.osmtracker.activity.TrackDetail;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.TrackContentProvider.Schema;
import net.osmtracker.test.util.MockData;

public class TrackDetailTest extends ActivityTestRule<TrackDetail> {

	private long trackId;
	
	public TrackDetailTest() {
		super(TrackDetail.class);
	}

	protected void setUp() throws Exception {
		trackId = MockData.mockTrack(InstrumentationRegistry.getInstrumentation().getContext());
		
		Intent i = new Intent();
		i.putExtra(Schema.COL_TRACK_ID, trackId);
		launchActivity(i);
	}
	
	@UiThreadTest
	public void testSave() {
		
		ContentResolver cr = InstrumentationRegistry.getInstrumentation().getContext().getContentResolver();
		Cursor cursor = cr.query(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
				null, null, null, null);
		
		Assert.assertTrue(cursor.moveToFirst());
		Assert.assertEquals("gpx-test", cursor.getString(cursor.getColumnIndex(Schema.COL_NAME)));
		Assert.assertNull(cursor.getString(cursor.getColumnIndex(Schema.COL_DESCRIPTION)));
		Assert.assertNull(cursor.getString(cursor.getColumnIndex(Schema.COL_TAGS)));
		Assert.assertEquals("Private", cursor.getString(cursor.getColumnIndex(Schema.COL_OSM_VISIBILITY)));
		cursor.close();
		
		((EditText) getActivity().findViewById(net.osmtracker.R.id.trackdetail_item_name)).setText("test name");
		((EditText) getActivity().findViewById(net.osmtracker.R.id.trackdetail_item_description)).setText("test description");
		((EditText) getActivity().findViewById(net.osmtracker.R.id.trackdetail_item_tags)).setText("test tags");
		((Spinner) getActivity().findViewById(net.osmtracker.R.id.trackdetail_item_osm_visibility)).setSelection(1);
		((Button) getActivity().findViewById(net.osmtracker.R.id.trackdetail_btn_ok)).performClick();

		cursor = cr.query(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
				null, null, null, null);
		
		Assert.assertTrue(cursor.moveToFirst());
		Assert.assertEquals("test name", cursor.getString(cursor.getColumnIndex(Schema.COL_NAME)));
		Assert.assertEquals("test description", cursor.getString(cursor.getColumnIndex(Schema.COL_DESCRIPTION)));
		Assert.assertEquals("test tags", cursor.getString(cursor.getColumnIndex(Schema.COL_TAGS)));
		Assert.assertEquals("Public", cursor.getString(cursor.getColumnIndex(Schema.COL_OSM_VISIBILITY)));
		cursor.close();

	}
	
	
}
