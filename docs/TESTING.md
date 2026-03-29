# Testing Guide — OSMTracker Android

This document explains how to run the test suite, generate a code-coverage report, and
interpret the coverage results.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Test types at a glance](#2-test-types-at-a-glance)
3. [Running unit tests](#3-running-unit-tests)
4. [Running a single test class or method](#4-running-a-single-test-class-or-method)
5. [Running instrumented tests](#5-running-instrumented-tests)
6. [Generating the coverage report](#6-generating-the-coverage-report)
7. [Reading the coverage report](#7-reading-the-coverage-report)
8. [Understanding coverage percentages](#8-understanding-coverage-percentages)
9. [Current coverage baseline](#9-current-coverage-baseline)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Prerequisites

| Tool | Required version | Notes |
|------|-----------------|-------|
| **JDK** | 17 (recommended) | CI uses Eclipse Temurin 17. JDK 24 is **not** compatible with Gradle 8.11.1 (see [Troubleshooting](#10-troubleshooting)). |
| **Android SDK** | API 25–35 | Set `sdk.dir` in `local.properties`. |
| **Gradle wrapper** | 8.11.1 | Use `./gradlew` — do **not** install Gradle manually. |

> **No emulator or physical device is needed to run unit tests.**
> Instrumented tests (Section 5) do require one.

---

## 2. Test types at a glance

| Type | Framework | Location | Device needed? |
|------|-----------|----------|---------------|
| **Unit tests** | Robolectric + Mockito | `app/src/test/` | No |
| **Instrumented tests** | Espresso | `app/src/androidTest/` | Yes (API ≥ 26) |

Unit tests use [Robolectric](https://robolectric.org/) to simulate the Android framework
in the JVM, so they run fast and require no connected hardware.

---

## 3. Running unit tests

From the **project root directory**, run:

```bash
./gradlew testDebugUnitTest
```

Gradle compiles the sources, runs all unit tests, and prints a summary:

```
BUILD SUCCESSFUL in 1m 42s
```

If any test fails, Gradle prints the failing test name and the assertion error, then exits
with a non-zero code (`BUILD FAILED`).

### Where results are written

| Artifact | Path |
|----------|------|
| XML results (machine-readable) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` |
| HTML results (human-readable) | `app/build/reports/tests/testDebugUnitTest/index.html` |

Open the HTML report in a browser for a navigable view of all test outcomes:

```bash
xdg-open app/build/reports/tests/testDebugUnitTest/index.html   # Linux
open app/build/reports/tests/testDebugUnitTest/index.html       # macOS
```

---

## 4. Running a single test class or method

### Run one test class

```bash
./gradlew testDebugUnitTest --tests "net.osmtracker.db.DataHelperTest"
./gradlew testDebugUnitTest --tests "net.osmtracker.service.gps.GPSLoggerTest"
```

### Run one specific test method

```bash
./gradlew testDebugUnitTest \
  --tests "net.osmtracker.db.DataHelperTest.E1_wayPoint_throwsNPEWhenLocationIsNull"
```

### Run multiple classes at once

```bash
./gradlew testDebugUnitTest \
  --tests "net.osmtracker.db.DataHelperTest" \
  --tests "net.osmtracker.service.gps.GPSLoggerTest"
```

The `--tests` flag supports wildcards:

```bash
# All tests in the db package
./gradlew testDebugUnitTest --tests "net.osmtracker.db.*"
```

---

## 5. Running instrumented tests

Instrumented tests require a **running emulator** or **physical device** with API level
≥ 26 connected via ADB.

### Start an emulator (command line)

```bash
# List available AVDs
$ANDROID_HOME/emulator/emulator -list-avds

# Start one (replace NAME with an AVD name from the list above)
$ANDROID_HOME/emulator/emulator -avd NAME
```

Wait until the emulator is fully booted, then run:

```bash
./gradlew connectedCheck
```

CI runs connected tests against API 26 using the
[android-emulator-runner](https://github.com/ReactiveCircus/android-emulator-runner)
GitHub Action.

---

## 6. Generating the coverage report

The project uses **JaCoCo** for coverage. The task `jacocoTestReport` depends on
`testDebugUnitTest`, so running the report task also runs all unit tests:

```bash
./gradlew testDebugUnitTest jacocoTestReport
```

Or simply:

```bash
./gradlew jacocoTestReport   # runs tests first automatically
```

### Where the report is written

| Format | Path |
|--------|------|
| **XML** (for CI / Coveralls) | `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` |
| **HTML** (for local reading) | `app/build/reports/jacoco/jacocoTestReport/html/index.html` |

> The XML report is what the CI pipeline uploads to
> [Coveralls](https://coveralls.io/) for the badge on the README.

Open the HTML report:

```bash
xdg-open app/build/reports/jacoco/jacocoTestReport/html/index.html   # Linux
open app/build/reports/jacoco/jacocoTestReport/html/index.html       # macOS
```

---

## 7. Reading the coverage report

The HTML report is a navigable tree that mirrors the package structure.

```
index.html
└── net.osmtracker
    ├── db/
    │   ├── DataHelper          ← click to see line-by-line coverage
    │   ├── TrackContentProvider
    │   └── model/
    │       ├── Track
    │       ├── WayPoint
    │       └── TrackPoint
    ├── service/gps/
    │   └── GPSLogger
    └── gpx/
        └── ExportTrackTask
```

Each row shows four coverage metrics for the class:

| Column | What it measures |
|--------|-----------------|
| **Missed Instructions** | Individual JVM bytecode instructions not executed |
| **Cov.** (Coverage %) | Percentage of instructions that were executed |
| **Missed Branches** | `if`/`switch` branches not taken |
| **Branch Cov.** | Percentage of branches exercised |
| **Missed Lines** | Source lines with no covered instruction |
| **Missed Methods** | Methods never called during tests |
| **Missed Classes** | Classes with zero covered instructions |

### Line colours in source view

Click any class name to open the line-by-line annotated source:

| Colour | Meaning |
|--------|---------|
| **Green** | Line fully covered (all branches taken) |
| **Yellow** | Line partially covered (some branches missed) |
| **Red** | Line not covered at all |
| No colour | Not executable (comments, blank lines, declarations) |

---

## 8. Understanding coverage percentages

### What the percentage means

```
Instruction coverage = (executed instructions) / (total instructions) × 100
```

A 70 % instruction coverage means 30 % of the compiled bytecode was never reached by any
test. Higher is better, but **coverage alone does not measure test quality** — a test can
execute a line without asserting anything meaningful about its result.

### Meaningful thresholds (industry guidance)

| Coverage | Interpretation |
|----------|---------------|
| < 40 % | Low — critical paths likely untested |
| 40 – 60 % | Moderate — main flows covered, edge cases missing |
| 60 – 80 % | Good — most paths exercised |
| > 80 % | High — edge cases and error paths covered |
| 100 % | Theoretical maximum — rarely practical or necessary |

> Prioritise covering **error paths** (null inputs, missing records, provider failures)
> over trivially-exercised happy paths. The bugs catalogued in
> [`docs/BUGS.md`](BUGS.md) were all in code that had 0 % coverage before the
> `new-test-cases` branch.

### Extracting the percentage from the XML report

The XML report lists coverage per package, class, and method. To extract the overall
instruction coverage quickly:

```bash
# Overall instruction coverage from the XML
python3 - << 'EOF'
import xml.etree.ElementTree as ET
tree = ET.parse("app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
root = tree.getroot()
for counter in root.findall("counter"):
    if counter.attrib["type"] == "INSTRUCTION":
        missed  = int(counter.attrib["missed"])
        covered = int(counter.attrib["covered"])
        total   = missed + covered
        pct     = covered / total * 100
        print(f"Overall instruction coverage: {pct:.1f}%  ({covered}/{total})")
EOF
```

Or use `grep` for a quick look:

```bash
grep -o 'type="INSTRUCTION"[^/]*/>' \
  app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml | tail -1
```

---

## 9. Current coverage baseline

The table below shows the coverage of the two modules targeted by the `new-test-cases`
audit (branch `new-test-cases`). Numbers are approximate and will change as more tests
are added or bugs are fixed.

| Module / Class | Before audit | After `new-test-cases` | Notes |
|----------------|-------------|------------------------|-------|
| `DataHelper` | ~0 % | ~65 % | Groups A–H in `DataHelperTest` |
| `GPSLogger` | ~0 % | ~55 % | Groups I–L in `GPSLoggerTest` |
| Overall project | ~30 % | ~40 % | Existing tests + new tests |

> Coverage will improve further once the bugs documented in `docs/BUGS.md` are fixed,
> because several `assertThrows` tests will be rewritten as positive assertions that
> exercise the now-correct code paths.

### Test file map

| Test class | Covers | # Tests |
|------------|--------|---------|
| `DataHelperTest` | `DataHelper` (all public methods) | 23 |
| `GPSLoggerTest` | `GPSLogger` (lifecycle, receiver, location) | 13 |
| `DataHelperNoteTest` | `DataHelper.trackNote`, `deleteNote` | 1 |
| `ExportToStorageTaskTest` | `ExportToStorageTask` GPX output | 9 |
| `ExportToTempFileTaskTest` | `ExportToTempFileTask` | 1 |
| `TrackTest` | `Track.build()` | 1 |
| `OSMVisibilityTest` | `Track.OSMVisibility` | 4 |
| `MercatorProjectionTest` | `MercatorProjection` util | 10 |
| `FileSystemUtilsTest` | `FileSystemUtils` | 10 |
| `ArrayUtilsTest` | `ArrayUtils` | 4 |
| `URLCreatorTest` | `URLCreator` | 5 |
| `CustomLayoutsUtilsTest` | `CustomLayoutsUtils` | 6 |
| `ThemeValidatorTest` | `ThemeValidator` | 2 |
| `ButtonsPresetsTest` | `ButtonsPresets` activity | 3 |
| `TrackDetailEditorTest` | `TrackDetailEditor` activity | 2 |
| `OpenStreetMapNotesUploadTest` | `OpenStreetMapNotesUpload` | 2 |
| `URLValidatorTaskTest` | `URLValidatorTask` | 1 |
| `DownloadCustomLayoutTaskTest` | `DownloadCustomLayoutTask` | 1 |
| **Total** | | **98** |

---

## 10. Troubleshooting

### `[JAVA_COMPILER] not present` error

```
Toolchain installation '/usr/lib/jvm/java-21-openjdk' does not provide the required
capabilities: [JAVA_COMPILER]
```

**Cause:** The system has a JRE (runtime only) instead of a full JDK (which includes
`javac`). Gradle needs `javac` to compile the project.

**Fix (local machines):** Tell Gradle which JDK to use by adding one line to
`gradle.properties` in the project root (do **not** commit this file change):

```properties
# gradle.properties  ← add this line, never commit it
org.gradle.java.home=/path/to/your/jdk17
```

Common locations:

| Environment | Path |
|------------|------|
| Android Studio bundled JBR (Linux Flatpak) | `/var/lib/flatpak/app/com.google.AndroidStudio/.../files/extra/jbr` |
| SDKMAN JDK 17 | `~/.sdkman/candidates/java/17.0.x-tem` |
| Homebrew (macOS) | `/opt/homebrew/opt/openjdk@17` |
| CI (GitHub Actions) | Handled automatically by `actions/setup-java@v4` |

To find the exact path on your machine:

```bash
# Linux
find /var /usr /opt /home -name "javac" 2>/dev/null | grep -v "build/"

# macOS
/usr/libexec/java_home -v 17
```

### `Type T not present` error with JDK 24

```
Could not create an instance of type org.gradle.api.internal.tasks.testing.DefaultTestTaskReports.
  > Type T not present
```

**Cause:** JDK 24 is not compatible with Android Gradle Plugin 8.7.3 / Gradle 8.11.1.

**Fix:** Use JDK 17 or 21. See the table above.

### Tests compile but none run

```
BUILD SUCCESSFUL   (0 tests executed)
```

**Cause:** Gradle cached a previous run. Force re-execution:

```bash
./gradlew testDebugUnitTest --rerun-tasks
```

### Out of memory during test run

```bash
./gradlew testDebugUnitTest \
  -Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

Or add permanently to `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Coverage report is empty / 0 %

Ensure you run both tasks together:

```bash
./gradlew testDebugUnitTest jacocoTestReport
```

Running `jacocoTestReport` alone (without fresh test execution data) produces an empty
report because JaCoCo needs the `.exec` file generated during the test run.
