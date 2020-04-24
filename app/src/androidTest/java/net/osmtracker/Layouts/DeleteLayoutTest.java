package net.osmtracker.Layouts;

import android.Manifest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;

import net.osmtracker.R;
import net.osmtracker.activity.ButtonsPresets;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.matcher.PreferenceMatchers.withTitleText;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static net.osmtracker.Layouts.TestUtils.checkToastIsShownWith;
import static net.osmtracker.Layouts.TestUtils.getLayoutsDirectory;
import static net.osmtracker.Layouts.TestUtils.getStringResource;
import static net.osmtracker.Layouts.TestUtils.injectMockLayout;
import static net.osmtracker.Layouts.TestUtils.listFiles;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;
import static org.apache.commons.io.FileUtils.deleteDirectory;


public class DeleteLayoutTest {

    @Rule
    public ActivityTestRule<ButtonsPresets> mRule = new ActivityTestRule<>(ButtonsPresets.class);

    // Storage permissions are required
    @Rule
    public GrantPermissionRule readPermission = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermission = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static String layoutName = "mock";

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
     * Makes sure that only the mock layout exists
     */
    @BeforeClass
    public static void setUp(){
        try {
            deleteDirectory(getLayoutsDirectory());
            injectMockLayout(layoutName);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        assertFalse(filesAfterDeletion.contains(layoutName+"_es.xml"));

        // Check the icons folder was deleted
        assertFalse(filesAfterDeletion.contains(layoutName+"_icons"));

    }
}
