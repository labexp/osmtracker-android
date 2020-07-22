package net.osmtracker.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.Preferences;
import net.osmtracker.util.CustomLayoutsUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

    @Rule
    public TemporaryFolder storageDirectory = new TemporaryFolder();

    private File existentDirectory;

    //FIXME: layout name and iso are coded.
    String layoutName = "abc";
    String iso = "en";


    public void setupMocks() {
        // Create PreferenceManager mock
        mockContext = mock(Context.class);

        //FIXME: the values for github username, repo and branch are (partially) repeated in
        // @URLCreatorTest and in @TestUtils.

        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME,
                OSMTracker.Preferences.VAL_GITHUB_USERNAME))
                .thenReturn("labexp");
        // jamescr has abc_icons

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME,
                OSMTracker.Preferences.VAL_BRANCH_NAME))
                .thenReturn("for_tests");

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME,
                OSMTracker.Preferences.VAL_REPOSITORY_NAME))
                .thenReturn("osmtracker-android-layouts");

        // Create PreferenceManager mock
        mockContext = mock(Context.class);
        mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);

        downloadCustomLayoutTask = new DownloadCustomLayoutTask(mockContext);

        // external storage is writeable
        mockStatic(Environment.class);
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);

        mockStatic(Log.class);

    }

    @Test
    public void downloadLayoutWithoutIconsTest() {
        setupMocks();

        boolean result = downloadCustomLayoutTask.downloadLayout(layoutName,iso);
        assertEquals(true, result);

        // Check if layout was downloaded at .../osmtracker/layouts/abc_en.xml
        String layoutFolderName = layoutName.replace(" ", "_");
        String layoutFilePath = Environment.getExternalStorageDirectory()
                + OSMTracker.Preferences.VAL_STORAGE_DIR + File.separator
                + Preferences.LAYOUTS_SUBDIR + File.separator
                + CustomLayoutsUtils.createFileName(layoutName, iso);
        File layoutFile = new File(layoutFilePath);
        assertTrue(layoutFile.exists());

        // Add N icons to abc layout and check if the N icons are downloaded
        // at ... /osmtracker/layouts/abc_icons.

    }
}
