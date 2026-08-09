package net.osmtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Bug-confirming tests for {@link DatabaseHelper}.
 *
 * <p>Every test in this class documents a <strong>known bug</strong> by asserting the current
 * broken behaviour. Each test passes <em>because</em> the bug exists — the tests are expected
 * to <strong>fail</strong> once the corresponding bug is fixed.
 *
 * <p>When a bug is fixed:
 * <ol>
 *   <li>Remove the {@literal @}Ignore annotation from the matching test in
 *       {@link DatabaseHelperTest} (the intended-behaviour companion).</li>
 *   <li>Delete (or permanently skip) the test in this class — it no longer represents
 *       correct expected behaviour.</li>
 *   <li>Run {@code ./gradlew testDebugUnitTest} — the formerly-{@literal @}Ignored test in
 *       {@link DatabaseHelperTest} must now pass.</li>
 * </ol>
 *
 * <p>See {@code docs/BUGS_DatabaseHelper.md} for the full description of each bug.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class DatabaseHelperTestBugs {

    private DatabaseHelper dbHelper;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Returns true if the given column has notnull=1 in PRAGMA table_info.
     */
    private boolean isColumnNotNull(SQLiteDatabase database, String table, String column) {
        Cursor c = database.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            int nameIdx = c.getColumnIndex("name");
            int notNullIdx = c.getColumnIndex("notnull");
            while (c.moveToNext()) {
                if (column.equals(c.getString(nameIdx))) {
                    return c.getInt(notNullIdx) == 1;
                }
            }
        } finally {
            c.close();
        }
        return false;
    }

    // ── Bug B10: segment_id missing NOT NULL after upgrade from v18 ──────────

    /**
     * Bug B10 — After upgrading from v18 to v19, {@code segment_id} is nullable.
     *
     * <p>The {@code ALTER TABLE} in {@code onUpgrade()} case 18 uses
     * {@code "integer default 0"} without {@code "not null"}, so after an upgrade
     * the column permits NULL values. In contrast, a fresh install via
     * {@code onCreate()} defines the column as {@code "integer not null default 0"}.
     *
     * <p>This test passes because the bug exists (notnull is 0 after upgrade).
     * When Bug B10 is fixed, this test will fail and should be deleted.
     * Remove {@literal @}Ignore from
     * {@code DatabaseHelperTest#onUpgrade_from18to19_segmentId_shouldBeNotNull}.
     *
     * @see DatabaseHelperTest#onUpgrade_from18to19_segmentId_shouldBeNotNull
     */
    @Test
    public void bug_B10_segmentId_missingNotNull_afterUpgradeFrom18() {
        SQLiteDatabase rawDb = SQLiteDatabase.create(null);
        try {
            // Simulate v18 trackpoint schema (no segment_id)
            rawDb.execSQL("create table trackpoint ("
                    + "_id integer primary key autoincrement,"
                    + "track_id integer not null,"
                    + "latitude double not null,"
                    + "longitude double not null,"
                    + "speed double null,"
                    + "elevation double null,"
                    + "accuracy double null,"
                    + "point_timestamp long not null,"
                    + "compass_heading double null,"
                    + "compass_accuracy integer null,"
                    + "atmospheric_pressure double null)");

            dbHelper.onUpgrade(rawDb, 18, 19);

            assertFalse("Bug B10: segment_id should be NOT NULL but is nullable after upgrade",
                    isColumnNotNull(rawDb, TrackContentProvider.Schema.TBL_TRACKPOINT,
                            TrackContentProvider.Schema.COL_SEG_ID));
        } finally {
            rawDb.close();
        }
    }

    /**
     * Bug B10 — NULL can be inserted into segment_id after an upgrade from v18.
     *
     * <p>Because the ALTER TABLE omits NOT NULL, a NULL value can be successfully
     * inserted into segment_id on an upgraded database. This would never happen
     * on a fresh install where NOT NULL is enforced.
     */
    @Test
    public void bug_B10_segmentId_allowsNullInsert_afterUpgrade() {
        SQLiteDatabase rawDb = SQLiteDatabase.create(null);
        try {
            rawDb.execSQL("create table trackpoint ("
                    + "_id integer primary key autoincrement,"
                    + "track_id integer not null,"
                    + "latitude double not null,"
                    + "longitude double not null,"
                    + "speed double null,"
                    + "elevation double null,"
                    + "accuracy double null,"
                    + "point_timestamp long not null,"
                    + "compass_heading double null,"
                    + "compass_accuracy integer null,"
                    + "atmospheric_pressure double null)");

            dbHelper.onUpgrade(rawDb, 18, 19);

            // Insert with explicit NULL for segment_id
            ContentValues values = new ContentValues();
            values.put(TrackContentProvider.Schema.COL_TRACK_ID, 1);
            values.put(TrackContentProvider.Schema.COL_LATITUDE, 48.0);
            values.put(TrackContentProvider.Schema.COL_LONGITUDE, 2.0);
            values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
            values.putNull(TrackContentProvider.Schema.COL_SEG_ID);

            long rowId = rawDb.insert(TrackContentProvider.Schema.TBL_TRACKPOINT, null, values);
            assertTrue("Bug B10: NULL insert into segment_id should succeed on upgraded DB",
                    rowId > 0);

            // Verify the value is actually NULL
            Cursor c = rawDb.query(TrackContentProvider.Schema.TBL_TRACKPOINT,
                    new String[]{TrackContentProvider.Schema.COL_SEG_ID},
                    "_id = ?", new String[]{String.valueOf(rowId)},
                    null, null, null);
            try {
                assertTrue(c.moveToFirst());
                assertTrue("Bug B10: segment_id should be NULL", c.isNull(0));
            } finally {
                c.close();
            }
        } finally {
            rawDb.close();
        }
    }

    /**
     * Bug B10 baseline — on a fresh install, segment_id enforces NOT NULL
     * (or at least defaults to 0 when NULL is attempted).
     *
     * <p>This establishes the correct baseline against which the upgrade path diverges.
     * On fresh installs, SQLite's NOT NULL with DEFAULT 0 causes a NULL insert to
     * use the default value of 0 instead.
     */
    @Test
    public void bug_B10_segmentId_freshInstall_defaultsToZeroWhenNullInserted() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_TRACK_ID, 1);
        values.put(TrackContentProvider.Schema.COL_LATITUDE, 48.0);
        values.put(TrackContentProvider.Schema.COL_LONGITUDE, 2.0);
        values.put(TrackContentProvider.Schema.COL_TIMESTAMP, System.currentTimeMillis());
        // Don't set segment_id — should default to 0

        long rowId = db.insert(TrackContentProvider.Schema.TBL_TRACKPOINT, null, values);
        assertTrue("insert should succeed on fresh DB", rowId > 0);

        Cursor c = db.query(TrackContentProvider.Schema.TBL_TRACKPOINT,
                new String[]{TrackContentProvider.Schema.COL_SEG_ID},
                "_id = ?", new String[]{String.valueOf(rowId)},
                null, null, null);
        try {
            assertTrue(c.moveToFirst());
            assertEquals("segment_id should default to 0 on fresh install",
                    0, c.getInt(0));
        } finally {
            c.close();
        }
    }
}
