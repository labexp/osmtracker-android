package net.osmtracker.service.gps;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.location.Location;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Bug-confirming tests for {@link GPSLogger}.
 *
 * <p>Every test in this class documents a <strong>known bug</strong> by asserting the current
 * broken behaviour. Each test passes <em>because</em> the bug exists — the tests are expected
 * to <strong>fail</strong> once the corresponding bug is fixed.
 *
 * <p>When a bug is fixed:
 * <ol>
 *   <li>Remove the {@literal @}Ignore annotation from the matching test in
 *       {@link GPSLoggerTest} (the intended-behaviour companion).</li>
 *   <li>Delete (or permanently skip) the test in this class — it no longer represents
 *       correct expected behaviour.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the formerly-{@literal @}Ignored test in
 *       {@link GPSLoggerTest} must now pass.</li>
 * </ol>
 *
 * <p>See {@code docs/BUGS.md} for the full description of each bug, including GitHub issue
 * templates and suggested fixes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class GPSLoggerTestBugs {

    private ServiceController<GPSLogger> controller;
    private GPSLogger service;
    private DataHelper mockDataHelper;

    @Before
    public void setUp() throws Exception {
        controller = Robolectric.buildService(GPSLogger.class).create();
        service = controller.get();

        mockDataHelper = mock(DataHelper.class);
        Field f = GPSLogger.class.getDeclaredField("dataHelper");
        f.setAccessible(true);
        f.set(service, mockDataHelper);
    }

    @After
    public void tearDown() {
        controller.destroy();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private void sendToReceiver(Intent intent) throws Exception {
        Field receiverField = GPSLogger.class.getDeclaredField("receiver");
        receiverField.setAccessible(true);
        BroadcastReceiver receiver = (BroadcastReceiver) receiverField.get(service);
        receiver.onReceive(service, intent);
    }

    // ── Bug B6: currentSegmentId is not volatile ──────────────────────────────

    /**
     * Bug B6 documentation — {@code currentSegmentId} is declared as a plain {@code long},
     * not {@code volatile}.
     *
     * <p>On a multi-core device, the GPS thread may observe a stale value written by
     * the main thread because there is no memory-visibility guarantee.
     * Robolectric runs single-threaded so the race cannot be reproduced here, but
     * this test confirms the field lacks the {@code volatile} modifier.
     *
     * <p>This test passes today because the field is <em>not</em> volatile (bug exists).
     * When Bug B6 is fixed (field made {@code volatile}), this test will fail
     * and should be deleted. Remove {@literal @}Ignore from
     * {@code GPSLoggerTest#currentSegmentId_isVolatile} at the same time.
     */
    @Test
    public void bug_B6_currentSegmentId_isNotVolatile() throws Exception {
        Field f = GPSLogger.class.getDeclaredField("currentSegmentId");
        // java.lang.reflect.Modifier.VOLATILE == 0x40 (64)
        boolean isVolatile = (f.getModifiers() & 0x40) != 0;
        assertFalse("Bug B6: currentSegmentId should be volatile but is not", isVolatile);
    }

    // ── Bug B7: empty catch swallows NPE on null link ─────────────────────────

    /**
     * Bug B7 — when the {@code link} extra is absent (null), {@code link.equals("null")}
     * throws NPE which is swallowed by an empty {@code catch(NullPointerException)} block.
     * The file path stays null and the attached file is silently never deleted.
     *
     * <p>This test verifies that {@code deleteWayPoint()} is still called (the waypoint DB
     * row is removed) even though the NPE is swallowed — confirming partial, broken
     * behaviour rather than full correct behaviour.
     *
     * <p>When Bug B7 is fixed (e.g., by using {@code "null".equals(link)} instead of
     * {@code link.equals("null")}), the null-link case will be handled without NPE and
     * the caller will correctly pass a {@code null} file path. This test will still pass
     * because the observable outcome ({@code deleteWayPoint} called with {@code null}
     * file path) is the same — but the internal NPE-swallowing path will no longer be
     * exercised. Delete this test once the fix is verified.
     */
    @Test
    public void bug_B7_deleteWaypoint_nullLink_swallowsNPEAndStillCallsDeleteWayPoint()
            throws Exception {
        String uuid = "test-uuid-null-link";
        Intent intent = new Intent(OSMTracker.INTENT_DELETE_WP);
        intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, 1L);
        intent.putExtra(OSMTracker.INTENT_KEY_UUID, uuid);
        // INTENT_KEY_LINK not set → extras.getString(...) returns null → NPE swallowed

        sendToReceiver(intent);

        // deleteWayPoint() is still called despite the swallowed NPE — confirms Bug B7
        verify(mockDataHelper).deleteWayPoint(eq(uuid), isNull());
    }
}
