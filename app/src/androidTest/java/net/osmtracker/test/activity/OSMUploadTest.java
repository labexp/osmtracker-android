package net.osmtracker.test.activity;

import android.support.test.rule.ActivityTestRule;

import net.osmtracker.activity.OpenStreetMapUpload;

public class OSMUploadTest extends ActivityTestRule<OpenStreetMapUpload> {

	public OSMUploadTest() {
		super(OpenStreetMapUpload.class);
	}
	
	public void test() {
		System.out.println("Test");
	}
}
