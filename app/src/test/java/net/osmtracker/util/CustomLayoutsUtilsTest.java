package net.osmtracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PreferenceManager.class)
public class CustomLayoutsUtilsTest {

    Context mockContext;
    SharedPreferences mockPrefs;

    public void setupMocks(){
        // Create SharedPreferences mock
        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT,
                OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT))
                .thenReturn("transporte publico");

        // Create PreferenceManager mock
        mockContext = mock(Context.class);

        mockStatic(PreferenceManager.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
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

    /* Not working...
    @Test
    public void getStringFromStream() throws IOException {
        setupMocks();
        InputStream is = mockContext.getAssets().open("gpx/gpx-test.gpx");
        String result = CustomLayoutsUtils.getStringFromStream(is);
        is.close();

        is = mockContext.getAssets().open("gpx/gpx-test.gpx");
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (is, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        is.close();
        String expected = textBuilder.toString();

        assertEquals(result, expected);
    }
     */

    @Test
    public void getCurrentLayoutName() {
        setupMocks();
        String result = CustomLayoutsUtils.getCurrentLayoutName(mockContext);
        String expected = "transporte publico";
        assertEquals(result, expected);
    }

}
