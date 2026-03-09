# OSMTracker Android — Bug Report Catalogue

This document describes **9 confirmed bugs** found in the `DataHelper` and `GPSLogger`
modules during a static-analysis + unit-test audit (branch `new-test-cases`).

For each bug you will find:
- A precise description and the affected code location.
- The severity and the risk if left unfixed.
- The unit test(s) that cover it and **a detailed explanation of why those tests pass
  today even though the bug is still present**.
- A ready-to-paste GitHub issue description.

---

## How can a test pass when a bug exists?

There are two complementary strategies used in this catalogue:

| Strategy | When used | What it means |
|----------|-----------|---------------|
| **Assert the broken behaviour** | Crash bugs (NPE, missing guard) | `assertThrows(NullPointerException.class, ...)` — the test *expects* the crash. It passes today because the crash *does* happen. When the bug is fixed the assertion must be removed. |
| **Assert the absence of the correct behaviour** | Resource/threading bugs | e.g. `verify(cursor, never()).close()` — the test asserts that `close()` is *never* called. It passes today because the bug means `close()` is indeed never called. When the bug is fixed, the `never()` must change to a plain `verify()`. |

This technique is sometimes called **characterisation testing**: tests lock in the current
(broken) behaviour so that any future change to the code immediately shows up as a test
result change, prompting the developer to either confirm the fix or update the test.

---

## Bug B1 — `wayPoint()`: NPE before null guard

### Location
`DataHelper.java`, lines 213–214 (inside `wayPoint()`):
```java
Log.d(TAG, "Tracking waypoint … nbSatellites=" + location.getExtras().getInt("satellites") …);
// ↑ This line runs BEFORE the guard on line 220:
if (location != null) { … }
```

### Description
The method accesses `location.getExtras().getInt("satellites")` in a log statement at the
very start of the method body, **before** any null check on `location` or on its extras
bundle. This means:

- Passing `location = null` (which the comment on line 218 acknowledges can happen) throws
  `NullPointerException` immediately.
- Passing a `Location` whose extras bundle was never initialised (common when constructing
  a `Location` programmatically) also throws `NullPointerException` because
  `getExtras()` returns `null`.

The `if (location != null)` guard on line 220 was intended to protect the insertion logic,
but the crash happens before it is ever reached.

### Severity: CRITICAL — app crash

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `E1_wayPoint_throwsNPEWhenLocationIsNull` | `assertThrows(NullPointerException.class, …)` | The NPE is thrown on line 214; `assertThrows` catches it and the test is green. |
| `E2_wayPoint_throwsNPEWhenLocationExtrasIsNull` | `assertThrows(NullPointerException.class, …)` | `getExtras()` returns `null`; `.getInt(…)` on null throws NPE; `assertThrows` catches it. |

When the bug is fixed (null-check the location and its extras before the log line), both
`assertThrows` calls will no longer see an exception and the tests will fail — that is the
intended signal to update the assertions.

### Fix hint
Move the `if (location != null)` guard to wrap the entire method body, including the log
statement, and add a separate guard for `location.getExtras() != null`.

---

### GitHub issue body

**Title:** `wayPoint()` crashes with NPE when `location` is null or has no extras bundle

```
## Bug description
`DataHelper.wayPoint()` calls `location.getExtras().getInt("satellites")` in a log
statement (line 214) **before** the `if (location != null)` guard on line 220.

Passing a null `location` — or a `Location` object whose extras bundle was never
initialised — throws `NullPointerException` immediately, crashing any operation that
tries to record a waypoint under those conditions.

## Reproduction
```java
dataHelper.wayPoint(trackId, null, "name", null, uuid, 0, 0, 0);
// → NullPointerException at DataHelper.java:214

Location loc = new Location("gps"); // no setExtras() call
dataHelper.wayPoint(trackId, loc, "name", null, uuid, 0, 0, 0);
// → NullPointerException at DataHelper.java:214
```

## Expected behaviour
The method should silently return (or log a warning) when `location` is null,
consistent with the developer comment already on line 218:
> "location should not be null, but sometime is."

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, lines 213–220

## Labels
`bug`, `crash`, `null-safety`
```

---

## Bug B2 — `trackNote()`: NPE, no null guard at all

### Location
`DataHelper.java`, lines 326–328 (inside `trackNote()`):
```java
Log.d(TAG, "Tracking note … nbSatellites=" + location.getExtras().getInt("satellites") …);
// No null check on location or its extras anywhere in this method
```

