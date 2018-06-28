package net.osmtracker.test.activity;

import net.osmtracker.activity.OpenStreetMapUpload;

import android.test.ActivityInstrumentationTestCase2;

public class OSMUploadTest extends ActivityInstrumentationTestCase2<OpenStreetMapUpload> {

	public OSMUploadTest() {
		super("net.osmtracker", OpenStreetMapUpload.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		// MockData.mockBigTrack(getInstrumentation().getContext(), 2000, 2000);
	}
	
	public void test() {
		System.out.println("Test");
	}
}
