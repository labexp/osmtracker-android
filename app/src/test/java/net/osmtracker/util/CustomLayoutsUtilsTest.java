package net.osmtracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import net.osmtracker.OSMTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class CustomLayoutsUtilsTest {

	private Context context;
	private SharedPreferences prefs;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// Ensure a clean state for every test
		prefs.edit().clear().apply();
	}

	@Test
	public void convertFileName() {
		assertEquals("public transport", CustomLayoutsUtils.convertFileName("public_transport.xml"));
		assertEquals("simple", CustomLayoutsUtils.convertFileName("simple.xml"));
	}

	@Test
	public void unconvertFileName() {
		assertEquals("public_transport.xml", CustomLayoutsUtils.unconvertFileName("public transport"));
	}

	@Test
	public void createFileName() {
		assertEquals("public_transport_es.xml", CustomLayoutsUtils.createFileName("public transport", "es"));
	}

	@Test
	public void getStringFromStream() throws IOException {
		String content = "GPX Test Content" + System.lineSeparator() + "Second Line";
		InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		String result = CustomLayoutsUtils.getStringFromStream(inputStream);
		assertEquals(content, result);
	}

	@Test
	public void getCurrentLayoutName() {
		// Set value in real Robolectric preferences
		prefs.edit().putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, "transporte publico").apply();
		String result = CustomLayoutsUtils.getCurrentLayoutName(context);
		assertEquals("transporte publico", result);
	}

	@Test
	public void getCurrentLayoutName_ReturnsDefaultWhenEmpty() {
		// Test fallback logic
		String result = CustomLayoutsUtils.getCurrentLayoutName(context);
		assertEquals(OSMTracker.Preferences.VAL_UI_BUTTONS_LAYOUT, result);
	}

}