### Description
Unlike `wayPoint()`, which at least has a (misplaced) `if (location != null)` guard,
`trackNote()` has **no null protection whatsoever**. The log statement on line 327
dereferences `location.getExtras()` unconditionally. Any caller that passes a `Location`
object without a pre-initialised extras bundle will trigger an immediate NPE.

### Severity: CRITICAL — app crash

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `F1_trackNote_throwsNPEWhenLocationExtrasIsNull` | `assertThrows(NullPointerException.class, …)` | `getExtras()` returns `null`; chaining `.getInt(…)` throws NPE; `assertThrows` catches it. |

### Fix hint
Add a null check on `location` (and `location.getExtras()`) before the log statement,
mirroring the fix required for B1.

---

### GitHub issue body

**Title:** `trackNote()` crashes with NPE — no null guard on `location` or its extras

```
## Bug description
`DataHelper.trackNote()` calls `location.getExtras().getInt("satellites")` in its first
log statement (line 327) without any null check on `location` or on its extras bundle.
Unlike `wayPoint()`, there is no guard anywhere in this method.

## Reproduction
```java
Location loc = new Location("gps"); // no setExtras()
dataHelper.trackNote(trackId, loc, "note", uuid);
// → NullPointerException at DataHelper.java:327
```

## Expected behaviour
The method should record the note even when the location has no extras bundle. The
satellite count should be treated as 0 or omitted from the log.

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, lines 325–328

## Labels
`bug`, `crash`, `null-safety`
```

---

## Bug B3 — `getSegmentIdFor()`: Cursor never closed — resource leak

### Location
`DataHelper.java`, lines 419–429 (inside `getSegmentIdFor()`):
```java
public static long getSegmentIdFor(long trackId, ContentResolver cr) {
    Cursor ca = cr.query(…);      // cursor opened here

    if (!ca.moveToFirst()) {
        return 0;                 // ← cursor NOT closed before early return
    }

    return Track.build(trackId, ca, cr, true).getMaxSegId();
    // ← cursor NOT closed in the success path either
}
```

### Description
The `Cursor` returned by `ContentResolver.query()` is opened at the top of the method but
is **never closed** in either code path — neither the early-return path (track not found)
nor the success path. This method is called every time GPS tracking starts (and potentially
on every segment boundary), so each call leaks a database cursor. Over time this exhausts
the cursor window pool, causing `SQLiteCantOpenDatabaseException` or
`CursorWindowAllocationException` on long-running tracking sessions.

### Severity: HIGH — resource leak, potential crash after extended use

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `A2_getSegmentIdFor_doesNotCloseCursorInEarlyReturnPath_documentedBug` | `verify(mockCursor, never()).close()` | The mock cursor's `close()` is genuinely never called, so `never()` is satisfied. |

The assertion uses `never()` deliberately: it documents that `close()` is *absent*. When
the bug is fixed and `close()` is added, this `never()` assertion will fail — the
developer must then change it to `verify(mockCursor).close()`.

### Fix hint
Wrap the method body in a `try-finally` block, or use try-with-resources if targeting
API 16+ (which this project does).

---

### GitHub issue body

**Title:** `getSegmentIdFor()`: Cursor opened but never closed — resource leak on every track start

```
## Bug description
`DataHelper.getSegmentIdFor()` opens a `Cursor` via `ContentResolver.query()` but never
calls `cursor.close()` in either code path (early return when the track is not found, or
the normal return after calling `Track.build()`).

This method is invoked every time GPS tracking starts. Each invocation leaks one cursor
handle. On long recording sessions the cursor pool can be exhausted, causing
`CursorWindowAllocationException` or `SQLiteCantOpenDatabaseException`.

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, lines 419–429

## Suggested fix
```java
public static long getSegmentIdFor(long trackId, ContentResolver cr) {
    Cursor ca = cr.query(…);
    if (ca == null) return 0;
    try {
        if (!ca.moveToFirst()) return 0;
        return Track.build(trackId, ca, cr, true).getMaxSegId();
    } finally {
        ca.close();
    }
}
```

## Labels
`bug`, `database`, `resource-leak`
```

---

## Bug B4 — `getTrackById()`: No null or empty-cursor guard

### Location
`DataHelper.java`, lines 584–595 (inside `getTrackById()`):
```java
public Track getTrackById(long trackId) {
    Cursor c = context.getContentResolver().query(…);
    Log.d(TAG, "Count: " + c.getCount()); // NPE if c is null
    c.moveToFirst();                       // no check on return value
    Track track = Track.build(trackId, c, contentResolver, true); // reads invalid position
    c.close();
    return track;
}
```
Even the developer left a `//TODO: Fix this method` comment above it.

