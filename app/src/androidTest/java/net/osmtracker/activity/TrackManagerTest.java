package net.osmtracker.activity;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.widget.TextView;

import net.osmtracker.R;
import net.osmtracker.util.TestUtils;

import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static net.osmtracker.util.TestUtils.checkToastIsShownWith;

public class TrackManagerTest {

    @Rule
    public ActivityTestRule<TrackManager> mRule = new ActivityTestRule<>(TrackManager.class);

    @Test
    public void emptyTracksTest() {

        TextView tv_emptymsg = (TextView) mRule.getActivity().findViewById(R.id.trackmgr_empty);
        if (tv_emptymsg.getVisibility() == View.VISIBLE) {
            onView(withText(TestUtils.getStringResource(R.string.trackmgr_empty)))
                    .check(ViewAssertions.matches(isDisplayed()));

            // TODO: check that settings menu only shows settings and about message.
            //openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

            // create a new track
            onView(withId(R.id.trackmgr_hint_icon)).perform(click());
            checkToastIsShownWith(TestUtils.getStringResource(R.string.tracklogger_waiting_gps));
            // stop and save
            onView(withId(R.id.tracklogger_menu_stoptracking)).perform(click());

        }

        // There is at least one track
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_empty)))
                .check(ViewAssertions.matches(not(isDisplayed())));

        // delete all tracks
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(TestUtils.getStringResource(R.string.menu_deletetracks))).perform(click());
        //confirmation button
        onView(withText(TestUtils.getStringResource(R.string.menu_deletetracks))).perform(click());

        // check empty tracks list message
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_empty)))
                .check(ViewAssertions.matches(isDisplayed()));


    }

    @Test
    public void trackOptionsTest() {

        // create a new track
        onView(withId(R.id.trackmgr_hint_icon)).perform(click());
        checkToastIsShownWith(TestUtils.getStringResource(R.string.tracklogger_waiting_gps));

        // get Title from track Logger (Actual activity)
        String trackLoggerActivityTitle = TestUtils.getActivityInstance().getTitle().toString();
        String baseTitleText = TestUtils.getStringResource(R.string.tracklogger) +  ": #";
        String strTrackId = trackLoggerActivityTitle.replace(baseTitleText, "");

        // stop and save
        onView(withId(R.id.tracklogger_menu_stoptracking)).perform(click());

        // Track created?
        onView(withText("#"+strTrackId)).check(ViewAssertions.matches(isDisplayed()));

        //resume and stop tracking
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_resume))).perform(click());
        checkToastIsShownWith(TestUtils.getStringResource(R.string.tracklogger_waiting_gps));

        Espresso.pressBack();
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_stop))).perform(click());

        // export track.  TODO: check GPX contents
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_export))).perform(click());
        onView(allOf(withId(R.id.trackmgr_item_statusicon), hasSibling(withText("#"+strTrackId))))
                .check(ViewAssertions.matches(isDisplayed()));


        // delete track
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_delete))).perform(click());
        onView(withText("#"+strTrackId)).check(ViewAssertions.doesNotExist());
    }


}
