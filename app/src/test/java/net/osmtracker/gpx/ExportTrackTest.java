package net.osmtracker.gpx;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import net.osmtracker.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static net.osmtracker.db.TrackContentProvider.Schema;
import static net.osmtracker.OSMTracker.Preferences.KEY_OUTPUT_FILENAME;
import static net.osmtracker.OSMTracker.Preferences.VAL_OUTPUT_FILENAME;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static net.osmtracker.util.UnitTestUtils.createDateFrom;
import static net.osmtracker.OSMTracker.Preferences;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PreferenceManager.class })
public class ExportTrackTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    ExportToStorageTask task;
    Context mockContext = Mockito.mock(Context.class);


    @Test
    public void testBuildGPXFilenameUsingOnlyTrackName() {
        // Method parameters
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = "MyTrack.gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);

    }

    @Test
    public void testBuildGPXFilenameUsingTrackNameAndStartDate() {
        // Method parameters
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME_DATE;

        String expectedFilename = "MyTrack_2000-01-02_03-04-05.gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameUsingOnlyStartDate() {
        // Method parameters
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_DATE;

        String expectedFilename = "2000-01-02_03-04-05.gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenSanitizesTrackName() {
        // Method parameters
        String trackNameInDatabase = ":M/y*T@r~a\\c?k:";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = ";M_y_T_r_a_c_k;.gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenUsesTrackNameButThereIsNoName() {
        // Method parameters
        String trackNameInDatabase = "";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = "2000-01-02_03-04-05.gpx"; // Must fallback to use the start date

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenUsesTrackNameAndStartDateButThereIsNoName() {
        // Method parameters
        String trackNameInDatabase = "";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME_DATE;

        String expectedFilename = "2000-01-02_03-04-05.gpx"; // Must fallback to use the start date

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }


    void doTestBuildGPXFilename(String trackName, String desiredFormat, long trackStartDate,
                                String expectedFilename) {
        setupPreferencesToReturn(desiredFormat);

        when(mockContext.getString(R.string.error_create_track_dir)).thenReturn("Any");

        task = new ExportToStorageTask(mockContext, 3);

        String result = task.buildGPXFilename(createMockCursor(trackName, trackStartDate), temporaryFolder.getRoot());

        assertEquals(expectedFilename, result);
    }


    // Used for testing buildGPXFilename
    void setupPreferencesToReturn(String desiredFormat) {
        // Mock preferences
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(KEY_OUTPUT_FILENAME, VAL_OUTPUT_FILENAME)).thenReturn(desiredFormat);

        mockStatic(PreferenceManager.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
    }


    // Used for testing buildGPXFilename
    Cursor createMockCursor(String trackName, long trackStartDate){
        Cursor mockCursor = Mockito.mock(Cursor.class);
        when(mockCursor.getColumnIndex(Schema.COL_NAME)).thenReturn(1);
        when(mockCursor.getString(1)).thenReturn(trackName);

        when(mockCursor.getColumnIndex(Schema.COL_START_DATE)).thenReturn(2);
        when(mockCursor.getLong(2)).thenReturn(trackStartDate);

        return mockCursor;

    }



}