### Description
Two distinct failure scenarios:

1. **Null cursor**: `ContentResolver.query()` is permitted to return `null` (e.g., if the
   provider crashes or is unavailable). Calling `c.getCount()` on a null reference throws
   NPE immediately.
2. **Empty cursor**: When the given `trackId` does not exist in the database, the cursor
   is valid but empty. `moveToFirst()` returns `false` (the cursor stays at position −1),
   but its return value is never checked. The subsequent `Track.build()` call reads column
   values from an invalid cursor position, throwing
   `CursorIndexOutOfBoundsException`.

### Severity: HIGH — crash on invalid or missing track ID

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `B1_getTrackById_throwsNPEWhenCursorIsNull` | `assertThrows(NullPointerException.class, …)` | Mock ContentResolver returns null; `c.getCount()` throws NPE; `assertThrows` catches it. |
| `B2_getTrackById_throwsForNonExistentId` | `assertThrows(RuntimeException.class, …)` | Empty cursor; `Track.build()` reads position −1; `CursorIndexOutOfBoundsException` is thrown and caught. |

### Fix hint
Check `c != null` and `c.moveToFirst()` before calling `Track.build()`. Return `null` (or
throw a meaningful checked exception) on failure, and always close the cursor.

---

### GitHub issue body

**Title:** `getTrackById()` crashes on null cursor or non-existent track ID

```
## Bug description
`DataHelper.getTrackById()` has two crash paths (acknowledged by the TODO comment above
the method):

1. If `ContentResolver.query()` returns `null`, `c.getCount()` throws NPE.
2. If the track does not exist, `moveToFirst()` is called but its `false` return is
   ignored. `Track.build()` then reads from an invalid cursor position, throwing
   `CursorIndexOutOfBoundsException`.

## Reproduction
```java
// Case 1: provider returns null
dataHelper.getTrackById(1L); // with mocked null cursor → NPE

// Case 2: ID does not exist
dataHelper.getTrackById(99999L); // → CursorIndexOutOfBoundsException
```

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, lines 582–595

## Labels
`bug`, `crash`, `database`
```

---

## Bug B5 — `getWayPointById()` / `getTrackPointById()`: Same null/empty-cursor pattern

### Location
`DataHelper.java`:
- `getWayPointById()`, lines 618–630
- `getTrackPointById()`, lines 652–664

### Description
Both methods share the same structural flaw as B4:
- No null check on the cursor returned by `ContentResolver.query()`.
- No check on the return value of `moveToFirst()` before passing the cursor to the
  `WayPoint(Cursor)` / `TrackPoint(Cursor)` constructors.
- Neither method closes the cursor after use (a secondary resource leak).

When a non-existent ID is queried, the cursor is empty and the constructor reads from an
invalid position, crashing with `CursorIndexOutOfBoundsException`.

### Severity: HIGH — crash on invalid waypoint or trackpoint ID

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `C1_getWayPointById_throwsNPEWhenCursorIsNull` | `assertThrows(NullPointerException.class, …)` | Null cursor; `cWayPoint.getCount()` throws NPE. |
| `C2_getWayPointById_throwsForNonExistentId` | `assertThrows(RuntimeException.class, …)` | Empty cursor; constructor reads position −1; exception thrown. |
| `D1_getTrackPointById_throwsNPEWhenCursorIsNull` | `assertThrows(NullPointerException.class, …)` | Same as C1 for trackpoints. |
| `D2_getTrackPointById_throwsForNonExistentId` | `assertThrows(RuntimeException.class, …)` | Same as C2 for trackpoints. |

### Fix hint
Apply the same null-and-empty-cursor pattern as described for B4, and add `cursor.close()`
after constructing the model object (or use try-finally).

---

### GitHub issue body

**Title:** `getWayPointById()` and `getTrackPointById()` crash on null cursor or non-existent ID

```
## Bug description
Both `DataHelper.getWayPointById()` and `DataHelper.getTrackPointById()` share the same
structural flaws:

1. No null check on the cursor from `ContentResolver.query()` → NPE.
2. `moveToFirst()` return value is ignored → `CursorIndexOutOfBoundsException` on empty cursor.
3. The cursor is never closed after use → secondary resource leak.

## Reproduction
```java
dataHelper.getWayPointById(99999);   // → CursorIndexOutOfBoundsException
dataHelper.getTrackPointById(99999); // → CursorIndexOutOfBoundsException
```

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, lines 618–664

## Labels
`bug`, `crash`, `database`, `resource-leak`
```

