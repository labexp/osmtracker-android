package net.osmtracker.util;

import android.content.SharedPreferences;

import net.osmtracker.OSMTracker;

import static org.powermock.api.mockito.PowerMockito.when;

public class UnitTestUtils {

    public static String TESTING_GITHUB_USER = "labexp";
    public static String TESTING_GITHUB_REPOSITORY = "osmtracker-android-layouts";
    public static String TESTING_GITHUB_BRANCH = "for_tests";

    public static void setGithubRepositorySettings(SharedPreferences mockPrefs, String user,
                                                   String repo, String branch) {
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME,
                OSMTracker.Preferences.VAL_GITHUB_USERNAME))
                .thenReturn(user);

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME,
                OSMTracker.Preferences.VAL_REPOSITORY_NAME))
                .thenReturn(repo);

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME,
                OSMTracker.Preferences.VAL_BRANCH_NAME))
                .thenReturn(branch);
    }

    public static void setLayoutsTestingRepository(SharedPreferences mockPrefs){
        setGithubRepositorySettings(mockPrefs, TESTING_GITHUB_USER, TESTING_GITHUB_REPOSITORY,
                TESTING_GITHUB_BRANCH);
    }

    public static void setLayoutsDefaultRepository(SharedPreferences mockPrefs){
        setGithubRepositorySettings(mockPrefs, OSMTracker.Preferences.VAL_GITHUB_USERNAME,
                OSMTracker.Preferences.VAL_REPOSITORY_NAME,
                OSMTracker.Preferences.VAL_BRANCH_NAME);
    }

}
