package net.osmtracker.activity

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import net.osmtracker.OSMTracker
import net.osmtracker.R

class Intro : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
                title = getString(R.string.app_intro_slide1_title),
                imageDrawable = R.drawable.icon_100x100,
                backgroundColorRes = R.color.appintro_background_color,
                description = getString(R.string.app_intro_slide1_description)
        ))

        //TODO: change the image of slide number 2.
        addSlide(AppIntroFragment.createInstance(
                title = getString(R.string.app_intro_slide2_title),
                imageDrawable = R.drawable.icon_100x100,
                backgroundColorRes = R.color.appintro_background_color,
                description = getString(R.string.app_intro_slide2_description)
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO, false).apply()
        finish()
    }
}