---

## Bug B6 — `GPSLogger.currentSegmentId`: Race condition between threads

### Location
`GPSLogger.java`, line 89:
```java
private long currentSegmentId = -1;  // ← should be volatile
```
Written on the **main thread** (in `startTracking()`, line 326):
```java
currentSegmentId = DataHelper.getSegmentIdFor(trackId, getContentResolver()) + 1;
```
Read on the **GPS callback thread** (in `onLocationChanged()`, line 356):
```java
dataHelper.track(currentTrackId, location, …, currentSegmentId);
```

### Description
Java's memory model does not guarantee that a write made on one thread is immediately
visible to another thread unless the field is declared `volatile` or the access is
synchronised. Without `volatile`, the GPS thread may observe a **stale value** of
`currentSegmentId` (e.g., still −1) even after the main thread has updated it. This leads
to trackpoints being recorded with the wrong segment ID, silently corrupting the GPX
output on multi-core devices.

The bug is non-deterministic: on a single-core device or emulator it may never manifest,
which makes it particularly dangerous in production.

### Severity: MEDIUM — silent data corruption, non-deterministic

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `J1_currentSegmentId_isNegativeOneBeforeTracking` | `assertEquals(-1L, segId)` | Passes because the field is correctly initialised to −1. Not a bug test, but a baseline. |
| `J2_currentSegmentId_fieldIsNotVolatile_documentedRaceCondition` | `assertFalse(isVolatile)` | Uses `Field.getModifiers()` to check the `volatile` modifier bit. The field is NOT volatile, so `assertFalse` is satisfied — confirming the bug. |

`J2` asserts the *absence* of `volatile`. When the bug is fixed (field made `volatile`),
`isVolatile` will be `true` and `assertFalse` will fail — the developer must then change
it to `assertTrue`.

Note: Robolectric runs tests single-threaded, so the actual memory-visibility race cannot
be reproduced in a unit test. The test instead validates the structural precondition
(missing modifier) that makes the race possible.

### Fix hint
Declare the field as `private volatile long currentSegmentId = -1;`.

---

### GitHub issue body

**Title:** `GPSLogger.currentSegmentId` is not `volatile` — race condition between main thread and GPS thread

```
## Bug description
`GPSLogger.currentSegmentId` is written on the main thread (inside `startTracking()`) and
read on the GPS location callback thread (inside `onLocationChanged()`). The field is a
plain `long` without the `volatile` modifier, so the Java Memory Model provides no
visibility guarantee between the two threads.

On a multi-core device the GPS thread may read a stale value (e.g., still −1) even after
the main thread has updated it. Trackpoints would then be recorded with the wrong segment
ID, silently corrupting the exported GPX file.

## Root cause
```java
// GPSLogger.java line 89
private long currentSegmentId = -1;  // missing volatile
```

## Fix
```java
private volatile long currentSegmentId = -1;
```

## Affected file
`app/src/main/java/net/osmtracker/service/gps/GPSLogger.java`, line 89

## Labels
`bug`, `thread-safety`, `data-corruption`
```

---

## Bug B7 — `GPSLogger` BroadcastReceiver: Empty `catch(NullPointerException)` silently swallows error

### Location
`GPSLogger.java`, lines 160–163 (inside the `INTENT_DELETE_WP` branch of the
`BroadcastReceiver`):
```java
String filePath = null;
try {
    filePath = link.equals("null") ? null
             : DataHelper.getTrackDirectory(trackId, context) + "/" + link;
} catch(NullPointerException ne) {}   // ← empty catch
dataHelper.deleteWayPoint(uuid, filePath);
```

### Description
When the `INTENT_DELETE_WP` broadcast is received without a `link` extra,
`intent.getExtras().getString(OSMTracker.INTENT_KEY_LINK)` returns `null`. The code then
calls `link.equals("null")` which immediately throws `NullPointerException`. The empty
`catch` block silently swallows this exception, leaving `filePath` as `null`.

The consequence is two-fold:
1. `deleteWayPoint(uuid, null)` is still called, so the **database row is deleted** — the
   waypoint is gone from the UI.
2. Because `filePath` is `null`, **the attached media file is never deleted** from
   storage. The file becomes permanently orphaned, silently consuming disk space.

