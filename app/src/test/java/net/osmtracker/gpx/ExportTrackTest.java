package net.osmtracker.gpx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import net.osmtracker.R;
import net.osmtracker.data.GPXMocks;
import net.osmtracker.data.TrackMocks;
import net.osmtracker.data.TrackPointMocks;
import net.osmtracker.data.WayPointMocks;
import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static net.osmtracker.OSMTracker.Preferences.KEY_OUTPUT_FILENAME;
import static net.osmtracker.OSMTracker.Preferences.VAL_OUTPUT_FILENAME;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static net.osmtracker.util.UnitTestUtils.createDateFrom;
import static net.osmtracker.OSMTracker.Preferences;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PreferenceManager.class })
//@PrepareForTest({ PreferenceManager.class, Environment.class, ExportTrackTask.class,
//        ExportToStorageTask.class})
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

        Track mockTrack = TrackMocks.createMockTrack(trackName, trackStartDate);
        String result = task.buildGPXFilename(mockTrack, temporaryFolder.getRoot());

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

    @Test
    public void testBuildMetadataString() {
        Track track = TrackMocks.createMockTrack("Nombre de la traza",
                990055228011l);
        track.setTags("OSMTracker, bekuo");
        track.setDescription("Descripción de prueba");

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);
        String result = task.buildMetadataString(track);
        assertEquals(GPXMocks.MOCK_WAYPOINT_XML_A, result);

    }

    @Test
    public void testBuildTrackPointString() {
        TrackPoint trkpt = TrackPointMocks.getMockTrackPointForXML();
        boolean fillHDOP = false;
        String compass = "extension";

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockContext.getResources()).thenReturn(mockResources);

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);

        String result = task.buildTrackPointString(trkpt, fillHDOP, compass);
        assertEquals(TrackPointMocks.MOCK_TRACKPOINT_XML_A, result);
    }


    @Test
    public void testBuildWayPointString() {
        WayPoint wpt = WayPointMocks.getMockWayPointForXML();
        String accuracyInfo = "none";
        boolean fillHDOP = false;
        String compass = "none";

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockContext.getResources()).thenReturn(mockResources);

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);

        String result = task.buildWayPointString(wpt, accuracyInfo, fillHDOP, compass);
        assertEquals(WayPointMocks.MOCK_WAYPOINT_XML_A, result);
    }

    /**
    @Test
    public void testExportTrackAsGpx()  throws Exception {
        mockStatic(Environment.class);
        when(Environment.getExternalStorageState()).thenReturn(Environment.MEDIA_MOUNTED);
        File testStorageDirectory = new File("./src/test/assets/gpx/");
        when(Environment.getExternalStorageDirectory()).thenReturn(testStorageDirectory);

        // Mock preferences
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        // gpx output dir per track.
        when(mockPrefs.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_DIR_PER_TRACK,
                OSMTracker.Preferences.VAL_OUTPUT_GPX_OUTPUT_DIR_PER_TRACK)).thenReturn(true);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_STORAGE_DIR,
                OSMTracker.Preferences.VAL_STORAGE_DIR)).thenReturn("/osmtracker-test");
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
                OSMTracker.Preferences.VAL_OUTPUT_FILENAME)).thenReturn("name_date");
        mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockContext.getResources()).thenReturn(mockResources);

        DataHelper dhMock = new MockDataHelper(mockContext);
        whenNew(DataHelper.class).withAnyArguments().thenReturn(dhMock);


        long trackId = TrackMocks.GPX_TEST_TRACKID;
        ExportTrackTask task = new ExportToStorageTask(mockContext, trackId);
        File outputGPX = task.exportTrackAsGpx(trackId);

        File expectedGPX = new File("./src/test/assets/gpx/real-track.gpx");
        assertEquals(true, FileUtils.contentEquals(outputGPX, expectedGPX));
    }
    */

}
