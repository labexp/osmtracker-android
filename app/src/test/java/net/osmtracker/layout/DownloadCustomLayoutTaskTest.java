package net.osmtracker.layout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PreferenceManager.class, Environment.class})
public class DownloadCustomLayoutTaskTest {

    DownloadCustomLayoutTask downloadCustomLayoutTask;

    Context mockContext;
    SharedPreferences mockPrefs;

    @Rule
    public TemporaryFolder storageDirectory = new TemporaryFolder();

    private File existentDirectory;



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

    }

    @Test
    public void downloadLayoutTest() {
        setupMocks();

        //FIXME: layout name and iso are coded.
        boolean result = downloadCustomLayoutTask.downloadLayout("abc","en");
        assertEquals(true, result);

        //TODO: check if after downloading the layout all files are correctly organized.

    }
}
