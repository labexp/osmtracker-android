package net.osmtracker.util;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class URLCreatorTest {

	private Context context;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
	}

    @Test
    public void createMetadataDirUrl() {
		String result = URLCreator.createMetadataDirUrl(context);
		String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/metadata?ref=master";
		assertEquals(expected, result);
    }

    @Test
    public void createMetadataFileURL() {
        String result = URLCreator.createMetadataFileURL(context, "transporte_publico");
        String expected = "https://raw.githubusercontent.com/labexp/osmtracker-android-layouts/master/layouts/metadata/transporte_publico.xml";
		assertEquals(expected, result);
    }

    @Test
    public void createLayoutFileURL() {
        String result = URLCreator.createLayoutFileURL(context, "hidrantes","es");
        String expected = "https://raw.githubusercontent.com/labexp/osmtracker-android-layouts/master/layouts/hidrantes/es.xml";
		assertEquals(expected, result);

    }

    @Test
    public void createIconsDirUrl() {
        String result = URLCreator.createIconsDirUrl(context, "hidrantes");
        String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/hidrantes/hidrantes_icons?ref=master";
		assertEquals(expected, result);
    }

    @Test
    public void createTestURL() {
        String result = URLCreator.createTestURL("labexp", "osmtracker-android-layouts", "master");
        String expected = "https://api.github.com/repos/labexp/osmtracker-android-layouts/contents/layouts/metadata?ref=master";
		assertEquals(expected, result);
    }

}
