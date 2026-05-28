package net.osmtracker.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.OSMTracker;
import net.osmtracker.activity.OpenStreetMapUpload;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.model.Track;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ContentProviderController;
import org.robolectric.android.util.concurrent.InlineExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.common.errors.OsmAuthorizationException;
import de.westnordost.osmapi.traces.GpsTraceDetails;
import de.westnordost.osmapi.traces.GpsTracesApi;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class UploadToOpenStreetMapTaskTest {

    private static final long TEST_TRACK_ID = 42L;

    private OpenStreetMapUpload activity;
    private File gpxFile;
    private ContentProviderController<TrackContentProvider> providerController;

    // ---------------------------------------------------------------------------
    // Stub GpsTracesApi subclasses — used instead of Mockito to avoid Byte Buddy
    // conflicts with Robolectric's instrumented class loader.
    // ---------------------------------------------------------------------------

    private static class SuccessGpsTracesApi extends GpsTracesApi {
        SuccessGpsTracesApi() { super(new OsmConnection("", "", "")); }

        @Override
        public long create(String name, GpsTraceDetails.Visibility visibility,
                           String description, List<String> tags, InputStream gpx) {
            return 99L;
        }
    }

    private static class AuthErrorGpsTracesApi extends GpsTracesApi {
        AuthErrorGpsTracesApi() { super(new OsmConnection("", "", "")); }

        @Override
        public long create(String name, GpsTraceDetails.Visibility visibility,
                           String description, List<String> tags, InputStream gpx) {
            throw new OsmAuthorizationException(401, "Unauthorized", "");
        }
    }

    private static class GenericErrorGpsTracesApi extends GpsTracesApi {
        GenericErrorGpsTracesApi() { super(new OsmConnection("", "", "")); }

        @Override
        public long create(String name, GpsTraceDetails.Visibility visibility,
                           String description, List<String> tags, InputStream gpx) {
            throw new RuntimeException("unexpected server error");
        }
    }

    /**
     * Subclass of the task that replaces GpsTracesApi with a stub, avoiding
     * any real network calls.
     */
    private static class StubUploadTask extends UploadToOpenStreetMapTask {
        private final GpsTracesApi stubApi;

        StubUploadTask(OpenStreetMapUpload activity, String token, long trackId,
                       File gpxFile, String filename, String description,
                       String tags, Track.OSMVisibility visibility,
                       GpsTracesApi stubApi) {
            super(activity, token, trackId, gpxFile, filename, description, tags, visibility);
            this.stubApi = stubApi;
        }

        @Override
        protected GpsTracesApi createGpsTracesApi(OsmConnection connection) {
            return stubApi;
        }
    }

    @Before
    public void setUp() throws IOException {
        ShadowLog.stream = System.out;
        OpenStreetMapConstants.setDevelopmentMode(true);

        // AsyncTask must run synchronously so tests can check side-effects inline
        ShadowPausedAsyncTask.overrideExecutor(new InlineExecutorService());

        // ContentProvider backing so DataHelper.setTrackUploadDate has a valid target
        providerController = Robolectric.buildContentProvider(TrackContentProvider.class)
                .create(TrackContentProvider.AUTHORITY);

        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_ID, TEST_TRACK_ID);
        values.put(TrackContentProvider.Schema.COL_START_DATE, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_NAME, "Test Track");
        values.put(TrackContentProvider.Schema.COL_DESCRIPTION, "Test description");
        values.put(TrackContentProvider.Schema.COL_TAGS, "test");
        values.put(TrackContentProvider.Schema.COL_OSM_VISIBILITY,
                Track.OSMVisibility.Private.toString());
        ApplicationProvider.getApplicationContext().getContentResolver()
                .insert(TrackContentProvider.CONTENT_URI_TRACK, values);

        // Fake OAuth token so the host activity doesn't try to open the browser
        PreferenceManager.getDefaultSharedPreferences(
                ApplicationProvider.getApplicationContext())
                .edit()
                .putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, "fake_token")
                .apply();

        // Build the host activity needed to show dialogs
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                OpenStreetMapUpload.class);
        intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, TEST_TRACK_ID);
        activity = Robolectric.buildActivity(OpenStreetMapUpload.class, intent)
                .create().start().resume().get();

        // Real temp file — the stub API ignores the stream content
        gpxFile = File.createTempFile("test_upload", ".gpx");
        gpxFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        ShadowPausedAsyncTask.reset();
        if (providerController != null) {
            providerController.shutdown();
        }
        ShadowContentResolver.reset();
        if (gpxFile != null) {
            gpxFile.delete();
        }
    }

    @Test
    public void constructor_nullDefaults_appliedCorrectly() throws Exception {
        UploadToOpenStreetMapTask task = new UploadToOpenStreetMapTask(
                activity, "token", TEST_TRACK_ID, gpxFile,
                "file.gpx", null, null, null);

        Field descField = UploadToOpenStreetMapTask.class.getDeclaredField("description");
        descField.setAccessible(true);
        assertEquals("test", descField.get(task));

        Field tagsField = UploadToOpenStreetMapTask.class.getDeclaredField("tags");
        tagsField.setAccessible(true);
        assertEquals("test", tagsField.get(task));

        Field visField = UploadToOpenStreetMapTask.class.getDeclaredField("visibility");
        visField.setAccessible(true);
        assertEquals(Track.OSMVisibility.Private, visField.get(task));
    }

    @Test
    public void doInBackground_success_updatesUploadDateInDbAndShowsSuccessDialog()
            throws Exception {
        StubUploadTask task = new StubUploadTask(
                activity, "fake_token", TEST_TRACK_ID, gpxFile,
                "test.gpx", "desc", "tags", Track.OSMVisibility.Public,
                new SuccessGpsTracesApi());
        task.execute();
        shadowOf(Looper.getMainLooper()).idle();

        // Success dialog was shown
        assertNotNull("Success dialog should be displayed", ShadowAlertDialog.getLatestAlertDialog());

        // DataHelper.setTrackUploadDate updated COL_OSM_UPLOAD_DATE via ContentProvider
        Uri trackUri = android.content.ContentUris.withAppendedId(
                TrackContentProvider.CONTENT_URI_TRACK, TEST_TRACK_ID);
        try (Cursor cursor = ApplicationProvider.getApplicationContext()
                .getContentResolver().query(trackUri, null, null, null, null)) {
            assertTrue("Track row should exist", cursor != null && cursor.moveToFirst());
            long uploadDate = cursor.getLong(
                    cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE));
            assertTrue("COL_OSM_UPLOAD_DATE should be set after successful upload",
                    uploadDate > 0);
        }
    }

    @Test
    public void doInBackground_gpxFileMissing_showsErrorDialogAndDoesNotSetUploadDate() {
        // Use a non-existent file so FileInputStream throws IOException
        File nonExistentFile = new File("/nonexistent/path/test.gpx");

        UploadToOpenStreetMapTask task = new UploadToOpenStreetMapTask(
                activity, "fake_token", TEST_TRACK_ID, nonExistentFile,
                "test.gpx", "desc", "tags", Track.OSMVisibility.Private);
        task.execute();
        shadowOf(Looper.getMainLooper()).idle();

        // Error dialog was shown (resultCode = -1)
        assertNotNull("Error dialog should be displayed", ShadowAlertDialog.getLatestAlertDialog());

        // COL_OSM_UPLOAD_DATE must remain 0 (not set) when the file is missing
        Uri trackUri = android.content.ContentUris.withAppendedId(
                TrackContentProvider.CONTENT_URI_TRACK, TEST_TRACK_ID);
        try (Cursor cursor = ApplicationProvider.getApplicationContext()
                .getContentResolver().query(trackUri, null, null, null, null)) {
            assertTrue(cursor != null && cursor.moveToFirst());
            long uploadDate = cursor.getLong(
                    cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE));
            assertEquals("COL_OSM_UPLOAD_DATE should remain 0 when upload fails", 0L, uploadDate);
        }
    }

    @Test
    public void doInBackground_authorizationException_showsAuthDialogAndDoesNotSetUploadDate()
            throws Exception {
        StubUploadTask task = new StubUploadTask(
                activity, "fake_token", TEST_TRACK_ID, gpxFile,
                "test.gpx", "desc", "tags", Track.OSMVisibility.Private,
                new AuthErrorGpsTracesApi());
        task.execute();
        shadowOf(Looper.getMainLooper()).idle();

        // Due to switch fall-through bug: ProgressDialog (index 0) + auth AlertDialog (index 1)
        List<android.app.Dialog> shown = ShadowDialog.getShownDialogs();
        assertTrue("At least ProgressDialog + auth dialog must be shown", shown.size() >= 2);
        assertNotNull("Auth dialog must be shown", shown.get(1));

        // COL_OSM_UPLOAD_DATE must remain 0 on auth failure
        Uri trackUri = android.content.ContentUris.withAppendedId(
                TrackContentProvider.CONTENT_URI_TRACK, TEST_TRACK_ID);
        try (Cursor cursor = ApplicationProvider.getApplicationContext()
                .getContentResolver().query(trackUri, null, null, null, null)) {
            assertTrue(cursor != null && cursor.moveToFirst());
            long uploadDate = cursor.getLong(
                    cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE));
            assertEquals("COL_OSM_UPLOAD_DATE should remain 0 on auth failure", 0L, uploadDate);
        }
    }

    @Test
    public void doInBackground_genericException_showsErrorDialogAndDoesNotSetUploadDate()
            throws Exception {
        StubUploadTask task = new StubUploadTask(
                activity, "fake_token", TEST_TRACK_ID, gpxFile,
                "test.gpx", "desc", "tags", Track.OSMVisibility.Private,
                new GenericErrorGpsTracesApi());
        task.execute();
        shadowOf(Looper.getMainLooper()).idle();

        assertNotNull("Error dialog should be displayed", ShadowAlertDialog.getLatestAlertDialog());

        Uri trackUri = android.content.ContentUris.withAppendedId(
                TrackContentProvider.CONTENT_URI_TRACK, TEST_TRACK_ID);
        try (Cursor cursor = ApplicationProvider.getApplicationContext()
                .getContentResolver().query(trackUri, null, null, null, null)) {
            assertTrue(cursor != null && cursor.moveToFirst());
            long uploadDate = cursor.getLong(
                    cursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_UPLOAD_DATE));
            assertEquals("COL_OSM_UPLOAD_DATE should remain 0 on generic failure", 0L, uploadDate);
        }
    }

    @Test
    public void onPostExecute_authError_yesButton_clearsStoredToken() throws Exception {
        PreferenceManager.getDefaultSharedPreferences(
                ApplicationProvider.getApplicationContext())
                .edit()
                .putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, "stored_token")
                .apply();

        StubUploadTask task = new StubUploadTask(
                activity, "stored_token", TEST_TRACK_ID, gpxFile,
                "test.gpx", "desc", "tags", Track.OSMVisibility.Private,
                new AuthErrorGpsTracesApi());
        task.execute();
        shadowOf(Looper.getMainLooper()).idle();

        // Auth dialog is at index 1 (after the ProgressDialog at index 0)
        List<android.app.Dialog> shown = ShadowDialog.getShownDialogs();
        android.app.AlertDialog authDialog = (android.app.AlertDialog) shown.get(1);
        assertNotNull(authDialog);

        // Click YES to clear stored credentials
        authDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertFalse("OAuth token must be removed after clicking YES",
                PreferenceManager.getDefaultSharedPreferences(
                        ApplicationProvider.getApplicationContext())
                        .contains(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN));
    }
}
