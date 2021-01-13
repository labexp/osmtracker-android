package net.osmtracker.gpx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.data.GPXMocks;
import net.osmtracker.data.MockDataHelper;
import net.osmtracker.data.TrackMocks;
import net.osmtracker.data.TrackPointMocks;
import net.osmtracker.data.WayPointMocks;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
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
@PrepareForTest({ PreferenceManager.class, ExportTrackTask.class })
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
    public void testWriteGPXFile() throws Exception {

        // Mock preferences
        SharedPreferences mockPrefs = mock(SharedPreferences.class);

        when(mockPrefs.getString(OSMTracker.Preferences.KEY_OUTPUT_ACCURACY,
                OSMTracker.Preferences.VAL_OUTPUT_ACCURACY)).thenReturn("none");
        when(mockPrefs.getBoolean(OSMTracker.Preferences.KEY_OUTPUT_GPX_HDOP_APPROXIMATION,
                OSMTracker.Preferences.VAL_OUTPUT_GPX_HDOP_APPROXIMATION)).thenReturn(true);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_OUTPUT_COMPASS,
                OSMTracker.Preferences.VAL_OUTPUT_COMPASS)).thenReturn("extension");
        mockStatic(PreferenceManager.class);
        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockResources.getString(R.string.gpx_track_name))
                .thenReturn("Trazado con OSMTracker para Android™");
        when(mockResources.getString(R.string.gpx_hdop_approximation_cmt))
                .thenReturn("Advertencia: los valores HDOP no son los valores HDOP devueltos por el"
                        + " dispositivo GPS. Son valores aproximados de acuerdo a la precisión de"
                        + " la ubicación en metros.");
        when(mockContext.getResources()).thenReturn(mockResources);

        Track track = TrackMocks.getMockTrackForGPX();
        File resultGPX = new File("./src/test/assets/gpx/output-test.gpx");
        ExportTrackTask task = new ExportToStorageTask(mockContext, TrackMocks.GPX_TRACKID);

        DataHelper dhMock = new MockDataHelper(mockContext);
        whenNew(DataHelper.class).withAnyArguments().thenReturn(dhMock);

        task.writeGpxFile(track, resultGPX);
        File expectedGPX = new File("./src/test/assets/gpx/gpx-test.gpx");
        assertEquals(true, FileUtils.contentEquals(resultGPX, expectedGPX));

    }

    @Test
    public void testBuildMetadataString() {
        Track track = TrackMocks.createMockTrack("Nombre de la traza",
                990055228011l);
        track.setTags("OSMTracker, bekuo");
        track.setDescription("Descripción de prueba");

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);
        String result = task.buildMetadataString(track);
        assertEquals(GPXMocks.MOCK_METADATA_XML_A, result);

    }

    @Test
    public void testBuildTrackPointString() {
        TrackPoint trkpt = TrackPointMocks.getMockTrackPointForXML();
        String result;
        boolean fillHDOP;
        String[] compassValues = {"none", "comment", "extension"};
        String[] expectedResults = {TrackPointMocks.MOCK_TRACKPOINT_XML_A,
                TrackPointMocks.MOCK_TRACKPOINT_XML_B, TrackPointMocks.MOCK_TRACKPOINT_XML_C,
                TrackPointMocks.MOCK_TRACKPOINT_XML_D, TrackPointMocks.MOCK_TRACKPOINT_XML_E,
                TrackPointMocks.MOCK_TRACKPOINT_XML_F, TrackPointMocks.MOCK_TRACKPOINT_XML_G
        };
        int expectedResultIndex= 0;

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockContext.getResources()).thenReturn(mockResources);

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);

        fillHDOP = false;
        for (String compass : compassValues) {
            result = task.buildTrackPointString(trkpt, fillHDOP, compass);
            assertEquals(expectedResults[expectedResultIndex], result);
            expectedResultIndex = expectedResultIndex + 1;
        }
        fillHDOP = true;
        for (String compass : compassValues) {
            result = task.buildTrackPointString(trkpt, fillHDOP, compass);
            assertEquals(expectedResults[expectedResultIndex], result);
            expectedResultIndex = expectedResultIndex + 1;
        }

        //hdop = true, compass = none and trkpt.Accuracy = null
        trkpt.setAccuracy(null);
        result = task.buildTrackPointString(trkpt, true, "none");
        assertEquals(expectedResults[expectedResultIndex], result);


    }


    @Test
    public void testBuildWayPointString() {
        WayPoint wpt = WayPointMocks.getMockWayPointForXML();
        String result;
        boolean fillHDOP;
        String[] accuracyInfoValues = {"none", "wpt_name", "wpt_cmt"};
        String[] compassValues = {"none", "comment", "extension"};
        String[] expectedResults = {WayPointMocks.MOCK_WAYPOINT_XML_A,
                WayPointMocks.MOCK_WAYPOINT_XML_B, WayPointMocks.MOCK_WAYPOINT_XML_C,
                WayPointMocks.MOCK_WAYPOINT_XML_D, WayPointMocks.MOCK_WAYPOINT_XML_E,
                WayPointMocks.MOCK_WAYPOINT_XML_F, WayPointMocks.MOCK_WAYPOINT_XML_G,
                WayPointMocks.MOCK_WAYPOINT_XML_H, WayPointMocks.MOCK_WAYPOINT_XML_I,
                WayPointMocks.MOCK_WAYPOINT_XML_J, WayPointMocks.MOCK_WAYPOINT_XML_K,
                WayPointMocks.MOCK_WAYPOINT_XML_L, WayPointMocks.MOCK_WAYPOINT_XML_M,
                WayPointMocks.MOCK_WAYPOINT_XML_N, WayPointMocks.MOCK_WAYPOINT_XML_O,
                WayPointMocks.MOCK_WAYPOINT_XML_P, WayPointMocks.MOCK_WAYPOINT_XML_Q,
                WayPointMocks.MOCK_WAYPOINT_XML_R
        };
        int expectedResultIndex= 0;

        // Setup the mock resources
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.various_unit_meters)).thenReturn("m");
        when(mockResources.getString(R.string.various_accuracy)).thenReturn("Precisión");
        when(mockContext.getResources()).thenReturn(mockResources);

        ExportTrackTask task = new ExportToStorageTask(mockContext, 3);

        fillHDOP = false;
        for (String compass : compassValues) {
            for (String accuracyInfo : accuracyInfoValues) {
                //System.out.println("buildWayPointString with " + accuracyInfo  + " "
                //        + fillHDOP + " " + compass);
                result = task.buildWayPointString(wpt, accuracyInfo, fillHDOP, compass);
                assertEquals(expectedResults[expectedResultIndex], result);
                expectedResultIndex = expectedResultIndex + 1;
            }
        }
        fillHDOP = true;
        for (String compass : compassValues) {
            for (String accuracyInfo : accuracyInfoValues) {
                //System.out.println("buildWayPointString with " + accuracyInfo  + " "
                //        + fillHDOP + " " + compass);
                result = task.buildWayPointString(wpt, accuracyInfo, fillHDOP, compass);
                assertEquals(expectedResults[expectedResultIndex], result);
                expectedResultIndex = expectedResultIndex + 1;
            }
        }

        //compass = extension, accuracy = comment. hdop = true
        //wpt.accuracy == null and wpt.atmosphericPressure == 5.54321
        wpt.setAccuracy(null);
        wpt.setAtmosphericPressure(5.54321);
        //wpt.setCompassHeading(null);
        result = task.buildWayPointString(wpt, "comment", true,
                "extension");
        assertEquals(WayPointMocks.MOCK_WAYPOINT_XML_S, result);

        //compass = extension, accuracy = comment. hdop = true
        //wpt.accuracy == null, wpt.atmosphericPressure == 5.54321, wpt.compassHeading == null
        // (but wpt.compassAccuracy != null)
        wpt.setCompassHeading(null);
        result = task.buildWayPointString(wpt, "comment", true,
                "extension");
        assertEquals(WayPointMocks.MOCK_WAYPOINT_XML_T, result);
    }

}
