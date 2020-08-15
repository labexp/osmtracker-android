package net.osmtracker.activity;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import net.osmtracker.R;
import net.osmtracker.util.TestUtils;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import static android.support.test.espresso.Espresso.onView;
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

        //FIXME: Only works if the db is empty.

        onView(withText(TestUtils.getStringResource(R.string.trackmgr_empty)))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void trackOptionsTest() {

        // create a new track
        onView(withId(R.id.trackmgr_hint_icon)).perform(click());

        // get Title from track Logger (Actual activity)
        String trackLoggerActivityTitle = getActivityInstance().getTitle().toString();
        String baseTitleText = TestUtils.getStringResource(R.string.tracklogger) +  ": #";
        String strTrackId = trackLoggerActivityTitle.replace(baseTitleText, "");

        // stop and save
        onView(withId(R.id.tracklogger_menu_stoptracking)).perform(click());

        // Track created?
        onView(withText("#"+strTrackId)).check(ViewAssertions.matches(isDisplayed()));

        //resume and stop tracking
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_resume))).perform(click());
        Espresso.pressBack();
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_stop))).perform(click());

        // TODO: check GPX contents
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_export))).perform(click());
        checkToastIsShownWith(TestUtils.getStringResource(R.string.various_export_finished));

        // delete track
        onView(withText("#"+strTrackId)).perform(longClick());
        onView(withText(TestUtils.getStringResource(R.string.trackmgr_contextmenu_delete))).perform(click());
        onView(withText("#"+strTrackId)).check(ViewAssertions.doesNotExist());
    }

    //https://stackoverflow.com/questions/38737127/espresso-how-to-get-current-activity-to-test-fragments?noredirect=1&lq=1
    private Activity getActivityInstance(){
        final Activity[] currentActivity = {null};

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable(){
            public void run(){
                Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                Iterator<Activity> it = resumedActivity.iterator();
                currentActivity[0] = it.next();
            }
        });

        return currentActivity[0];
    }

}
