package net.osmtracker.util;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class CustomLayoutsUtilsTest {

    Context mockContext;
    SharedPreferences mockPrefs;
    AssetManager mockAssetManager;
    InputStream resultStream;
    InputStream expectedStream;

    public void setupMocks() {
        // Create Context mock
        mockContext = mock(Context.class);

        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT))
                .thenReturn("transporte publico");

        when(mockContext.getSharedPreferences(mockContext.getString(R.string.shared_pref), MODE_PRIVATE)).thenReturn(mockPrefs);

        mockAssetManager = mock(AssetManager.class);

        try {
            resultStream = new FileInputStream("./src/test/assets/gpx/gpx-test.gpx");
            expectedStream = new FileInputStream("./src/test/assets/gpx/gpx-test.gpx");
            when(mockContext.getAssets()).thenReturn(mockAssetManager);
            when(mockAssetManager.open("result.gpx")).thenReturn(resultStream);
            when(mockAssetManager.open("expected.gpx")).thenReturn(expectedStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void convertFileName() {
        String result = CustomLayoutsUtils.convertFileName("public_transport.xml");
        String expected = "public transport";
        assertEquals(result, expected);
    }

    @Test
    public void unconvertFileName() {
        String result = CustomLayoutsUtils.unconvertFileName("public transport");
        String expected = "public_transport.xml";
        assertEquals(result, expected);
    }

    @Test
    public void createFileName() {
        String result = CustomLayoutsUtils.createFileName("public transport", "es");
        String expected = "public_transport_es.xml";
        assertEquals(result, expected);
    }

    @Test
    public void getStringFromStream() throws IOException {
        setupMocks();

        InputStream resultIs = mockAssetManager.open("result.gpx");
        String result = CustomLayoutsUtils.getStringFromStream(resultIs);

        String expected;
        try (InputStream expectedIs = mockAssetManager.open("expected.gpx");
             InputStreamReader expectedIsr = new InputStreamReader(expectedIs, StandardCharsets.UTF_8);
             BufferedReader expectedReader = new BufferedReader(expectedIsr)) {
             StringBuilder expectedBuilder = new StringBuilder();
             String line;
             while ((line = expectedReader.readLine()) != null) {
                 expectedBuilder.append(line).append(System.lineSeparator());
             }
             expected = expectedBuilder.toString();
        }
        assertEquals("String should have same content", expected, result);
    }

    @Test
    public void getCurrentLayoutName() {
        setupMocks();
        String result = CustomLayoutsUtils.getCurrentLayoutName(mockContext);
        String expected = "transporte publico";
        assertEquals(result, expected);
    }

}
