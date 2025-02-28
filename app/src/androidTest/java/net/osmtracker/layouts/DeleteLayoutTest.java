package net.osmtracker.layouts;

import android.Manifest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import net.osmtracker.R;
import net.osmtracker.activity.ButtonsPresets;
import net.osmtracker.activity.Preferences;
import net.osmtracker.util.CustomLayoutsUtils;
import net.osmtracker.util.TestUtils;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

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
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;
import static org.apache.commons.io.FileUtils.deleteDirectory;


public class DeleteLayoutTest {

    @Rule
    public GrantPermissionRule storagePermission = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public ActivityTestRule<ButtonsPresets> mRule = new ActivityTestRule(ButtonsPresets.class) {
        @Override
        protected void beforeActivityLaunched() {
            //Makes sure that only the mock layout exists
            try {
                deleteDirectory(getLayoutsDirectory());
                injectMockLayout(layoutName, ISOLanguageCode);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private static String layoutName = "mock";
    private static String ISOLanguageCode = "es";

    /**
     * Assumes being in the ButtonsPresets activity
     * Deletes the layout with the received name
     */
    private void deleteLayout(String layoutName){
        onView(withText(layoutName)).perform(longClick());
        onView(withText(getStringResource(R.string.buttons_presets_context_menu_delete))).perform(click());
        String textToMatch = getStringResource(R.string.buttons_presets_delete_positive_confirmation);
        onView(withText(equalToIgnoringCase(textToMatch))).perform(click());
    }

    /**
     * Deletes the mock layout and then checks that:
     *  - The UI option doesn't appear anymore
     *  - The XML file is deleted
     *  - A Toast is shown to inform about what happened
     *  - The icons directory is deleted
     */
    @Test
    public void layoutDeletionTest(){

        deleteLayout(layoutName);

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
        assertFalse(filesAfterDeletion.contains(layoutName+ Preferences.ICONS_DIR_SUFFIX));

    }
}
