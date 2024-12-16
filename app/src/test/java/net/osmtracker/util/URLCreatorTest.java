package net.osmtracker.util;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import net.osmtracker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class URLCreatorTest {

    Context mockContext;
    SharedPreferences mockPrefs;


    public void setupMocks(){
        // Create Context mock
        mockContext = mock(Context.class);

        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        UnitTestUtils.setLayoutsDefaultRepository(mockPrefs);
        when(mockContext.getSharedPreferences(mockContext.getString(R.string.shared_pref), MODE_PRIVATE)).thenReturn(mockPrefs);
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
