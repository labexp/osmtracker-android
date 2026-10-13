package net.osmtracker.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Intended-behaviour tests for {@link DataHelper}.
 *
 * <p>Every test in this class describes what {@link DataHelper} <strong>should</strong> do.
 * Tests that are currently broken by a known bug are annotated with {@link Ignore}
 * and reference the bug ID documented in {@code docs/BUGS.md}.
 *
 * <p>The companion class {@link DataHelperTestBugs} contains the tests that confirm each
 * bug exists by asserting the current (broken) behaviour.
 *
 * <h3>How to use {@literal @}Ignore tests</h3>
 * <ol>
 *   <li>Fix the referenced bug in production code.</li>
 *   <li>Remove the {@literal @}Ignore annotation from the corresponding test here.</li>
 *   <li>Delete (or mark as obsolete) the matching test in {@link DataHelperTestBugs}.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the test must now pass.</li>
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class DataHelperTest {

    private Context context;
    private DataHelper dataHelper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dataHelper = new DataHelper(context);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private long insertTestTrack() {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_START_DATE, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
        Uri uri = context.getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
        assertNotNull(uri);
        return ContentUris.parseId(uri);
    }

    private long insertTestTrackPoint(long trackId, double lat, double lon, long segId) {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, lat);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, lon);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_SEG_ID, segId);
        Uri uri = context.getContentResolver().insert(
                TrackContentProvider.trackPointsUri(trackId), values);
        assertNotNull(uri);
        return ContentUris.parseId(uri);
    }

    private long insertTestWayPoint(long trackId, String uuid) {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, 48.0);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, 2.0);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_NAME, "TestWP");
        values.put(TrackContentProvider.Schema.COL_NBSATELLITES, 5);
        if (uuid != null) values.put(TrackContentProvider.Schema.COL_UUID, uuid);
        Uri uri = context.getContentResolver().insert(
                TrackContentProvider.waypointsUri(trackId), values);
        assertNotNull(uri);
        return ContentUris.parseId(uri);
    }

    /** Returns a {@link Location} with an initialised extras Bundle. */
    private Location makeLocation() {
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        loc.setExtras(new Bundle());
        return loc;
    }

    // ── getSegmentIdFor() ─────────────────────────────────────────────────────

    /** Returns 0 when the track does not exist (no matching rows in DB). */
    @Test
    public void getSegmentIdFor_returnsZeroWhenTrackDoesNotExist() {
        long result = DataHelper.getSegmentIdFor(99999L, context.getContentResolver());
        assertEquals(0L, result);
    }

    /** Returns the highest segment ID recorded for the track. */
    @Test
    public void getSegmentIdFor_returnsMaxSegmentIdAcrossAllTrackpoints() {
        long trackId = insertTestTrack();
        insertTestTrackPoint(trackId, 48.0, 2.0, 1L);
        insertTestTrackPoint(trackId, 48.1, 2.1, 3L);

        long result = DataHelper.getSegmentIdFor(trackId, context.getContentResolver());
        assertEquals(3L, result);
    }

    /**
     * The cursor opened during the query must be closed when the method returns,
     * regardless of whether the track exists.
     *
     * <p><b>Currently fails</b> — Bug B3: {@code getSegmentIdFor()} never calls
     * {@code cursor.close()} in either code path.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B3_getSegmentIdFor_doesNotCloseCursor}
     * once the bug is fixed.
     */
    @Ignore("Bug B3 — getSegmentIdFor() never closes the cursor. See docs/BUGS.md")
    @Test
    public void getSegmentIdFor_closesCursorAfterQuery() {
        ContentResolver mockCr = mock(ContentResolver.class);
        Cursor mockCursor = mock(Cursor.class);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);

        DataHelper.getSegmentIdFor(99999L, mockCr);

        verify(mockCursor).close();
    }

    // ── getTrackById() ────────────────────────────────────────────────────────

    /** Returns a populated {@link Track} when the ID exists. */
    @Test
    public void getTrackById_returnsTrackForValidId() {
        long trackId = insertTestTrack();
        Track track = dataHelper.getTrackById(trackId);
        assertNotNull(track);
        assertEquals(trackId, track.getTrackId());
    }

    /**
     * Returns {@code null} when no track with the given ID exists.
     *
     * <p><b>Currently fails</b> — Bug B4: an empty cursor causes
     * {@code CursorIndexOutOfBoundsException} instead.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B4_getTrackById_throwsWhenIdDoesNotExist}
     * once the bug is fixed.
     */
    @Ignore("Bug B4 — getTrackById() throws on non-existent ID instead of returning null. See docs/BUGS.md")
    @Test
    public void getTrackById_returnsNullForNonExistentId() {
        Track result = dataHelper.getTrackById(99999L);
        assertNull(result);
    }

    /**
     * Returns {@code null} gracefully when the ContentResolver returns a null cursor
     * (e.g., due to a provider error) instead of throwing NPE.
     *
     * <p><b>Currently fails</b> — Bug B4: {@code c.getCount()} throws NPE on a null cursor.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B4_getTrackById_throwsNPEWhenCursorIsNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B4 — getTrackById() throws NPE on null cursor instead of returning null. See docs/BUGS.md")
    @Test
    public void getTrackById_returnsNullWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        Track result = dh.getTrackById(1L);
        assertNull(result);
    }

    // ── getWayPointById() ─────────────────────────────────────────────────────

    /** Returns a populated {@link WayPoint} when the ID exists. */
    @Test
    public void getWayPointById_returnsWayPointForValidId() {
        long trackId = insertTestTrack();
        long wpId = insertTestWayPoint(trackId, UUID.randomUUID().toString());
        WayPoint wp = dataHelper.getWayPointById((int) wpId);
        assertNotNull(wp);
    }

    /**
     * Returns {@code null} when no waypoint with the given ID exists.
     *
     * <p><b>Currently fails</b> — Bug B5: an empty cursor causes
     * {@code CursorIndexOutOfBoundsException} instead.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B5_getWayPointById_throwsWhenIdDoesNotExist}
     * once the bug is fixed.
     */
    @Ignore("Bug B5 — getWayPointById() throws on non-existent ID instead of returning null. See docs/BUGS.md")
    @Test
    public void getWayPointById_returnsNullForNonExistentId() {
        WayPoint result = dataHelper.getWayPointById(99999);
        assertNull(result);
    }

    /**
     * Returns {@code null} gracefully when the ContentResolver returns a null cursor.
     *
     * <p><b>Currently fails</b> — Bug B5: {@code cWayPoint.getCount()} throws NPE.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B5_getWayPointById_throwsNPEWhenCursorIsNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B5 — getWayPointById() throws NPE on null cursor instead of returning null. See docs/BUGS.md")
    @Test
    public void getWayPointById_returnsNullWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        WayPoint result = dh.getWayPointById(1);
        assertNull(result);
    }

    // ── getTrackPointById() ───────────────────────────────────────────────────

    /** Returns a populated {@link TrackPoint} when the ID exists. */
    @Test
    public void getTrackPointById_returnsTrackPointForValidId() {
        long trackId = insertTestTrack();
        long tpId = insertTestTrackPoint(trackId, 48.0, 2.0, 1L);
        TrackPoint tp = dataHelper.getTrackPointById((int) tpId);
        assertNotNull(tp);
    }

    /**
     * Returns {@code null} when no trackpoint with the given ID exists.
     *
     * <p><b>Currently fails</b> — Bug B5: an empty cursor causes
     * {@code CursorIndexOutOfBoundsException} instead.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B5_getTrackPointById_throwsWhenIdDoesNotExist}
     * once the bug is fixed.
     */
    @Ignore("Bug B5 — getTrackPointById() throws on non-existent ID instead of returning null. See docs/BUGS.md")
    @Test
    public void getTrackPointById_returnsNullForNonExistentId() {
        TrackPoint result = dataHelper.getTrackPointById(99999);
        assertNull(result);
    }

    /**
     * Returns {@code null} gracefully when the ContentResolver returns a null cursor.
     *
     * <p><b>Currently fails</b> — Bug B5: {@code cTrackPoint.getCount()} throws NPE.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B5_getTrackPointById_throwsNPEWhenCursorIsNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B5 — getTrackPointById() throws NPE on null cursor instead of returning null. See docs/BUGS.md")
    @Test
    public void getTrackPointById_returnsNullWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        TrackPoint result = dh.getTrackPointById(1);
        assertNull(result);
    }

    // ── wayPoint() ────────────────────────────────────────────────────────────

    /** Persists a waypoint row when location and extras are valid. */
    @Test
    public void wayPoint_persistsWaypointWithValidLocation() {
        long trackId = insertTestTrack();
        dataHelper.wayPoint(trackId, makeLocation(), "Cafe", null,
                UUID.randomUUID().toString(), -1, 0, 0);

        Cursor c = context.getContentResolver().query(
                TrackContentProvider.waypointsUri(trackId), null, null, null, null);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        c.close();
    }

    /**
     * Silently does nothing when {@code location} is {@code null} — no waypoint should
     * be persisted and no exception should be thrown.
     *
     * <p><b>Currently fails</b> — Bug B1: {@code location.getExtras()} is called before
     * the {@code if (location != null)} guard, causing NPE immediately.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B1_wayPoint_throwsNPEWhenLocationIsNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B1 — wayPoint() crashes before null guard when location is null. See docs/BUGS.md")
    @Test
    public void wayPoint_silentlyIgnoresNullLocation() {
        long trackId = insertTestTrack();
        dataHelper.wayPoint(trackId, null, "name", null, UUID.randomUUID().toString(), 0, 0, 0);

        Cursor c = context.getContentResolver().query(
                TrackContentProvider.waypointsUri(trackId), null, null, null, null);
        assertNotNull(c);
        assertEquals(0, c.getCount()); // no waypoint persisted
        c.close();
    }

    /**
     * Does not crash when {@code location.getExtras()} returns {@code null}
     * (extras bundle never initialised).
     *
     * <p><b>Currently fails</b> — Bug B1: {@code getExtras().getInt("satellites")} throws
     * NPE when extras is {@code null}.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B1_wayPoint_throwsNPEWhenLocationExtrasAreNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B1 — wayPoint() throws NPE when location has no extras bundle. See docs/BUGS.md")
    @Test
    public void wayPoint_doesNotCrashWhenLocationExtrasAreNull() {
        long trackId = insertTestTrack();
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        // No setExtras() call — getExtras() returns null

        dataHelper.wayPoint(trackId, loc, "name", null, UUID.randomUUID().toString(), 0, 0, 0);
    }

    // ── trackNote() ───────────────────────────────────────────────────────────

    /** Persists a note row when location and extras are valid. */
    @Test
    public void trackNote_persistsNoteWithValidLocation() {
        long trackId = insertTestTrack();
        dataHelper.trackNote(trackId, makeLocation(), "My note", UUID.randomUUID().toString());

        Cursor c = context.getContentResolver().query(
                TrackContentProvider.notesUri(trackId), null, null, null, null);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        c.close();
    }

    /**
     * Does not crash when {@code location.getExtras()} returns {@code null}.
     *
     * <p><b>Currently fails</b> — Bug B2: there is no null guard anywhere in
     * {@code trackNote()}; {@code getExtras().getInt("satellites")} throws NPE.
     * Remove {@literal @}Ignore and delete
     * {@code DataHelperTestBugs#bug_B2_trackNote_throwsNPEWhenLocationExtrasAreNull}
     * once the bug is fixed.
     */
    @Ignore("Bug B2 — trackNote() throws NPE when location has no extras bundle. See docs/BUGS.md")
    @Test
    public void trackNote_doesNotCrashWhenLocationExtrasAreNull() {
        long trackId = insertTestTrack();
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        // No setExtras() call

        dataHelper.trackNote(trackId, loc, "note text", UUID.randomUUID().toString());
    }

    // ── CRUD baselines ────────────────────────────────────────────────────────

    /** track() inserts a trackpoint row visible via the ContentProvider. */
    @Test
    public void track_insertsTrackPointIntoDb() {
        long trackId = insertTestTrack();
        dataHelper.track(trackId, makeLocation(), -1f, 0, 0f, 1L);

        Cursor c = context.getContentResolver().query(
                TrackContentProvider.trackPointsUri(trackId), null, null, null, null);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        c.close();
    }

    /** stopTracking() flips the track's active flag; getActiveTrackId() reflects it. */
    @Test
    public void stopTracking_marksTrackInactive() {
        long trackId = insertTestTrack();
        assertEquals(trackId, DataHelper.getActiveTrackId(context.getContentResolver()));

        dataHelper.stopTracking(trackId);

        assertEquals(-1L, DataHelper.getActiveTrackId(context.getContentResolver()));
    }

    /** getActiveTrackId() returns -1 when no active track exists in the DB. */
    @Test
    public void getActiveTrackId_returnsNegativeOneWhenNoActiveTrack() {
        assertEquals(-1L, DataHelper.getActiveTrackId(context.getContentResolver()));
    }

    /** setTrackName() persists the new name so that getTrackNameInDB() returns it. */
    @Test
    public void setTrackName_updatesNameInDb() {
        long trackId = insertTestTrack();
        DataHelper.setTrackName(trackId, "My Test Track", context.getContentResolver());
        assertEquals("My Test Track",
                DataHelper.getTrackNameInDB(trackId, context.getContentResolver()));
    }

    /** deleteWayPoint() removes the waypoint row from the database. */
    @Test
    public void deleteWayPoint_removesWayPointFromDb() {
        long trackId = insertTestTrack();
        String uuid = UUID.randomUUID().toString();
        insertTestWayPoint(trackId, uuid);

        dataHelper.deleteWayPoint(uuid, null);

        Cursor after = context.getContentResolver().query(
                TrackContentProvider.waypointsUri(trackId), null, null, null, null);
        assertNotNull(after);
        assertEquals(0, after.getCount());
        after.close();
    }
}
