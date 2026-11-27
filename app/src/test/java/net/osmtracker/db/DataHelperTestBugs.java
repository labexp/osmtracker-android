package net.osmtracker.db;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bug-confirming tests for {@link DataHelper}.
 *
 * <p>Every test in this class documents a <strong>known bug</strong> by asserting the current
 * broken behaviour. Each test passes <em>because</em> the bug exists — the tests are expected
 * to <strong>fail</strong> once the corresponding bug is fixed.
 *
 * <p>When a bug is fixed:
 * <ol>
 *   <li>Remove the {@literal @}Ignore annotation from the matching test in
 *       {@link DataHelperTest} (the intended-behaviour companion).</li>
 *   <li>Delete (or permanently skip) the test in this class — it no longer represents
 *       correct expected behaviour.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the formerly-{@literal @}Ignored test in
 *       {@link DataHelperTest} must now pass.</li>
 * </ol>
 *
 * <p>See {@code docs/BUGS.md} for the full description of each bug, including GitHub issue
 * templates and suggested fixes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class DataHelperTestBugs {

    private Context context;
    private DataHelper dataHelper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dataHelper = new DataHelper(context);
    }

    // ── Bug B1: wayPoint() NPE before null guard ──────────────────────────────

    /**
     * Bug B1 — {@code wayPoint()} calls {@code location.getExtras().getInt("satellites")}
     * before the {@code if (location != null)} guard, so a null location causes NPE
     * immediately rather than being skipped.
     *
     * <p>This test passes today because the NPE is thrown. When Bug B1 is fixed,
     * the method will silently skip null locations and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B1_wayPoint_throwsNPEWhenLocationIsNull() {
        long trackId = insertTestTrack();
        dataHelper.wayPoint(trackId, null, "name", null, UUID.randomUUID().toString(), 0, 0, 0);
    }

    /**
     * Bug B1 — when {@code location.getExtras()} returns {@code null} (extras never
     * initialised), {@code getExtras().getInt("satellites")} throws NPE.
     *
     * <p>This test passes today because the NPE is thrown. When Bug B1 is fixed,
     * this scenario will be handled gracefully and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B1_wayPoint_throwsNPEWhenLocationExtrasAreNull() {
        long trackId = insertTestTrack();
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        // intentionally no setExtras() — getExtras() returns null

        dataHelper.wayPoint(trackId, loc, "name", null, UUID.randomUUID().toString(), 0, 0, 0);
    }

    // ── Bug B2: trackNote() NPE on null extras ─────────────────────────────────

    /**
     * Bug B2 — {@code trackNote()} has no null guard at all before calling
     * {@code location.getExtras().getInt("satellites")}, so a location without an extras
     * bundle causes NPE.
     *
     * <p>This test passes today because the NPE is thrown. When Bug B2 is fixed,
     * this scenario will be handled gracefully and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B2_trackNote_throwsNPEWhenLocationExtrasAreNull() {
        long trackId = insertTestTrack();
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        // intentionally no setExtras()

        dataHelper.trackNote(trackId, loc, "note text", UUID.randomUUID().toString());
    }

    // ── Bug B3: getSegmentIdFor() cursor leak ─────────────────────────────────

    /**
     * Bug B3 — {@code getSegmentIdFor()} opens a cursor but never calls
     * {@code cursor.close()} in either the early-return or the normal-return code path.
     *
     * <p>This test passes today because {@code close()} is <em>never</em> called.
     * When Bug B3 is fixed, the cursor will be closed and this test will fail
     * (the {@code verify(never())} assertion will throw).
     */
    @Test
    public void bug_B3_getSegmentIdFor_doesNotCloseCursor() {
        ContentResolver mockCr = mock(ContentResolver.class);
        Cursor mockCursor = mock(Cursor.class);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);

        DataHelper.getSegmentIdFor(99999L, mockCr);

        // Passes today because close() is never called — confirms Bug B3
        verify(mockCursor, never()).close();
    }

    // ── Bug B4: getTrackById() null / empty cursor ────────────────────────────

    /**
     * Bug B4 — when the {@link android.content.ContentProvider} returns a {@code null}
     * cursor, {@code getTrackById()} immediately dereferences it, causing NPE.
     *
     * <p>This test passes today because NPE is thrown. When Bug B4 is fixed,
     * the method will return {@code null} gracefully and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B4_getTrackById_throwsNPEWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        dh.getTrackById(1L);
    }

    /**
     * Bug B4 — when no row matches the requested ID, the cursor is empty and
     * {@code cursor.moveToFirst()} returns {@code false}, but the code calls
     * {@code Track.build()} unconditionally, causing {@code CursorIndexOutOfBoundsException}.
     *
     * <p>This test passes today because the exception is thrown. When Bug B4 is fixed,
     * the method will return {@code null} for a missing ID and this test will fail.
     */
    @Test(expected = android.database.CursorIndexOutOfBoundsException.class)
    public void bug_B4_getTrackById_throwsWhenIdDoesNotExist() {
        dataHelper.getTrackById(99999L);
    }

    // ── Bug B5: getWayPointById() / getTrackPointById() null / empty cursor ───

    /**
     * Bug B5 — {@code getWayPointById()} does not guard against a {@code null} cursor,
     * causing NPE when the ContentProvider returns {@code null}.
     *
     * <p>This test passes today because NPE is thrown. When Bug B5 is fixed,
     * the method will return {@code null} gracefully and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B5_getWayPointById_throwsNPEWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        dh.getWayPointById(1);
    }

    /**
     * Bug B5 — when no row matches the requested waypoint ID, the empty cursor causes
     * {@code CursorIndexOutOfBoundsException}.
     *
     * <p>This test passes today because the exception is thrown. When Bug B5 is fixed,
     * the method will return {@code null} and this test will fail.
     */
    @Test(expected = android.database.CursorIndexOutOfBoundsException.class)
    public void bug_B5_getWayPointById_throwsWhenIdDoesNotExist() {
        dataHelper.getWayPointById(99999);
    }

    /**
     * Bug B5 — {@code getTrackPointById()} does not guard against a {@code null} cursor,
     * causing NPE when the ContentProvider returns {@code null}.
     *
     * <p>This test passes today because NPE is thrown. When Bug B5 is fixed,
     * the method will return {@code null} gracefully and this test will fail.
     */
    @Test(expected = NullPointerException.class)
    public void bug_B5_getTrackPointById_throwsNPEWhenCursorIsNull() {
        Context mockContext = mock(Context.class);
        ContentResolver mockCr = mock(ContentResolver.class);
        when(mockContext.getContentResolver()).thenReturn(mockCr);
        when(mockCr.query(any(Uri.class), any(), any(), any(), any())).thenReturn(null);

        DataHelper dh = new DataHelper(mockContext);
        dh.getTrackPointById(1);
    }

    /**
     * Bug B5 — when no row matches the requested trackpoint ID, the empty cursor causes
     * {@code CursorIndexOutOfBoundsException}.
     *
     * <p>This test passes today because the exception is thrown. When Bug B5 is fixed,
     * the method will return {@code null} and this test will fail.
     */
    @Test(expected = android.database.CursorIndexOutOfBoundsException.class)
    public void bug_B5_getTrackPointById_throwsWhenIdDoesNotExist() {
        dataHelper.getTrackPointById(99999);
    }

    // ── Bug B8: FILENAME_FORMATTER thread safety ───────────────────────────────

    /**
     * Bug B8 — {@code DataHelper.FILENAME_FORMATTER} is a shared, mutable
     * {@code public static final SimpleDateFormat}. {@link SimpleDateFormat} is not
     * thread-safe: concurrent calls to {@code format()} corrupt each other's output.
     *
     * <p>This test attempts to reproduce the race by formatting two distinct timestamps
     * simultaneously from multiple threads and checking that each result matches the
     * expected string. It may pass non-deterministically on single-core JVMs but
     * consistently exposes the bug under real concurrent load.
     *
     * <p>When Bug B8 is fixed (e.g., by using {@code ThreadLocal<SimpleDateFormat>}),
     * all threads will produce correct results and this test should be deleted in favour
     * of a simple deterministic formatting test.
     */
    @Test
    public void bug_B8_filenameFormatter_isNotThreadSafe() throws InterruptedException {
        // Two dates that format to obviously different strings
        final Date dateA = new Date(0L);               // 1970-01-01
        final Date dateB = new Date(1_000_000_000_000L); // 2001-09-09

        final String expectedA = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(dateA);
        final String expectedB = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(dateB);

        final int threads = 20;
        final int iterations = 100;
        final AtomicBoolean corruptionDetected = new AtomicBoolean(false);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final boolean useA = (t % 2 == 0);
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        Date d = useA ? dateA : dateB;
                        String expected = useA ? expectedA : expectedB;
                        String actual = DataHelper.FILENAME_FORMATTER.format(d);
                        if (!actual.equals(expected)) {
                            corruptionDetected.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads simultaneously
        assertTrue("Threads should finish within 10 seconds",
                doneLatch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        // Passes when corruption is detected (confirms bug).
        // On single-core JVMs this may occasionally not detect corruption — that is expected.
        // If this assertion consistently fails after a fix, the fix resolved the race.
        assertTrue("Bug B8: FILENAME_FORMATTER produced corrupted output under concurrent access " +
                "(expected on multi-core JVM — confirms thread-safety bug)", corruptionDetected.get());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private long insertTestTrack() {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(TrackContentProvider.Schema.COL_START_DATE, System.currentTimeMillis());
        values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
        android.net.Uri uri = context.getContentResolver().insert(
                TrackContentProvider.CONTENT_URI_TRACK, values);
        return android.content.ContentUris.parseId(uri);
    }
}
