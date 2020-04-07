package net.osmtracker.activity;

import android.content.SharedPreferences;
import android.widget.CheckBox;

import org.junit.Test;
import org.mockito.internal.matchers.Any;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
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
    public void selectLayoutTest() throws Exception{
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
}