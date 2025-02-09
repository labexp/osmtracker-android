package net.osmtracker.gpx;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static net.osmtracker.OSMTracker.Preferences;
import static net.osmtracker.OSMTracker.Preferences.KEY_OUTPUT_FILENAME;
import static net.osmtracker.OSMTracker.Preferences.VAL_OUTPUT_FILENAME;
import static net.osmtracker.db.TrackContentProvider.Schema;
import static net.osmtracker.util.UnitTestUtils.createDateFrom;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.exception.ExportTrackException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Date;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Environment.class, PreferenceManager.class})
@PowerMockIgnore("jdk.internal.reflect.*")
public class ExportToStorageTaskTest {

    @Rule
    private final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final Context mockContext = mock(Context.class);
    private final DataHelper mockDataHelper = mock(DataHelper.class);
    private final SharedPreferences mockPrefs = mock(SharedPreferences.class);
    private final Resources mockResources = mock(Resources.class);

    private ExportToStorageTask task;

    private static final String ERROR_CREATE_TRACK_DIR = "Error creating track directory";
    private static final String UNABLE_TO_WRITE_STORAGE = "Unable to write to external storage";

    @Before
    public void setUp() {
        mockStatic(Environment.class);
        mockStatic(PreferenceManager.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getString(R.string.error_create_track_dir)).thenReturn(ERROR_CREATE_TRACK_DIR);
        when(mockResources.getString(R.string.error_externalstorage_not_writable)).thenReturn(UNABLE_TO_WRITE_STORAGE);

        task = new ExportToStorageTask(mockContext, mockDataHelper, 1L);
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
    }

    @Test
    public void testBuildGPXFilenameUsingOnlyTrackName() {
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = "MyTrack_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameUsingTrackNameAndStartDate() {
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME_DATE;

        String expectedFilename = "MyTrack_2000-01-02_03-04-05_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameUsingStartDateAndTrackName() {
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_DATE_NAME;

        String expectedFilename = "2000-01-02_03-04-05_MyTrack_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameUsingOnlyStartDate() {
        String trackNameInDatabase = "MyTrack";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_DATE;

        String expectedFilename = "2000-01-02_03-04-05_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenSanitizesTrackName() {
        String trackNameInDatabase = ":M/y*T@r~a\\c?k:";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = ";M_y_T_r_a_c_k;_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx";

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenUsesTrackNameButThereIsNoName() {
        String trackNameInDatabase = "";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME;

        String expectedFilename = "2000-01-02_03-04-05_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx"; // Must fallback to use the start date

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    @Test
    public void testBuildGPXFilenameWhenUsesTrackNameAndStartDateButThereIsNoName() {
        String trackNameInDatabase = "";
        Date trackStartDate = createDateFrom(2000, 1, 2, 3, 4, 5);
        String preferenceSetting = Preferences.VAL_OUTPUT_FILENAME_NAME_DATE;

        String expectedFilename = "2000-01-02_03-04-05_"+Preferences.VAL_OUTPUT_FILENAME_LABEL+".gpx"; // Must fallback to use the start date

        doTestBuildGPXFilename(trackNameInDatabase, preferenceSetting, trackStartDate.getTime(), expectedFilename);
    }

    private void doTestBuildGPXFilename(String trackName, String desiredFormat, long trackStartDate, String expectedFilename) {
        when(mockPrefs.getString(KEY_OUTPUT_FILENAME, VAL_OUTPUT_FILENAME)).thenReturn(desiredFormat);

        String result = task.buildGPXFilename(createMockCursor(trackName, trackStartDate), temporaryFolder.getRoot());

        assertEquals(expectedFilename, result);
    }

    private Cursor createMockCursor(String trackName, long trackStartDate) {
        Cursor mockCursor = mock(Cursor.class);
        when(mockCursor.getColumnIndex(Schema.COL_NAME)).thenReturn(1);
        when(mockCursor.getString(1)).thenReturn(trackName);

        when(mockCursor.getColumnIndex(Schema.COL_START_DATE)).thenReturn(2);
        when(mockCursor.getLong(2)).thenReturn(trackStartDate);

        return mockCursor;
    }

    @Test
    public void testGetExportDirectoryWhenStorageIsWritableAndDirExists() throws Exception {
        // Mocking external storage state
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);

        // Mocking preferences and context
        when(mockPrefs.getString(any(), any())).thenReturn(OSMTracker.Preferences.VAL_STORAGE_DIR);
        when(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)).thenReturn(temporaryFolder.getRoot());

        var osmTrackerFolder = temporaryFolder.newFolder("osmtracker");

        // Creating task and invoking method
        File exportDirectory = task.getExportDirectory(new Date());

        // Verifying the directory path
        assertEquals(osmTrackerFolder.getAbsolutePath(), exportDirectory.getAbsolutePath());
    }

