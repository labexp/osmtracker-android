package net.osmtracker.util;

import android.view.View;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

public class WaitForView implements ViewAction {
    private final int viewId;
    private final long timeout;

    /**
     * This ViewAction tells espresso to wait till a certain view is found in the view hierarchy.
     * @param viewId The id of the view to wait for.
     * @param timeout The maximum time which espresso will wait for the view to show up (in milliseconds)
     */
    public WaitForView(int viewId, long timeout) {
        this.viewId = viewId;
        this.timeout = timeout;
    }

    @Override
    public Matcher<View> getConstraints() {
        return ViewMatchers.isRoot();
    }

    @Override
    public String getDescription() {
        return "wait for a specific view with id " + viewId + " during " + timeout + " millis.";
    }

    @Override
    public void perform(UiController uiController, View rootView) {
        uiController.loopMainThreadUntilIdle();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;
        Matcher<View> viewMatcher = ViewMatchers.withId(viewId);

        do {
            for (View child : TreeIterables.breadthFirstViewTraversal(rootView)) {
                if (viewMatcher.matches(child)) {
                    return;
                }
            }
            uiController.loopMainThreadForAtLeast(100);
        } while (System.currentTimeMillis() < endTime);

        throw new PerformException.Builder()
                .withCause(new TimeoutException())
                .withActionDescription(this.getDescription())
                .withViewDescription(HumanReadables.describe(rootView))
                .build();
    }

    public static ViewAction waitForView(final int viewId, final long timeout) {
        return new WaitForView(viewId, timeout);
    }
}
