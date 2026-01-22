package net.osmtracker.activity;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.CheckBox;
import android.os.Environment;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;

import java.lang.reflect.Field;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;

import androidx.preference.PreferenceManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ButtonsPresetsTest {

    ButtonsPresets activity;

	@Before
	public void setUp() {
		// Build and start the activity lifecycle
		activity = Robolectric.buildActivity(ButtonsPresets.class)
				.create()
				.start()
				.resume()
				.get();
	}

	@Test
	public void getIsoTest() throws Exception {
		int VALUE = 0, EXPECTED = 1;
		String[][] cases = {
				{"test_es.xml", "es"},
				{"a_ge.xml", "ge"},
				{"en_fr.xml", "fr"},
				{"foo_en.xml", "en"},
				{"en.xml", "en"},
		};

		Method getIso = ButtonsPresets.class.getDeclaredMethod("getIso", String.class);
		getIso.setAccessible(true);

		for (String[] option : cases) {
			String result = (String) getIso.invoke(activity, option[VALUE]);
			assertEquals(option[EXPECTED], result);
		}
	}

	@Test
	public void testSelectLayout_UpdatesUIAndPreferences() throws Exception {
		// 1. Setup: Create two CheckBoxes to simulate "old" and "new" selection
		CheckBox oldCheckBox = new CheckBox(activity);
		oldCheckBox.setText("Default");
		oldCheckBox.setChecked(true);

		CheckBox newCheckBox = new CheckBox(activity);
		newCheckBox.setText("Cycling");
		newCheckBox.setChecked(false);

		// 2. Setup: Populate the internal Hashtable and 'selected' field via reflection
		Hashtable<String, String> mockLayouts = new Hashtable<>();
		mockLayouts.put("Default", "default.xml");
		mockLayouts.put("Cycling", "cycling_en.xml");

		setInternalState(ButtonsPresets.class, "layoutsFileNames", mockLayouts);
		setInternalState(activity, "selected", oldCheckBox);

		// 3. Execution: Invoke the private selectLayout method
		Method selectLayoutMethod =
				ButtonsPresets.class.getDeclaredMethod("selectLayout", CheckBox.class);
		selectLayoutMethod.setAccessible(true);
		selectLayoutMethod.invoke(activity, newCheckBox);

		// 4. Assertions: Verify UI State
		Assert.assertFalse("Previous checkbox should be unchecked", oldCheckBox.isChecked());
		Assert.assertTrue("New checkbox should be checked", newCheckBox.isChecked());

		// 5. Assertions: Verify Internal State
		CheckBox currentSelected = (CheckBox) getInternalState(activity, "selected");
		Assert.assertEquals(
				"Internal 'selected' field should be updated",newCheckBox, currentSelected);

		// 6. Assertions: Verify Persistence in SharedPreferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		String savedLayout = prefs.getString(OSMTracker.Preferences.KEY_UI_BUTTONS_LAYOUT, null);
		Assert.assertEquals("SharedPreferences should store the filename from the map",
				"cycling_en.xml", savedLayout);
	}

	@Test
	@SuppressWarnings("unchecked") // Suppress cast warning for the internal Hashtable
	public void testRefreshActivity_PopulatesUIFromFilesystem() throws Exception {
		// 1. Setup: Mock the SD Card being mounted
		ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

		// 2. Setup: Create dummy layout files in the expected directory
		// The path logic in ButtonsPresets uses: getExternalFilesDir(null) + /osmtracker/layouts
		File externalDir = activity.getExternalFilesDir(null);
		File layoutsDir = new File(externalDir, "osmtracker" + File.separator + "layouts");
		if (!layoutsDir.exists()) {
			Assert.assertTrue(layoutsDir.mkdirs());
		}

		// Create two dummy layout files
		File layout1 = new File(layoutsDir, "hiking_en.xml");
		File layout2 = new File(layoutsDir, "cycling_es.xml");
		Assert.assertTrue(layout1.createNewFile());
		Assert.assertTrue(layout2.createNewFile());

		// 3. Execution: Trigger the refresh
		activity.refreshActivity();

		// 4. Assertions: Check internal state (the Hashtable)
		// We use reflection to get the private static field 'layoutsFileNames'
		Hashtable<String, String> layoutsMap = 	(Hashtable<String, String>) getInternalState(
				ButtonsPresets.class, "layoutsFileNames");

		Assert.assertNotNull("Hashtable should be initialized", layoutsMap);
		// Map should contain 'hiking', 'cycling', and 'Default' (from defaultCheckBox)
		Assert.assertTrue("Should contain 'hiking' layout", layoutsMap.containsKey("hiking"));
		Assert.assertTrue("Should contain 'cycling' layout", layoutsMap.containsKey("cycling"));

		// 5. Assertions: Check UI state (the LinearLayout)
		LinearLayout listLayouts = activity.findViewById(R.id.list_layouts);

		// Count how many CheckBoxes were added.
		// listLayouts should contain CheckBoxes for every file found.
		int checkBoxCount = 0;
		for (int i = 0; i < listLayouts.getChildCount(); i++) {
			if (listLayouts.getChildAt(i) instanceof CheckBox) {
				checkBoxCount++;
			}
		}

		assertEquals("Two checkboxes should have been added to the UI", 2, checkBoxCount);

		// 6. Verification: Check 'Empty Message' visibility
		TextView emptyText = activity.findViewById(R.id.btnpre_empty);
		Assert.assertEquals("Empty message should be INVISIBLE because layouts exist",
				View.INVISIBLE, emptyText.getVisibility());
	}

	private void setInternalState(Object target, String fieldName, Object value) throws Exception {
		Field field;
		if (target instanceof Class) {
			field = ((Class<?>) target).getDeclaredField(fieldName);
		} else {
			field = target.getClass().getDeclaredField(fieldName);
		}
		field.setAccessible(true);
		field.set(target instanceof Class ? null : target, value);
	}

	private Object getInternalState(Object target, String fieldName) throws Exception {
		Field field;
		if (target instanceof Class) {
			field = ((Class<?>) target).getDeclaredField(fieldName);
		} else {
			field = target.getClass().getDeclaredField(fieldName);
		}
		field.setAccessible(true);
		return field.get(target instanceof Class ? null : target);
	}

}

