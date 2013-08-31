package me.guillaumin.android.osmtracker.test.activity;

import me.guillaumin.android.osmtracker.activity.OpenStreetMapUpload;
import me.guillaumin.android.osmtracker.test.util.MockData;
import android.test.ActivityInstrumentationTestCase2;

public class OSMUploadTest extends ActivityInstrumentationTestCase2<OpenStreetMapUpload> {

	public OSMUploadTest() {
		super("me.guillaumin.android.osmtracker", OpenStreetMapUpload.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		// MockData.mockBigTrack(getInstrumentation().getContext(), 2000, 2000);
	}
	
	public void test() {
		System.out.println("Test");
	}
}
