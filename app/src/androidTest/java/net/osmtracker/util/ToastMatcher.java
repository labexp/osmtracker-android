package net.osmtracker.util;

import android.os.IBinder;
import androidx.test.espresso.Root;
import android.view.WindowManager;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Used to search for Toasts in the UI
 * This class was taken from <a href="https://stackoverflow.com/questions/28390574/checking-toast-message-in-android-espresso">stackoverflow</a>
 */
public class ToastMatcher extends TypeSafeMatcher<Root> {

    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        if ((type == WindowManager.LayoutParams.TYPE_TOAST)) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            if (windowToken == appToken) {
                return true;
            }
        }
        return false;
    }
}