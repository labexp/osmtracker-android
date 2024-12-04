package net.osmtracker.layouts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmtracker.util.TestUtils.checkToastIsShownWith;
import static net.osmtracker.util.TestUtils.getLayoutsDirectory;
import static net.osmtracker.util.TestUtils.getStringResource;
import static net.osmtracker.util.TestUtils.injectMockLayout;
import static net.osmtracker.util.TestUtils.listFiles;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;

import android.Manifest;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.rule.GrantPermissionRule;

import net.osmtracker.R;
import net.osmtracker.activity.ButtonsPresets;
import net.osmtracker.activity.Preferences;
import net.osmtracker.util.CustomLayoutsUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

public class DeleteLayoutTest {

	@Rule
	public GrantPermissionRule storagePermission = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

	public ActivityScenario<ButtonsPresets> activity;

	private static final String layoutName = "mock";
	private static final String ISOLanguageCode = "es";

	@Before
	public void setUp() {
		// Makes sure that only the mock layout exists
		try {
			deleteDirectory(getLayoutsDirectory());
			injectMockLayout(layoutName, ISOLanguageCode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Launch activity
		activity = ActivityScenario.launch(ButtonsPresets.class);
		activity.moveToState(Lifecycle.State.RESUMED);
	}

	@After
	public void tearDown() {
		activity.close();
	}

	/**
	 * Assumes being in the ButtonsPresets activity
	 * Deletes the layout with the received name
	 */
	private void deleteLayout() {
		onView(withText(layoutName)).perform(longClick());
		onView(withText(getStringResource(R.string.buttons_presets_context_menu_delete))).perform(click());
		String textToMatch = getStringResource(R.string.buttons_presets_delete_positive_confirmation);
		onView(withText(equalToIgnoringCase(textToMatch))).perform(click());
	}

	/**
	 * Deletes the mock layout and then checks that:
	 * - The UI option doesn't appear anymore
	 * - The XML file is deleted
	 * - A Toast is shown to inform about what happened
	 * - The icons directory is deleted
	 */
	@Test
	public void layoutDeletionTest() {
		deleteLayout();

		// Check the informative Toast is shown
		checkToastIsShownWith(getStringResource(R.string.buttons_presets_successful_delete));

		// Check the layout doesn't appear anymore
		onView(withText(layoutName)).check(doesNotExist());

		// List files after the deletion
		ArrayList<String> filesAfterDeletion = listFiles(getLayoutsDirectory());

		// Check the xml file was deleted
		String layoutFileName = CustomLayoutsUtils.createFileName(layoutName, ISOLanguageCode);
		assertFalse(filesAfterDeletion.contains(layoutFileName));

		// Check the icons folder was deleted
		assertFalse(filesAfterDeletion.contains(layoutName + Preferences.ICONS_DIR_SUFFIX));
	}
}
