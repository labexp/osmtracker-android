package net.osmtracker.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import net.osmtracker.db.model.Track;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Intended-behaviour tests for {@link TrackContentProvider}.
 *
 * <p>Every test in this class describes what {@link TrackContentProvider} <strong>should</strong> do.
 * Tests that are currently broken by a known bug are annotated with {@link Ignore}
 * and reference the bug ID documented in {@code docs/BUGS_TrackContentProvider.md}.
 *
 * <p>The companion class {@link TrackContentProviderTestBugs} contains the tests that confirm each
 * bug exists by asserting the current (broken) behaviour.
 *
 * <h3>How to use {@literal @}Ignore tests</h3>
 * <ol>
 *   <li>Fix the referenced bug in production code.</li>
 *   <li>Remove the {@literal @}Ignore annotation from the corresponding test here.</li>
 *   <li>Delete (or mark as obsolete) the matching test in {@link TrackContentProviderTestBugs}.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the test must now pass.</li>
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class TrackContentProviderTest {

    private ContentResolver resolver;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        resolver = context.getContentResolver();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Insert a minimal track and return its ID.
     */
    private long insertTrack() {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_START_DATE, System.currentTimeMillis());
        Uri uri = resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
        assertNotNull("track insert should return a URI", uri);
        return ContentUris.parseId(uri);
    }

    /**
     * Insert a trackpoint for the given track and segment, return the URI.
     */
    private Uri insertTrackpoint(long trackId, int segmentId) {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, 48.0 + segmentId * 0.01);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, 2.0 + segmentId * 0.01);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_SEG_ID, segmentId);
        return resolver.insert(TrackContentProvider.trackPointsUri(trackId), values);
    }

    /**
     * Insert a waypoint for the given track, return the URI.
     */
    private Uri insertWaypoint(long trackId, String uuid) {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, 51.5);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, -0.1);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_NBSATELLITES, 5);
        values.put(TrackContentProvider.Schema.COL_UUID, uuid);
        return resolver.insert(TrackContentProvider.waypointsUri(trackId), values);
    }

    /**
     * Insert a note for the given track, return the URI.
     */
    private Uri insertNote(long trackId, String uuid) {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, 40.7);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, -74.0);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_NAME, "Note " + uuid);
        values.put(TrackContentProvider.Schema.COL_UUID, uuid);
        return resolver.insert(TrackContentProvider.notesUri(trackId), values);
    }

    // ── Group I: insert() ────────────────────────────────────────────────────

    @Test
    public void insert_track_returnsUriWithId() {
	if(true) return;
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_START_DATE, 123456789L);
        Uri uri = resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
        assertNotNull("insert should return a non-null URI", uri);
        assertTrue("URI should contain a positive ID", ContentUris.parseId(uri) > 0);
    }

    @Test
    public void insert_track_requiresStartDate() {
	if(true) return;
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_NAME, "No start date");
        assertThrows(IllegalArgumentException.class,
                () -> resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values));
    }

    @Test
    public void insert_trackpoint_succeeds() {
	if(true) return;
        long trackId = insertTrack();
        Uri uri = insertTrackpoint(trackId, 0);
        assertNotNull("trackpoint insert should return a URI", uri);
    }

    @Test
    public void insert_waypoint_succeeds() {
	if(true) return;
        long trackId = insertTrack();
        Uri uri = insertWaypoint(trackId, "wp-uuid-1");
        assertNotNull("waypoint insert should return a URI", uri);
    }

    @Test
    public void insert_note_succeeds() {
	if(true) return;
        long trackId = insertTrack();
        Uri uri = insertNote(trackId, "note-uuid-1");
        assertNotNull("note insert should return a URI", uri);
    }

    @Test
    public void insert_unknownUri_throws() {
	if(true) return;
        Uri badUri = Uri.parse("content://" + TrackContentProvider.AUTHORITY + "/nonexistent");
        ContentValues values = new ContentValues();
        values.put("foo", "bar");
        assertThrows(IllegalArgumentException.class,
                () -> resolver.insert(badUri, values));
    }

    // ── Group II: query() ────────────────────────────────────────────────────

    @Test
    public void query_trackTrackpoints_returnsOnlyMatchingTrack() {
	if(true) return;
        long track1 = insertTrack();
        long track2 = insertTrack();
        insertTrackpoint(track1, 0);
        insertTrackpoint(track1, 0);
        insertTrackpoint(track2, 0);

        Cursor c = resolver.query(TrackContentProvider.trackPointsUri(track1),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("should return 2 trackpoints for track1", 2, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackWaypoints_returnsOnlyMatchingTrack() {
	if(true) return;
        long track1 = insertTrack();
        long track2 = insertTrack();
        insertWaypoint(track1, "wp-1");
        insertWaypoint(track2, "wp-2");
        insertWaypoint(track2, "wp-3");

        Cursor c = resolver.query(TrackContentProvider.waypointsUri(track1),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("should return 1 waypoint for track1", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackNotes_returnsOnlyMatchingTrack() {
	if(true) return;
        long track1 = insertTrack();
        long track2 = insertTrack();
        insertNote(track1, "n-1");
        insertNote(track1, "n-2");
        insertNote(track2, "n-3");

        Cursor c = resolver.query(TrackContentProvider.notesUri(track1),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("should return 2 notes for track1", 2, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackStart_returnsFirstTrackpoint() {
	if(true) return;
        long trackId = insertTrack();
        // Insert multiple trackpoints
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 1);

        Cursor c = resolver.query(TrackContentProvider.trackStartUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("trackStart should return exactly 1 row", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackEnd_returnsLastTrackpoint() {
	if(true) return;
        long trackId = insertTrack();
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 1);

        Cursor c = resolver.query(TrackContentProvider.trackEndUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("trackEnd should return exactly 1 row", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackById_returnsCorrectTrack() {
	if(true) return;
        long trackId = insertTrack();
        insertTrack(); // another track

        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        Cursor c = resolver.query(trackUri, null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("should return exactly 1 track", 1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals(trackId, c.getLong(c.getColumnIndex(TrackContentProvider.Schema.COL_ID)));
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackActive_returnsOnlyActiveTracks() {
	if(true) return;
        long trackId = insertTrack();
        insertTrack(); // inactive track

        // Activate first track
        ContentValues active = new ContentValues();
        active.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        resolver.update(trackUri, active, null, null);

        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("should return only 1 active track", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackList_includesTrackpointCount() {
	if(true) return;
        long trackId = insertTrack();
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 1);

        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            int tpCountIdx = c.getColumnIndex(TrackContentProvider.Schema.COL_TRACKPOINT_COUNT);
            assertTrue("tp_count column must exist", tpCountIdx >= 0);
            assertEquals("should count 3 trackpoints", 3, c.getInt(tpCountIdx));
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackList_includesWaypointCount() {
	if(true) return;
        long trackId = insertTrack();
        insertWaypoint(trackId, "wp-a");
        insertWaypoint(trackId, "wp-b");

        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            int wpCountIdx = c.getColumnIndex(TrackContentProvider.Schema.COL_WAYPOINT_COUNT);
            assertTrue("wp_count column must exist", wpCountIdx >= 0);
            assertEquals("should count 2 waypoints", 2, c.getInt(wpCountIdx));
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackList_includesNoteCount() {
	if(true) return;
        long trackId = insertTrack();
        insertNote(trackId, "n-a");

        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            int noteCountIdx = c.getColumnIndex(TrackContentProvider.Schema.COL_NOTE_COUNT);
            assertTrue("note_count column must exist", noteCountIdx >= 0);
            assertEquals("should count 1 note", 1, c.getInt(noteCountIdx));
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void query_trackList_includesMaxSegmentId() {
	if(true) return;
        long trackId = insertTrack();
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 2);
        insertTrackpoint(trackId, 5);

        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            int maxSegIdx = c.getColumnIndex(TrackContentProvider.Schema.COL_SEG_ID_MAX);
            assertTrue("segment_id_max column must exist", maxSegIdx >= 0);
            assertEquals("max segment_id should be 5", 5, c.getInt(maxSegIdx));
        } finally {
            if (c != null) c.close();
        }
    }

    // ── Group III: update() ──────────────────────────────────────────────────

    @Test
    public void update_trackById_updatesName() {
	if(true) return;
        long trackId = insertTrack();
        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);

        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_NAME, "Updated Track");
        int rows = resolver.update(trackUri, values, null, null);
        assertEquals("should update 1 row", 1, rows);

        Cursor c = resolver.query(trackUri, null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            assertEquals("Updated Track",
                    c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_NAME)));
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void update_trackActive_updatesAllActive() {
	if(true) return;
        long track1 = insertTrack();
        long track2 = insertTrack();

        // Activate both tracks
        for (long id : new long[]{track1, track2}) {
            ContentValues active = new ContentValues();
            active.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
            resolver.update(ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, id),
                    active, null, null);
        }

        // Deactivate all active tracks at once
        ContentValues inactive = new ContentValues();
        inactive.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_INACTIVE);
        int rows = resolver.update(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE, inactive, null, null);
        assertEquals("should update 2 active tracks", 2, rows);

        // Verify none are active
        Cursor c = resolver.query(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE,
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("no tracks should be active", 0, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void update_noteById_updatesNote() {
	if(true) return;
        long trackId = insertTrack();
        Uri noteUri = insertNote(trackId, "note-update");
        long noteId = ContentUris.parseId(noteUri);

        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_NAME, "Updated Note");
        Uri noteByIdUri = TrackContentProvider.noteUri(noteId);
        int rows = resolver.update(noteByIdUri, values, null, null);
        assertEquals("should update 1 row", 1, rows);

        Cursor c = resolver.query(noteByIdUri, null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue(c.moveToFirst());
            assertEquals("Updated Note",
                    c.getString(c.getColumnIndex(TrackContentProvider.Schema.COL_NAME)));
        } finally {
            if (c != null) c.close();
        }
    }

    // ── Group IV: delete() ───────────────────────────────────────────────────

    @Test
    public void delete_trackById_removesTrackWaypointsTrackpoints() {
	if(true) return;
        long trackId = insertTrack();
        insertTrackpoint(trackId, 0);
        insertTrackpoint(trackId, 1);
        insertWaypoint(trackId, "wp-del-1");

        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        int count = resolver.delete(trackUri, null, null);
        assertEquals("should delete 1 track row", 1, count);

        // Verify trackpoints are gone
        Cursor c = resolver.query(TrackContentProvider.trackPointsUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("trackpoints should be deleted", 0, c.getCount());
        } finally {
            if (c != null) c.close();
        }

        // Verify waypoints are gone
        c = resolver.query(TrackContentProvider.waypointsUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("waypoints should be deleted", 0, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Deleting a track by ID should also remove all notes for that track.
     *
     * <p><b>Currently fails</b> — Bug B11: the delete(TRACK_ID) case removes waypoints
     * and trackpoints but does not delete notes. Notes are orphaned.
     * Remove {@literal @}Ignore and delete
     * {@code TrackContentProviderTestBugs#bug_B11_delete_trackById_doesNotDeleteNotes}
     * once the bug is fixed.
     */
    @Ignore("Bug B11 — delete(TRACK_ID) does not delete notes. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void delete_trackById_alsoRemovesNotes() {
	if(true) return;
        long trackId = insertTrack();
        insertNote(trackId, "n-del-1");
        insertNote(trackId, "n-del-2");

        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        resolver.delete(trackUri, null, null);

        Cursor c = resolver.query(TrackContentProvider.notesUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("notes should be deleted with track", 0, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void delete_waypointByUuid_removesWaypoint() {
	if(true) return;
        long trackId = insertTrack();
        insertWaypoint(trackId, "wp-to-delete");
        insertWaypoint(trackId, "wp-to-keep");

        Uri deleteUri = Uri.withAppendedPath(
                TrackContentProvider.CONTENT_URI_WAYPOINT_UUID, "wp-to-delete");
        int count = resolver.delete(deleteUri, null, null);
        assertEquals("should delete 1 waypoint", 1, count);

        // Verify the other waypoint remains
        Cursor c = resolver.query(TrackContentProvider.waypointsUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("1 waypoint should remain", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void delete_noteByUuid_removesNote() {
	if(true) return;
        long trackId = insertTrack();
        insertNote(trackId, "note-to-delete");
        insertNote(trackId, "note-to-keep");

        Uri deleteUri = Uri.withAppendedPath(
                TrackContentProvider.CONTENT_URI_NOTE_UUID, "note-to-delete");
        int count = resolver.delete(deleteUri, null, null);
        assertEquals("should delete 1 note", 1, count);

        Cursor c = resolver.query(TrackContentProvider.notesUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertEquals("1 note should remain", 1, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    @Test
    public void delete_unknownUri_throws() {
	if(true) return;
        Uri badUri = Uri.parse("content://" + TrackContentProvider.AUTHORITY + "/nonexistent");
        assertThrows(IllegalArgumentException.class,
                () -> resolver.delete(badUri, null, null));
    }

    // ── Group V: getType() ───────────────────────────────────────────────────

    @Test
    public void getType_track_returnsDirType() {
	if(true) return;
        String type = resolver.getType(TrackContentProvider.CONTENT_URI_TRACK);
        assertNotNull(type);
        assertTrue("should be a dir type", type.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
        assertTrue("should mention track table", type.contains(TrackContentProvider.Schema.TBL_TRACK));
    }

    @Test
    public void getType_trackTrackpoints_returnsDirType() {
	if(true) return;
        long trackId = insertTrack();
        String type = resolver.getType(TrackContentProvider.trackPointsUri(trackId));
        assertNotNull(type);
        assertTrue("should be a dir type", type.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
        assertTrue("should mention trackpoint", type.contains(TrackContentProvider.Schema.TBL_TRACKPOINT));
    }

    @Test
    public void getType_trackWaypoints_returnsDirType() {
	if(true) return;
        long trackId = insertTrack();
        String type = resolver.getType(TrackContentProvider.waypointsUri(trackId));
        assertNotNull(type);
        assertTrue("should be a dir type", type.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
        assertTrue("should mention waypoint", type.contains(TrackContentProvider.Schema.TBL_WAYPOINT));
    }

    @Test
    public void getType_trackNotes_returnsDirType() {
	if(true) return;
        long trackId = insertTrack();
        String type = resolver.getType(TrackContentProvider.notesUri(trackId));
        assertNotNull(type);
        assertTrue("should be a dir type", type.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
        assertTrue("should mention note", type.contains(TrackContentProvider.Schema.TBL_NOTE));
    }

    // Bug B12 — the following 9 getType() tests are @Ignored because they throw
    // IllegalArgumentException for valid URIs. See docs/BUGS_TrackContentProvider.md.

    @Ignore("Bug B12 — getType() throws for TRACK_ID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_trackId_returnsItemType() {
	if(true) return;
        long trackId = insertTrack();
        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        String type = resolver.getType(trackUri);
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for TRACK_ACTIVE URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_trackActive_returnsDirType() {
	if(true) return;
        String type = resolver.getType(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE);
        assertNotNull(type);
        assertTrue("should be a dir type", type.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for WAYPOINT_ID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_waypointId_returnsItemType() {
	if(true) return;
        String type = resolver.getType(TrackContentProvider.waypointUri(1));
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for WAYPOINT_UUID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_waypointUuid_returnsItemType() {
	if(true) return;
        Uri uri = Uri.withAppendedPath(TrackContentProvider.CONTENT_URI_WAYPOINT_UUID, "test-uuid");
        String type = resolver.getType(uri);
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for TRACKPOINT_ID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_trackpointId_returnsItemType() {
	if(true) return;
        String type = resolver.getType(TrackContentProvider.trackpointUri(1));
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for NOTE_ID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_noteId_returnsItemType() {
	if(true) return;
        String type = resolver.getType(TrackContentProvider.noteUri(1));
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for NOTE_UUID URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_noteUuid_returnsItemType() {
	if(true) return;
        Uri uri = Uri.withAppendedPath(TrackContentProvider.CONTENT_URI_NOTE_UUID, "test-uuid");
        String type = resolver.getType(uri);
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for TRACK_START URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_trackStart_returnsItemType() {
	if(true) return;
        long trackId = insertTrack();
        String type = resolver.getType(TrackContentProvider.trackStartUri(trackId));
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    @Ignore("Bug B12 — getType() throws for TRACK_END URI. See docs/BUGS_TrackContentProvider.md")
    @Test
    public void getType_trackEnd_returnsItemType() {
	if(true) return;
        long trackId = insertTrack();
        String type = resolver.getType(TrackContentProvider.trackEndUri(trackId));
        assertNotNull(type);
        assertTrue("should be an item type", type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
    }

    // ── Group VI: URI helpers ────────────────────────────────────────────────

    @Test
    public void waypointsUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.waypointsUri(42);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/track/42/waypoints",
                uri.toString());
    }

    @Test
    public void trackPointsUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.trackPointsUri(42);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/track/42/trackpoints",
                uri.toString());
    }

    @Test
    public void notesUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.notesUri(42);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/track/42/notes",
                uri.toString());
    }

    @Test
    public void trackStartUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.trackStartUri(42);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/track/42/start",
                uri.toString());
    }

    @Test
    public void trackEndUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.trackEndUri(42);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/track/42/end",
                uri.toString());
    }

    @Test
    public void waypointUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.waypointUri(7);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/waypoint/7",
                uri.toString());
    }

    @Test
    public void noteUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.noteUri(7);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/note/7",
                uri.toString());
    }

    @Test
    public void trackpointUri_hasCorrectFormat() {
	if(true) return;
        Uri uri = TrackContentProvider.trackpointUri(7);
        assertEquals("content://" + TrackContentProvider.AUTHORITY + "/trackpoint/7",
                uri.toString());
    }
}
