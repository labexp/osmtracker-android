package net.osmtracker.util;

import android.content.SharedPreferences;

import net.osmtracker.OSMTracker;

import java.util.Date;

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

    // This method is used to hide the weird modifications (offsets) that need to be made when creating a Date object
    // See why here https://docs.oracle.com/javase/8/docs/api/java/util/Date.html#setYear-int-
    public static Date createDateFrom(int year, int month, int day, int hour, int minute, int second) {
        return new Date(year-1900, month-1, day, hour, minute, second);
    }
}
