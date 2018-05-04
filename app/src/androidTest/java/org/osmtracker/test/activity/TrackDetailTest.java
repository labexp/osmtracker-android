package org.osmtracker.test.activity;

import junit.framework.Assert;
import org.osmtracker.R;
import org.osmtracker.activity.TrackDetail;
import org.osmtracker.db.TrackContentProvider;
import org.osmtracker.db.TrackContentProvider.Schema;
import org.osmtracker.test.util.MockData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class TrackDetailTest extends ActivityInstrumentationTestCase2<TrackDetail> {

	private long trackId;
	
	public TrackDetailTest() {
		super("org.osmtracker", TrackDetail.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		trackId = MockData.mockTrack(getInstrumentation().getContext());
		
		Intent i = new Intent();
		i.putExtra(Schema.COL_TRACK_ID, trackId);
		setActivityIntent(i);
	}
	
	@UiThreadTest
	public void testSave() {
		
		ContentResolver cr = getInstrumentation().getContext().getContentResolver();
		Cursor cursor = cr.query(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
				null, null, null, null);
		
		Assert.assertTrue(cursor.moveToFirst());
		Assert.assertEquals("gpx-test", cursor.getString(cursor.getColumnIndex(Schema.COL_NAME)));
		Assert.assertNull(cursor.getString(cursor.getColumnIndex(Schema.COL_DESCRIPTION)));
		Assert.assertNull(cursor.getString(cursor.getColumnIndex(Schema.COL_TAGS)));
		Assert.assertEquals("Private", cursor.getString(cursor.getColumnIndex(Schema.COL_OSM_VISIBILITY)));
		cursor.close();
		
		((EditText) getActivity().findViewById(R.id.trackdetail_item_name)).setText("test name");
		((EditText) getActivity().findViewById(R.id.trackdetail_item_description)).setText("test description");
		((EditText) getActivity().findViewById(R.id.trackdetail_item_tags)).setText("test tags");
		((Spinner) getActivity().findViewById(R.id.trackdetail_item_osm_visibility)).setSelection(1);
		((Button) getActivity().findViewById(R.id.trackdetail_btn_ok)).performClick();

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
