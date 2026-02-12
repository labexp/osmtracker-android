package net.osmtracker.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import net.osmtracker.OSMTracker
import net.osmtracker.R
import androidx.core.content.edit

class Intro : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable Edge-to-Edge support. Must be called before super.onCreate()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        // Set the colors for the bottom bar elements
        val activeColor = ContextCompat.getColor(this, R.color.colorAccent)
        val inactiveColor = ContextCompat.getColor(this, R.color.colorPrimary)

        setIndicatorColor(
            selectedIndicatorColor = activeColor,
            unselectedIndicatorColor = inactiveColor
        )

        setColorDoneText(activeColor)
        setColorSkipButton(activeColor)
        setNextArrowColor(activeColor)

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
                title = getString(R.string.app_intro_slide1_title),
                imageDrawable = R.drawable.icon_100x100,
                backgroundColorRes = R.color.appintro_background_color,
                description = getString(R.string.app_intro_slide1_description)
        ))

        // Whats new Fragment
        addSlide(AppIntroFragment.createInstance(
            title = getString(R.string.app_intro_slide_whats_new_title),
            imageDrawable = R.drawable.icon_100x100,
            backgroundColorRes = R.color.appintro_background_color,
            description = getString(R.string.app_intro_slide_whats_new_description)
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
        // Use the KTX extension for cleaner SharedPreferences editing
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putBoolean(
                OSMTracker.Preferences.KEY_DISPLAY_APP_INTRO,
                false
            )
        }
        finish()
    }
}