The correct idiom is `"null".equals(link)` (Yoda condition), which is null-safe.

### Severity: MEDIUM — silent failure, orphaned files, disk leak

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `K1_deleteWp_nullLink_swallowsNPEAndStillCallsDeleteWayPoint` | `verify(mockDataHelper).deleteWayPoint(eq(uuid), isNull())` | The NPE is swallowed, execution continues, and `deleteWayPoint` IS called with `null` as the file path. Mockito's `verify` confirms this. |

The test passes because it verifies the *observable consequence* of the bug (the waypoint
row is removed but no file path is passed). If the bug is fixed with the Yoda condition,
`filePath` will be correctly `null` for a null `link` (same result for this specific
case), but the empty catch block will be eliminated — a separate assertion (or
code-review check) would then confirm the fix.

### Fix hint
Replace `link.equals("null")` with `"null".equals(link)` and remove the `try-catch` entirely.

---

### GitHub issue body

**Title:** `GPSLogger` BroadcastReceiver silently swallows NPE on waypoint deletion — attached file never deleted

```
## Bug description
When `INTENT_DELETE_WP` is received without a `link` extra, `link` is `null`.
`link.equals("null")` throws `NullPointerException`, which is caught by an **empty**
`catch(NullPointerException)` block. Execution continues with `filePath = null`.

Result:
- The waypoint **database row is deleted** (correct).
- The **attached media file is never deleted** (incorrect) — it becomes permanently
  orphaned on the device's storage.

## Root cause
```java
// GPSLogger.java lines 160-163
try {
    filePath = link.equals("null") ? null : …; // NPE when link is null
} catch(NullPointerException ne) {}            // silently swallowed
```

## Fix
```java
// Null-safe Yoda condition — no try-catch needed
filePath = "null".equals(link) ? null
         : DataHelper.getTrackDirectory(trackId, context) + "/" + link;
```

## Affected file
`app/src/main/java/net/osmtracker/service/gps/GPSLogger.java`, lines 158–164

## Labels
`bug`, `silent-failure`, `file-management`
```

---

## Bug B8 — `DataHelper.FILENAME_FORMATTER`: `SimpleDateFormat` is not thread-safe

### Location
`DataHelper.java`, line 113:
```java
public static final SimpleDateFormat FILENAME_FORMATTER =
        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
```

### Description
`java.text.SimpleDateFormat` is explicitly documented as **not thread-safe**. It maintains
internal mutable state (calendar fields, number formatters) that is modified during each
`format()` or `parse()` call. When two threads call `FILENAME_FORMATTER.format(date)`
concurrently, they corrupt each other's internal state, producing garbled or swapped date
strings in file names.

In OSMTracker, file-naming is used for waypoint media files (photos, voice recordings).
Under concurrent waypoint recording (e.g., rapid tapping), two waypoints could end up
with identical or incorrect timestamps in their file names.

### Severity: LOW — data corruption under concurrent use, rare in practice

### Covering tests and why they pass
| Test | Assertion | Why it passes today |
|------|-----------|---------------------|
| `G1_filenameFormatter_isNotThreadSafe_documentedRaceCondition` | Logs errors, does not fail | The test submits 100 concurrent `format()` calls from 20 threads and collects any garbled output. It does not call `fail()` because the race is timing-dependent — it may not manifest in every run. Any error printed to stdout indicates Bug B8 has been reproduced. |

This test is intentionally non-failing; it serves as a **canary**. On a loaded multi-core
CI machine it occasionally produces output confirming the race.

### Fix hint
Use `ThreadLocal<SimpleDateFormat>`:
```java
public static final ThreadLocal<SimpleDateFormat> FILENAME_FORMATTER =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"));
// Usage: DataHelper.FILENAME_FORMATTER.get().format(date)
```
Or use the thread-safe `DateTimeFormatter` from `java.time` (API 26+).

---

### GitHub issue body

**Title:** `DataHelper.FILENAME_FORMATTER` (`SimpleDateFormat`) is not thread-safe — potential garbled file names

```
## Bug description
`DataHelper.FILENAME_FORMATTER` is a `public static final SimpleDateFormat` instance
shared across all threads. `SimpleDateFormat` is explicitly not thread-safe. Concurrent
calls to `format()` (e.g., during rapid waypoint recording) corrupt its internal state
and produce incorrect date strings in media file names.

## Root cause
```java
// DataHelper.java line 113
public static final SimpleDateFormat FILENAME_FORMATTER =
        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
