package net.osmtracker.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.Preferences;
import net.osmtracker.util.UnitTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PreferenceManager.class, Environment.class, Log.class})
public class DownloadCustomLayoutTaskTest {

    DownloadCustomLayoutTask downloadCustomLayoutTask;

    Context mockContext;
    SharedPreferences mockPrefs;

    //FIXME: layout name and iso are coded.
    String layoutName = "abc";
    String iso = "en";
    String expectedLayoutFilename = "abc_en.xml";


    public void setupMocks() {
        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        UnitTestUtils.setLayoutsTestingRepository(mockPrefs);

        // Create PreferenceManager mock
        mockContext = mock(Context.class);
        mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
        // external storage is writeable
        mockStatic(Environment.class);
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);
        // log
        mockStatic(Log.class);

        downloadCustomLayoutTask = new DownloadCustomLayoutTask(mockContext);
    }

    @Test
    public void downloadLayoutWithoutIconsTest() {
        setupMocks();

        boolean result = downloadCustomLayoutTask.downloadLayout(layoutName,iso);
        assertEquals(true, result);

        // Check if layout was downloaded at .../osmtracker/layouts/abc_en.xml
        String expectedLayoutFilePath = Environment.getExternalStorageDirectory()
                + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator
                + Preferences.LAYOUTS_SUBDIR + File.separator
                + expectedLayoutFilename;

        System.out.println(expectedLayoutFilePath);
        File layoutFile = new File(expectedLayoutFilePath);
        assertTrue(layoutFile.exists());

        // Add N icons to abc layout and check if the N icons are downloaded
        // at ... /osmtracker/layouts/abc_icons.

    }
}
