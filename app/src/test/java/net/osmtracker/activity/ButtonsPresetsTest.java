package net.osmtracker.activity;

import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class ButtonsPresetsTest {



    // Variables used in selectLayoutTest
    ButtonsPresets activity;
    private CheckBox pressedCheckbox, priorSelectedCheckbox;
    private Field selectedField;
    private String label = "SOME LABEL";
    SharedPreferences.Editor mockEditor;
    SharedPreferences mockPrefs;
    Field layoutFileNamesField;
    Field prefsField;
    Hashtable<String, String> mockHash;

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
        }; // TODO add invalid cases when the real method is improved (now fails with outOfBounds)

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
        try {
            setupMocksForSelectLayoutTest();
            callSelectLayout();
            makeAssertionsForSelectLayout();
        }catch (Exception e){
            System.out.println("Error testing selectLayout method");
            e.printStackTrace();
            fail();
        }


    }

    @Test
    @PrepareForTest({ButtonsPresets.class, Environment.class})
    public void refreshActivityTest() {
        try {
            ButtonsPresets mockActivity = mock(ButtonsPresets.class);

            injectMockHashtable(mockActivity, 1); // to check it's reset actually

            LinearLayout downloadedLayouts = mock(LinearLayout.class);
            LinearLayout defaultSection = mock(LinearLayout.class);


            when(mockActivity,"findViewById", R.id.list_layouts).thenReturn(downloadedLayouts);
            when(mockActivity,"findViewById",R.id.buttons_presets).thenReturn(defaultSection);


            mockStatic(Environment.class);
            when(Environment.getExternalStorageDirectory()).thenReturn(new File(""));

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
    public void initializeAttributesTest() {
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

    @Test
    @PrepareForTest({File.class, Preferences.class, Environment.class})
    public void listLayoutsTest(){
        for (int i = 0; i <= 1 ; i++) {
            try {
                doTestListLayouts(i);
            }
            catch (Exception e){
                e.printStackTrace();
                fail();
            }
        }
    }





    // TODO: make this test complete after refactoring the method,
    //  now tests only a small part because couldn't be tested deeply
    //  without refactoring or making it public
    private void doTestListLayouts(int numberOfDownloadedLayouts) throws Exception {
        ButtonsPresets mockActivity = mock(ButtonsPresets.class);

        mockStatic(Environment.class); // avoid getExternal call

        TextView mockEmpyText = mock(TextView.class);
        when(mockActivity.findViewById(R.id.btnpre_empty)).thenReturn(mockEmpyText);

        CheckBox mockDefaultcheckBox = mock(CheckBox.class);
        when(mockDefaultcheckBox.getText()).thenReturn("foo");
        when(mockActivity.findViewById(R.id.def_layout)).thenReturn(mockDefaultcheckBox);

        injectMockHashtable(mockActivity, numberOfDownloadedLayouts);

        callListLayouts(mockActivity, null);

        int expectedVisibility = (numberOfDownloadedLayouts > 1) ? View.INVISIBLE : View.VISIBLE;
        verify(mockEmpyText).setVisibility(expectedVisibility);




    }

    private void callListLayouts(ButtonsPresets mockActivity, LinearLayout mockRootLayout) {
        try {
            Method m = ButtonsPresets.class.getDeclaredMethod("listLayouts", LinearLayout.class);
            m.setAccessible(true);
            m.invoke(mockActivity, mockRootLayout);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
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
        assertEquals(File.separator+OSMTracker.Preferences.VAL_STORAGE_DIR, storageDir);
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

    private void injectMockHashtable(ButtonsPresets mockActivity, int numberOfEntries) {
        Hashtable internalHash = new Hashtable<String,String>();
        for (int i = 0; i < numberOfEntries; i++) {
            internalHash.put("foo", "bar");
        }
        Whitebox.setInternalState(mockActivity.getClass(), "layoutsFileNames", internalHash);
    }

    private void setupMocksForSelectLayoutTest() throws Exception{

        activity = new ButtonsPresets();

        // Mock a selected checkbox
        pressedCheckbox = mock(CheckBox.class);
        when(pressedCheckbox.getText()).thenReturn(label);

        // Mock and set a previously selected checkbox
        priorSelectedCheckbox = mock(CheckBox.class);
        selectedField = activity.getClass().getDeclaredField("selected");
        selectedField.setAccessible(true);
        selectedField.set(activity, priorSelectedCheckbox);

        // Mock and set the PreferencesEditor
        mockEditor = mock(SharedPreferences.Editor.class);
        when(mockEditor.commit()).thenReturn(true);
        when(mockEditor.putString("ui.buttons.layout", label)).thenReturn(mockEditor);


        // Mock prefs to use the mock editor
        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.edit()).thenReturn(mockEditor);

        // Mock and set the preferences
        prefsField = activity.getClass().getDeclaredField("prefs");
        prefsField.setAccessible(true);
        prefsField.set(activity, mockPrefs);

        // Mock and set the layoutFilenames Hashtable
        mockHash = mock(Hashtable.class);
        when(mockHash.get(label)).thenReturn(label);
        layoutFileNamesField = activity.getClass().getDeclaredField("layoutsFileNames");
        layoutFileNamesField.setAccessible(true);
        layoutFileNamesField.set(activity, mockHash);
    }

    private void callSelectLayout() throws Exception{
        Method selectLayoutMethod = activity.getClass().getDeclaredMethod("selectLayout", CheckBox.class);
        selectLayoutMethod.setAccessible(true);
        selectLayoutMethod.invoke(activity, pressedCheckbox);
    }

    private void makeAssertionsForSelectLayout() throws Exception{
        // Make sure the previously selected is unchecked
        verify(priorSelectedCheckbox).setChecked(false);

        // Make sure the just selected is checked
        verify(pressedCheckbox).setChecked(true);

        // Make sure selected variable is updated to match the just selected
        assertEquals(selectedField.get(activity), pressedCheckbox);

        // Make sure the value in SharedPreferences is updated to match the just selected
        verify(mockEditor).putString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, label);
        verify(mockEditor).commit();
    }

}

