package net.osmtracker.util;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class ThemeValidatorTest {
    Context mockContext;
    SharedPreferences mockPrefs;
    Resources mockRes;
    Editor mockEditor;

    /** Setup all the mocks(classes) that are used  when calling the
     * ThemeValidator class with a selected theme
    * @param theme
    */
    public void setupMocks(String theme) {

        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_UI_THEME,
                OSMTracker.Preferences.VAL_UI_THEME))
                .thenReturn(theme);


        String[] themes = {	"net.osmtracker:style/DefaultTheme",
		"net.osmtracker:style/DarkTheme",
		"net.osmtracker:style/LightTheme",
		"net.osmtracker:style/HighContrast"};

        mockRes = mock(Resources.class);
        when(mockRes.getStringArray(R.array.prefs_theme_values))
                .thenReturn(themes);


        mockContext = mock(Context.class);

        when(mockContext.getSharedPreferences(mockContext.getString(R.string.shared_pref), MODE_PRIVATE)).thenReturn(mockPrefs);

        mockEditor=mock(SharedPreferences.Editor.class);

        when(mockPrefs.edit())
                .thenReturn(mockEditor);

    }

    @Test
    public void validateDefaultTheme(){
        setupMocks("net.osmtracker:style/DefaultTheme");
        String result =ThemeValidator.getValidTheme(mockPrefs, mockRes);
        String expected = "net.osmtracker:style/DefaultTheme";
        assertEquals(result, expected);
    }

    /*Use a theme that is not included on the theme values array and also
     *  verify methods of the mocked editor so that the preferences are saved.*/
    @Test
    public void validateWrongTheme(){

        setupMocks("net.osmtracker:style/YellowTheme");
        String result =ThemeValidator.getValidTheme(mockPrefs, mockRes);
        String expected = "net.osmtracker:style/DefaultTheme";
        assertEquals(result, expected);
        verify(mockPrefs,atLeastOnce()).edit();
        verify(mockEditor,atLeastOnce()).putString(OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
        verify(mockEditor).apply();
    }
}