```

## Fix
```java
public static final ThreadLocal<SimpleDateFormat> FILENAME_FORMATTER =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"));
```

## Affected file
`app/src/main/java/net/osmtracker/db/DataHelper.java`, line 113

## Labels
`bug`, `thread-safety`
```

---

## Bug B9 — `Track.setTrackId()`: Setter has an empty body — silently does nothing

### Location
`Track.java`, lines 134–135:
```java
public void setTrackId(long trackId) {
    // empty — the field `this.trackId` is never assigned
}
```

### Description
The setter `setTrackId()` is declared, accepts a parameter, but contains **no
implementation**. Any caller that tries to update the track ID of a `Track` object via
this setter will observe no effect: the internal `trackId` field will retain its previous
value. There is no exception, no log message — the data update is silently discarded.

This is especially insidious because `getTrackId()` exists and works correctly; the
asymmetry is not obvious from the API surface. If any part of the codebase (or a
future contributor) calls `setTrackId()` expecting to update the object, the resulting
silent mismatch can lead to incorrect GPX exports, wrong database queries, or corrupt
OSM uploads.

### Severity: LOW — silent incorrect behaviour; impact depends on call sites

> **Note:** No automated unit test was written for this bug in the current audit because
> `Track.build()` (the primary construction path) sets `trackId` directly via field
> assignment (`out.trackId = trackId`), bypassing the setter. The setter is currently
> uncalled in production code. The risk is forward-looking: any future refactor that
> routes through the setter will silently break.

### Fix hint
```java
public void setTrackId(long trackId) {
    this.trackId = trackId;  // add this line
}
```

---

### GitHub issue body

**Title:** `Track.setTrackId()` has an empty body — setter silently does nothing

```
## Bug description
`Track.setTrackId(long trackId)` is declared as a public setter but its body is empty:
the internal `trackId` field is never updated. Any caller of this method will silently
get no effect.

## Root cause
```java
// Track.java lines 134-135
public void setTrackId(long trackId) {
    // missing: this.trackId = trackId;
}
```

## Risk
The bug is currently latent because the primary construction path (`Track.build()`)
sets `trackId` directly. However, any future code or refactor that calls `setTrackId()`
will silently produce wrong track IDs in database queries, GPX exports, or OSM uploads
without any error or warning.

## Fix
```java
public void setTrackId(long trackId) {
    this.trackId = trackId;
}
```

## Affected file
`app/src/main/java/net/osmtracker/db/model/Track.java`, lines 134–135

## Labels
`bug`, `data-model`
```

---

## Quick reference

| Bug | Module | Severity | Crash? | Tests |
|-----|--------|----------|--------|-------|
| B1 — `wayPoint()` NPE before null guard | `DataHelper` | CRITICAL | Yes | E1, E2 |
| B2 — `trackNote()` NPE, no guard at all | `DataHelper` | CRITICAL | Yes | F1 |
| B3 — `getSegmentIdFor()` cursor never closed | `DataHelper` | HIGH | Eventually | A2 |
| B4 — `getTrackById()` no null/empty-cursor guard | `DataHelper` | HIGH | Yes | B1, B2 |
| B5 — `getWayPointById()` / `getTrackPointById()` same as B4 | `DataHelper` | HIGH | Yes | C1, C2, D1, D2 |
| B6 — `currentSegmentId` non-`volatile` race | `GPSLogger` | MEDIUM | No (data corruption) | J2 |
| B7 — Empty `catch(NPE)` swallows null-link error | `GPSLogger` | MEDIUM | No (silent failure) | K1 |
| B8 — `FILENAME_FORMATTER` not thread-safe | `DataHelper` | LOW | No (garbled names) | G1 |
| B9 — `Track.setTrackId()` empty body | `Track` | LOW | No (silent wrong data) | — |

### Recommended fix order
Fix the highest-severity bugs first to prevent user-visible crashes:

1. **B1, B2** — add null guards in `wayPoint()` and `trackNote()` (one-line fixes).
2. **B4, B5** — add null and empty-cursor guards in the three `getById()` methods.
3. **B3** — wrap `getSegmentIdFor()` cursor in try-finally.
4. **B9** — add `this.trackId = trackId` in `Track.setTrackId()`.
5. **B6** — add `volatile` to `currentSegmentId`.
6. **B7** — replace `link.equals("null")` with `"null".equals(link)`, remove try-catch.
7. **B8** — switch to `ThreadLocal<SimpleDateFormat>` or `DateTimeFormatter`.
