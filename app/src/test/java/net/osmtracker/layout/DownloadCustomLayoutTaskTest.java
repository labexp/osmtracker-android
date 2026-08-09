package net.osmtracker.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;

import java.io.File;

import static org.junit.Assert.assertTrue;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class DownloadCustomLayoutTaskTest {

    DownloadCustomLayoutTask downloadCustomLayoutTask;
	private Context context;

    String layoutName = "abc";
    String iso = "en";
    String expectedLayoutFilename = "abc_en.xml";


	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();

		// Setup real SharedPreferences logic
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit()
				.putString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, "labexp")
				.putString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, "osmtracker-android-layouts")
				.putString(OSMTracker.Preferences.KEY_BRANCH_NAME, "for_tests")
				.apply();

		// Setup Environment Shadow
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

		downloadCustomLayoutTask = new DownloadCustomLayoutTask(context);
	}

	@Test
	public void downloadLayoutWithoutIconsTest() {
		boolean result = downloadCustomLayoutTask.downloadLayout(layoutName, iso);
		assertTrue("Download should return true", result);

		// Check if layout was downloaded at .../osmtracker/layouts/abc_en.xml
		File layoutsDir = new File(context.getExternalFilesDir(null),
				OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator + DataHelper.LAYOUTS_SUBDIR);

		File layoutFile = new File(layoutsDir, expectedLayoutFilename);

		System.out.println("Expected path: " + layoutFile.getAbsolutePath());
		assertTrue("Layout file should exist at path", layoutFile.exists());

		// Add N icons to abc layout and check if the N icons are downloaded
		// at ... /osmtracker/layouts/abc_icons.
	}
}
