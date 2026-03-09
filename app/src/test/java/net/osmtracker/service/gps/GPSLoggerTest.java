package net.osmtracker.service.gps;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import net.osmtracker.OSMTracker;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Intended-behaviour tests for {@link GPSLogger}.
 *
 * <p>Every test in this class describes what {@link GPSLogger} <strong>should</strong> do.
 * Tests that are currently broken by a known bug are annotated with {@link Ignore}
 * and reference the bug ID documented in {@code docs/BUGS.md}.
 *
 * <p>The companion class {@link GPSLoggerTestBugs} contains the tests that confirm each
 * bug exists by asserting the current (broken) behaviour.
 *
 * <h3>How to use {@literal @}Ignore tests</h3>
 * <ol>
 *   <li>Fix the referenced bug in production code.</li>
 *   <li>Remove the {@literal @}Ignore annotation from the corresponding test here.</li>
 *   <li>Delete (or mark as obsolete) the matching test in {@link GPSLoggerTestBugs}.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the test must now pass.</li>
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class GPSLoggerTest {

    private ServiceController<GPSLogger> controller;
    private GPSLogger service;
    private DataHelper mockDataHelper;

    @Before
    public void setUp() throws Exception {
        controller = Robolectric.buildService(GPSLogger.class).create();
        service = controller.get();

        // Replace the DataHelper created in onCreate() with a Mockito mock
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

    /** Build a Location with an initialised extras Bundle. */
    private Location makeLocation() {
        Location loc = new Location("gps");
        loc.setLatitude(48.0);
        loc.setLongitude(2.0);
        loc.setTime(System.currentTimeMillis());
        loc.setExtras(new Bundle());
        return loc;
    }

    /**
     * Invoke the private {@link BroadcastReceiver} field directly so tests do
     * not depend on Android's broadcast-delivery machinery.
     */
    private void sendToReceiver(Intent intent) throws Exception {
        Field receiverField = GPSLogger.class.getDeclaredField("receiver");
        receiverField.setAccessible(true);
        BroadcastReceiver receiver = (BroadcastReceiver) receiverField.get(service);
        receiver.onReceive(service, intent);
    }

    /** Write a {@code long} field on the service via reflection. */
    private void setLongField(String fieldName, long value) throws Exception {
        Field f = GPSLogger.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    /** Write a {@code boolean} field on the service via reflection. */
    private void setBooleanField(String fieldName, boolean value) throws Exception {
        Field f = GPSLogger.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(service, value);
    }

    /** Read a field from the service via reflection. */
    private Object getField(String fieldName) throws Exception {
        Field f = GPSLogger.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(service);
    }

    // ── Group I: Service lifecycle ────────────────────────────────────────────

    /** After onCreate(), isTracking() must be false. */
    @Test
    public void service_onCreate_startsWithTrackingFalse() {
        assertFalse(service.isTracking());
    }

    /** After onCreate(), isGpsEnabled() must be false. */
    @Test
    public void service_onCreate_startsWithGpsEnabledFalse() {
        assertFalse(service.isGpsEnabled());
    }

    /** onStartCommand() must complete without throwing. */
    @Test
    public void service_onStartCommand_doesNotThrow() {
        controller.startCommand(0, 1);
        // reaching this line means no exception was thrown
    }

    /**
     * onUnbind() returns false (no re-bind) — the service should not request re-binding
     * because it is a one-shot service.
     */
    @Test
    public void service_onUnbind_returnsFalseWhenNotTracking() {
        boolean result = service.onUnbind(new Intent());
        assertFalse("onUnbind() should return false to prevent onRebind()", result);
    }

    // ── currentSegmentId initial state ────────────────────────────────────────

    /** Before any tracking starts, {@code currentSegmentId} must be -1. */
    @Test
    public void currentSegmentId_isNegativeOneBeforeTracking() throws Exception {
        long segId = (long) getField("currentSegmentId");
        assertEquals(-1L, segId);
    }

    /**
     * {@code currentSegmentId} must be declared {@code volatile} so that its value is
     * always visible across threads (GPS thread reads, main thread writes).
     *
     * <p><b>Currently fails</b> — Bug B6: the field is a plain {@code long} without
     * the {@code volatile} modifier. On a multi-core device the GPS thread may observe
     * a stale value written by the main thread.
     * Remove {@literal @}Ignore and delete
     * {@code GPSLoggerTestBugs#bug_B6_currentSegmentId_isNotVolatile}
     * once the bug is fixed.
     */
    @Ignore("Bug B6 — currentSegmentId is not volatile; race condition between threads. See docs/BUGS.md")
    @Test
    public void currentSegmentId_isVolatile() throws Exception {
        Field f = GPSLogger.class.getDeclaredField("currentSegmentId");
        // java.lang.reflect.Modifier.VOLATILE == 0x40 (64)
        boolean isVolatile = (f.getModifiers() & 0x40) != 0;
        assertTrue("currentSegmentId should be declared volatile for thread safety", isVolatile);
    }

    // ── deleteWaypoint receiver (Bug B7 happy path) ───────────────────────────

    /**
     * Happy path — with a non-null, non-{@code "null"} link, {@code deleteWayPoint()} is
     * called with a file path that includes the link filename.
     */
    @Test
    public void deleteWaypoint_withValidLink_callsDeleteWayPointWithFilePath() throws Exception {
        String uuid = "test-uuid-valid-link";
        String link = "photo.jpg";
        Intent intent = new Intent(OSMTracker.INTENT_DELETE_WP);
        intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, 1L);
        intent.putExtra(OSMTracker.INTENT_KEY_UUID, uuid);
        intent.putExtra(OSMTracker.INTENT_KEY_LINK, link);

        sendToReceiver(intent);

        verify(mockDataHelper).deleteWayPoint(eq(uuid), contains(link));
    }

    // ── onLocationChanged() behaviour ─────────────────────────────────────────

    /**
     * When not tracking, onLocationChanged() must not call DataHelper.track().
     */
    @Test
    public void onLocationChanged_whenNotTracking_doesNotCallDataHelper() {
        assertFalse(service.isTracking()); // pre-condition

        service.onLocationChanged(makeLocation());

        verify(mockDataHelper, never()).track(
                anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong());
    }

    /**
     * When tracking, onLocationChanged() must call DataHelper.track() with the
     * current track ID and segment ID.
     */
    @Test
    public void onLocationChanged_whenTracking_callsDataHelperTrack() throws Exception {
        setLongField("currentTrackId", 42L);
        setLongField("currentSegmentId", 1L);
        setLongField("lastGPSTimestamp", 0L);
        setLongField("gpsLoggingInterval", 0L);
        setBooleanField("isTracking", true);

        service.onLocationChanged(makeLocation());

        verify(mockDataHelper).track(
                eq(42L), any(Location.class), anyFloat(), anyInt(), anyFloat(), eq(1L));
    }

    /**
     * When two fixes arrive within the configured logging interval, only the first
     * triggers DataHelper.track() — the second is dropped.
     */
    @Test
    public void onLocationChanged_respectsLoggingInterval() throws Exception {
        setLongField("currentTrackId", 42L);
        setLongField("currentSegmentId", 1L);
        // Simulate a fix that was just logged
        setLongField("lastGPSTimestamp", System.currentTimeMillis());
        setLongField("gpsLoggingInterval", 60_000L); // 60-second interval
        setBooleanField("isTracking", true);

        service.onLocationChanged(makeLocation()); // arrives too soon
        service.onLocationChanged(makeLocation()); // also too soon

        verify(mockDataHelper, never()).track(
                anyLong(), any(Location.class), anyFloat(), anyInt(), anyFloat(), anyLong());
    }

    /**
     * onProviderDisabled() must set isGpsEnabled to false.
     */
    @Test
    public void onProviderDisabled_setsGpsEnabledFalse() {
        service.onProviderEnabled("gps");
        assertTrue(service.isGpsEnabled()); // pre-condition

        service.onProviderDisabled("gps");

        assertFalse(service.isGpsEnabled());
    }

    /**
     * onProviderEnabled() must set isGpsEnabled to true.
     */
    @Test
    public void onProviderEnabled_setsGpsEnabledTrue() {
        assertFalse(service.isGpsEnabled()); // starts as false

        service.onProviderEnabled("gps");

        assertTrue(service.isGpsEnabled());
    }
}
