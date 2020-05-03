package net.osmtracker.activity;

import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.widget.LinearLayout;

import net.osmtracker.R;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;

public class ButtonsPresetsTest {

    @Test
    public void getIsoTest(){
        ButtonsPresets activity = new ButtonsPresets();

        int VALUE = 0, EXPECTED = 1;
        String cases[][] = {
                {"test_es.xml", "es"},
                {"a_ge.xml", "ge"},
                {"en_fr.xml", "fr"},
                {"foo_en.xml", "en"},
                {"en.xml", "en"},
                {"", null},
                {".xml", null}
        };

        try{
            // Make the method callable
            Method getIso = activity.getClass().getDeclaredMethod("getIso", new Class[]{String.class});
            getIso.setAccessible(true);
            String result, expected;

            for(String[] option : cases){
                result = (String) getIso.invoke(activity, option[VALUE]);
                expected = option[EXPECTED];
                assertEquals(expected, result);
            }


        }catch(Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void selectLayoutTest() throws Exception {
        ButtonsPresets activity = new ButtonsPresets();

        String label = "SOME LABEL";

        // Mock a selected checkbox
        CheckBox pressedCheckbox = mock(CheckBox.class);
        when(pressedCheckbox.getText()).thenReturn(label);

        // Mock and set a previously selected checkbox
        CheckBox priorSelectedCheckbox = mock(CheckBox.class);
        Field selectedField = activity.getClass().getDeclaredField("selected");
        selectedField.setAccessible(true);
        selectedField.set(activity, priorSelectedCheckbox);

        // Mock and set the PreferencesEditor
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class); // TODO this
        when(mockEditor.commit()).thenReturn(true);
        when(mockEditor.putString("ui.buttons.layout", label)).thenReturn(mockEditor);


        // Mock prefs to use the mock editor
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        // Mock and set the preferences
        Field prefsField = activity.getClass().getDeclaredField("prefs");
        prefsField.setAccessible(true);
        prefsField.set(activity, mockPrefs);

        // Mock and set the layoutFilenames Hashtable
        Hashtable<String, String> mockHash = mock(Hashtable.class);
        when(mockHash.get(label)).thenReturn(label);
        Field layoutFileNamesField = activity.getClass().getDeclaredField("layoutsFileNames");
        layoutFileNamesField.setAccessible(true);
        layoutFileNamesField.set(activity, mockHash);

        // Call the method
        Method selectLayoutMethod = activity.getClass().getDeclaredMethod("selectLayout", CheckBox.class);
        selectLayoutMethod.setAccessible(true);
        selectLayoutMethod.invoke(activity, pressedCheckbox);

        // Assertions

        // Make sure the previously selected is unchecked
        verify(priorSelectedCheckbox).setChecked(false);

        // Make sure the just selected is checked
        verify(pressedCheckbox).setChecked(true);

        // Make sure selected variable is updated to match the just selected
        assertEquals(selectedField.get(activity), pressedCheckbox);

        // Make sure the value in SharedPreferences is updated to match the just selected
        verify(mockEditor).putString("ui.buttons.layout", label);
        verify(mockEditor).commit();
    }

    @Test
    @PrepareForTest(ButtonsPresets.class)
    public void refreshActivityTest() {
        try {
            ButtonsPresets mockActivity = mock(ButtonsPresets.class);

            injectOneEntryHashtable(mockActivity); // to check it's reset actually

            LinearLayout downloadedLayouts = mock(LinearLayout.class);
            LinearLayout defaultSection = mock(LinearLayout.class);


            when(mockActivity,"refreshActivity").thenCallRealMethod();


            when(mockActivity,"findViewById", R.id.list_layouts).thenReturn(downloadedLayouts);
            when(mockActivity,"findViewById",R.id.buttons_presets).thenReturn(defaultSection);

            // Actual method call
            when(mockActivity,"refreshActivity").thenCallRealMethod();
            mockActivity.refreshActivity();

            Hashtable internalHash = Whitebox.getInternalState(mockActivity.getClass(), "layoutsFileNames");
            assertEquals(0, internalHash.size());

            // Check internal method calls happen
            verifyPrivate(mockActivity).invoke("listLayouts", downloadedLayouts);
            verifyPrivate(mockActivity).invoke("checkCurrentLayout", downloadedLayouts, defaultSection);

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error testing refresh activity");
            fail();
        }
    }

    @Test
    @PrepareForTest(PreferenceManager.class)
    public void testInitializeAttributes() {
        ButtonsPresets mockActivity = mock(ButtonsPresets.class);

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.prefs_ui_buttons_layout)).thenReturn("fooTitle");
        when(mockActivity.getResources()).thenReturn(mockResources);

        // Mock the shared preferences
        mockDefaultSharedPreferences(mockActivity);

        // Call actual method
        callInitializeAttributes(mockActivity);

        // Check internal methods calls
        verify(mockActivity).setTitle("fooTitle");
        verify(mockActivity).setContentView(R.layout.buttons_presets);

        // Check attributes are set
        checkAttributesAfterInitialization(mockActivity);
    }

    private void mockDefaultSharedPreferences(Context context) {
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(mockPrefs);
    }

    private void checkAttributesAfterInitialization(ButtonsPresets mockActivity) {
        Hashtable hashtable = Whitebox.getInternalState(mockActivity.getClass(), "layoutsFileNames");
        Object listener = Whitebox.getInternalState(mockActivity, "listener");
        Object sharedPrefs = Whitebox.getInternalState(mockActivity, "prefs");
        String storageDir = Whitebox.getInternalState(mockActivity.getClass(),"storageDir");

        assertTrue(sharedPrefs instanceof SharedPreferences);
        assertEquals(0, hashtable.size());
        assertNotNull(listener);
        assertEquals("//osmtracker", storageDir);
    }

    private void callInitializeAttributes(ButtonsPresets mockActivity) {
        try {
            Method m = ButtonsPresets.class.getDeclaredMethod("initializeAttributes");
            m.setAccessible(true);
            m.invoke(mockActivity);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }

    private void injectOneEntryHashtable(ButtonsPresets mockActivity) {
        Hashtable internalHash = new Hashtable<String,String>();
        internalHash.put("foo", "bar");
        Whitebox.setInternalState(mockActivity.getClass(), "layoutsFileNames", internalHash);
    }
}