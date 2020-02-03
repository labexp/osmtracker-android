package net.osmtracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
//import static org.mockito.Mockito.mock;


@RunWith(PowerMockRunner.class)
@PrepareForTest(PreferenceManager.class)

public class URLCreatorTest {

    Context mockContext;
    SharedPreferences mockPrefs;


    public void setupMocks(){
        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME,
                OSMTracker.Preferences.VAL_GITHUB_USERNAME))
                .thenReturn("labexp");

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_BRANCH_NAME,
                OSMTracker.Preferences.VAL_BRANCH_NAME))
                .thenReturn("master");

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME,
                OSMTracker.Preferences.VAL_REPOSITORY_NAME))
                .thenReturn("osmtracker-android-layouts");



        // Create PreferenceManager mock
        mockContext = mock(Context.class);


        mockStatic(PreferenceManager.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);



    }

    @Test
    public void createMetadataDirUrl() {
        setupMocks();
        String result = URLCreator.createMetadataDirUrl(mockContext);
        String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/metadata?ref=master";
        assertEquals(result, expected);
    }

    @Test
    public void createMetadataFileURL() {
        setupMocks();
        String result = URLCreator.createMetadataFileURL(mockContext, "transporte_publico");
        String expected = "https://raw.githubusercontent.com/labexp/osmtracker-android-layouts/master/layouts/metadata/transporte_publico.xml";
        assertEquals(result, expected);
    }

    @Test
    public void createLayoutFileURL() {
        setupMocks();
        String result = URLCreator.createLayoutFileURL(mockContext, "hidrantes","es");
        String expected = "https://raw.githubusercontent.com/labexp/osmtracker-android-layouts/master/layouts/hidrantes/es.xml";
        assertEquals(result, expected);

    }

    @Test
    public void createIconsDirUrl() {
        setupMocks();
        String result = URLCreator.createIconsDirUrl(mockContext, "hidrantes");
        String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/hidrantes/hidrantes_icons?ref=master";
        assertEquals(result, expected);
    }

    @Test
    public void createTestURL() {
        setupMocks();
        String result = URLCreator.createTestURL("labexp", "osmtracker-android-layouts", "master");
        String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/metadata?ref=master";
        assertEquals(result, expected);
    }

}