package net.osmtracker.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Bug-confirming tests for {@link TrackContentProvider}.
 *
 * <p>Every test in this class documents a <strong>known bug</strong> by asserting the current
 * broken behaviour. Each test passes <em>because</em> the bug exists — the tests are expected
 * to <strong>fail</strong> once the corresponding bug is fixed.
 *
 * <p>When a bug is fixed:
 * <ol>
 *   <li>Remove the {@literal @}Ignore annotation from the matching test in
 *       {@link TrackContentProviderTest} (the intended-behaviour companion).</li>
 *   <li>Delete (or permanently skip) the test in this class — it no longer represents
 *       correct expected behaviour.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the formerly-{@literal @}Ignored test in
 *       {@link TrackContentProviderTest} must now pass.</li>
 * </ol>
 *
 * <p>See {@code docs/BUGS_TrackContentProvider.md} for the full description of each bug.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class TrackContentProviderTestBugs {

    private ContentResolver resolver;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        resolver = context.getContentResolver();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private long insertTrack() {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_START_DATE, System.currentTimeMillis());
        Uri uri = resolver.insert(TrackContentProvider.CONTENT_URI_TRACK, values);
        assertNotNull(uri);
        return ContentUris.parseId(uri);
    }

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

    // ── Bug B11: delete(TRACK_ID) does not delete notes ──────────────────────

    /**
     * Bug B11 — When deleting a track by ID, the associated notes are NOT deleted.
     *
     * <p>The {@code delete()} method in {@code TrackContentProvider} handles
     * {@code URI_CODE_TRACK_ID} by deleting waypoints and trackpoints, but it does
     * not delete notes from the note table. This leaves orphaned note rows in the database.
     *
     * <p>This test passes because the bug exists (notes remain after track deletion).
     * When Bug B11 is fixed, this test will fail and should be deleted.
     * Remove {@literal @}Ignore from
     * {@code TrackContentProviderTest#delete_trackById_alsoRemovesNotes}.
     *
     * @see TrackContentProviderTest#delete_trackById_alsoRemovesNotes
     */
    @Test
    public void bug_B11_delete_trackById_doesNotDeleteNotes() {
        long trackId = insertTrack();
        insertNote(trackId, "orphan-note-1");
        insertNote(trackId, "orphan-note-2");

        // Delete the track
        Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        resolver.delete(trackUri, null, null);

        // Verify notes are still present (the bug)
        Cursor c = resolver.query(TrackContentProvider.notesUri(trackId),
                null, null, null, null);
        try {
            assertNotNull(c);
            assertTrue("Bug B11: notes should remain after track deletion (they are orphaned)",
                    c.getCount() > 0);
            assertEquals("Bug B11: both notes should be orphaned", 2, c.getCount());
        } finally {
            if (c != null) c.close();
        }
    }

    // ── Bug B12: getType() throws for 9 valid URI codes ──────────────────────

    /**
     * Bug B12 — {@code getType()} throws {@code IllegalArgumentException} for
     * {@code URI_CODE_TRACK_ID}. Only 4 of 13 valid URI codes are handled.
     *
     * <p>Each test below confirms that getType() throws for a valid URI.
     * When Bug B12 is fixed, these tests will fail and should be deleted.
     * Remove {@literal @}Ignore from the corresponding tests in
     * {@link TrackContentProviderTest}.
     */
    @Test
    public void bug_B12_getType_throwsForTrackId() {
        long trackId = insertTrack();
        Uri uri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
        assertThrows("Bug B12: getType should not throw for track/#",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForTrackActive() {
        assertThrows("Bug B12: getType should not throw for track/active",
                IllegalArgumentException.class,
                () -> resolver.getType(TrackContentProvider.CONTENT_URI_TRACK_ACTIVE));
    }

    @Test
    public void bug_B12_getType_throwsForWaypointId() {
        Uri uri = TrackContentProvider.waypointUri(1);
        assertThrows("Bug B12: getType should not throw for waypoint/#",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForWaypointUuid() {
        Uri uri = Uri.withAppendedPath(
                TrackContentProvider.CONTENT_URI_WAYPOINT_UUID, "test-uuid");
        assertThrows("Bug B12: getType should not throw for waypoint/uuid/*",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForTrackpointId() {
        Uri uri = TrackContentProvider.trackpointUri(1);
        assertThrows("Bug B12: getType should not throw for trackpoint/#",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForNoteId() {
        Uri uri = TrackContentProvider.noteUri(1);
        assertThrows("Bug B12: getType should not throw for note/#",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForNoteUuid() {
        Uri uri = Uri.withAppendedPath(
                TrackContentProvider.CONTENT_URI_NOTE_UUID, "test-uuid");
        assertThrows("Bug B12: getType should not throw for note/uuid/*",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForTrackStart() {
        long trackId = insertTrack();
        Uri uri = TrackContentProvider.trackStartUri(trackId);
        assertThrows("Bug B12: getType should not throw for track/#/start",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }

    @Test
    public void bug_B12_getType_throwsForTrackEnd() {
        long trackId = insertTrack();
        Uri uri = TrackContentProvider.trackEndUri(trackId);
        assertThrows("Bug B12: getType should not throw for track/#/end",
                IllegalArgumentException.class,
                () -> resolver.getType(uri));
    }
}
