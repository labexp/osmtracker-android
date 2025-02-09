package net.osmtracker.layouts;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static net.osmtracker.util.TestUtils.checkToastIsShownWith;
import static net.osmtracker.util.TestUtils.getStringResource;
import static org.hamcrest.core.IsNot.not;

import androidx.test.espresso.ViewAssertion;
import androidx.test.rule.ActivityTestRule;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.activity.AvailableLayouts;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

public class RepositorySettingsDialogTest {

    @Rule
    public ActivityTestRule<AvailableLayouts> mRule = new ActivityTestRule<>(AvailableLayouts.class);


    @Test
    public void testToggleBehaviour(){
        onView(withId(R.id.github_config)).perform(click());

        onView(withId(R.id.default_server)).perform(click(), closeSoftKeyboard());
        checkStateAfterToggle(R.id.default_server, R.id.custom_server);
        checkTextFieldsState(not(isEnabled()));
        checkTextFieldsDefaultValues();

        onView(withId(R.id.custom_server)).perform(click(), closeSoftKeyboard());
        checkStateAfterToggle(R.id.custom_server, R.id.default_server);
        checkTextFieldsState(isEnabled());
    }

    @Test
    public void testRepositoryValidation(){
        String validUser = OSMTracker.Preferences.VAL_GITHUB_USERNAME;
        String validRepository = OSMTracker.Preferences.VAL_REPOSITORY_NAME;
        String validBranch = OSMTracker.Preferences.VAL_BRANCH_NAME;
        String invalidBranch = "NONE";

        checkRepositoryValidity(validUser,validRepository,validBranch, true);
        checkRepositoryValidity(validUser,validRepository,invalidBranch, false);
    }


    public void checkStateAfterToggle(int expectedActiveId, int expectedInactiveId){
        onView(withId(expectedActiveId)).check(matches(not(isEnabled())));
        onView(withId(expectedActiveId)).check(matches(isChecked()));
        onView(withId(expectedInactiveId)).check(matches(not(isChecked())));
        onView(withId(expectedInactiveId)).check(matches(isEnabled()));
    }

    public void checkRepositoryValidity(String user, String repo, String branch, boolean isValid) {
        onView(withId(R.id.github_config)).perform(click());

        onView(withId(R.id.custom_server)).perform(click(), closeSoftKeyboard());

        onView(withId(R.id.github_username)).perform(clearText(), typeText(user), closeSoftKeyboard());
        onView(withId(R.id.repository_name)).perform(clearText(), typeText(repo), closeSoftKeyboard());
        onView(withId(R.id.branch_name)).perform(clearText(), typeText(branch), closeSoftKeyboard());

        onView(withText(getStringResource(R.string.menu_save))).perform(click());

        String expectedMessage = (isValid) ? getStringResource(R.string.github_repository_settings_valid_server) :
                getStringResource(R.string.github_repository_settings_invalid_server);

        checkToastIsShownWith(expectedMessage);

        ViewAssertion expectedDialogState = (isValid) ? doesNotExist() : matches(isDisplayed());
        checkDialogState(expectedDialogState);
    }

    /**
     * Check if the dialog is shown by looking for its title on the screen
     */
    private void checkDialogState(ViewAssertion assertion) {
        onView(withText(getStringResource(R.string.prefs_ui_github_repository_settings))).check(assertion);
    }

    /**
     * Check that the text fields values match the expected default ones
     */
    private void checkTextFieldsDefaultValues() {
        onView(withId(R.id.repository_name)).check(matches(withText(OSMTracker.Preferences.VAL_REPOSITORY_NAME)));
        onView(withId(R.id.branch_name)).check(matches(withText(OSMTracker.Preferences.VAL_BRANCH_NAME)));
        onView(withId(R.id.github_username)).check(matches(withText(OSMTracker.Preferences.VAL_GITHUB_USERNAME)));
    }

    /**
     * @param matcher can be isEnabled or not(isEnabled()) or any matcher
     */
    public void checkTextFieldsState(Matcher matcher){

        onView(withId(R.id.github_username)).check(matches(matcher));
        onView(withId(R.id.repository_name)).check(matches(matcher));
        onView(withId(R.id.branch_name)).check(matches(matcher));
    }


}