    @Test
    public void testGetExportDirectoryWhenStorageIsNotWritable() {
        // Mocking external storage state
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED_READ_ONLY);
        
        // Verifying the exception
        assertThrows(ExportTrackException.class, () -> task.getExportDirectory(new Date()));
    }

    @Test
    public void testGetExportDirectoryWhenStorageIsWritableAndDirNotExists() throws Exception {
        // Mocking external storage state
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);

        // Mocking preferences and context
        when(mockPrefs.getString(any(), any())).thenReturn(OSMTracker.Preferences.VAL_STORAGE_DIR);
        when(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)).thenReturn(temporaryFolder.getRoot());

        // Creating task and invoking method
        File exportDirectory = task.getExportDirectory(new Date());

        var osmTrackerFolder = new File(temporaryFolder.getRoot(), OSMTracker.Preferences.VAL_STORAGE_DIR);

        // Verifying the directory path
        assertEquals(osmTrackerFolder.getAbsolutePath(), exportDirectory.getAbsolutePath());
    }

    @Test
    public void testGetExportDirectoryWhenDirDoesNotExistAndCreatesIt() throws Exception {
        // Mocking external storage state
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);
        
        // Mocking preferences and context
        when(mockPrefs.getString(any(), any())).thenReturn("NonExistentDir");

        // Creating task and invoking method
        File exportDirectory = task.getExportDirectory(new Date());

        // Verifying the directory creation
        assertEquals(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NonExistentDir").getAbsolutePath(), exportDirectory.getAbsolutePath());
    }

    @Test
    public void testGetSanitizedTrackNameByStartDateWithValidTrackName() {
        // Mock track data
        Track mockTrack = new Track();
        mockTrack.setName("My/Track");
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        // Execute the method
        String result = task.getSanitizedTrackNameByStartDate(new Date());

        // Verify the sanitized track name
        assertEquals("My_Track", result);
    }

    @Test
    public void testGetSanitizedTrackNameByStartDateWithEmptyTrackName() {
        // Mock track data
        Track mockTrack = new Track();
        mockTrack.setName("");
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        // Execute the method
        String result = task.getSanitizedTrackNameByStartDate(new Date());

        // Verify the sanitized track name
        assertEquals("", result);
    }

    @Test
    public void testGetSanitizedTrackNameByStartDateWithNullTrackName() {
        // Mock track data
        Track mockTrack = new Track();
        mockTrack.setName(null);
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        // Execute the method
        String result = task.getSanitizedTrackNameByStartDate(new Date());

        // Verify the sanitized track name
		assertNull(result);
    }

    @Test
    public void testGetSanitizedTrackNameByStartDateWithSpecialCharacters() {
        // Mock track data
        Track mockTrack = new Track();
        mockTrack.setName("/M/y/T/r/@/c/k/");
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        // Execute the method
        String result = task.getSanitizedTrackNameByStartDate(new Date());

        // Verify the sanitized track name
        assertEquals("_M_y_T_r_@_c_k_", result);
    }

    @Test
    public void testGetSanitizedTrackNameByStartDateWithNoTrackFound() {
        // Mock no track data
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(null);

        // Execute the method
        String result = task.getSanitizedTrackNameByStartDate(new Date());

        // Verify the sanitized track name
        assertEquals("", result);
    }

    @Test
    public void testConstructorCallsSuperclassConstructor() {
        long trackId = 1L;

        // Use a spy to verify the constructor call
        var taskSpy = new ExportToStorageTask(mockContext, trackId);

        assertTrue(taskSpy.exportMediaFiles());
        assertTrue(taskSpy.updateExportDate());
    }

    @Test
    public void testCreateDirectoryPerTrack() throws Exception{
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);
        when(mockPrefs.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)).thenReturn(temporaryFolder.getRoot());
        when(mockPrefs.getString(any(), any())).thenReturn(OSMTracker.Preferences.VAL_STORAGE_DIR);
        Track mockTrack = new Track();
        mockTrack.setName("MyTrack");
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        task.getExportDirectory(new Date());
    }

    @Test
    public void testCreateDirectoryPerTrackEmptyTrackname() throws Exception{
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);
        when(mockPrefs.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        when(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)).thenReturn(temporaryFolder.getRoot());
        when(mockPrefs.getString(any(), any())).thenReturn(OSMTracker.Preferences.VAL_STORAGE_DIR);
        Track mockTrack = new Track();
        mockTrack.setName("");
        when(mockDataHelper.getTrackByStartDate(any(Date.class))).thenReturn(mockTrack);

        task.getExportDirectory(new Date());
    }
}
