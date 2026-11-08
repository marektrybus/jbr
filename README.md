# Gallery QA investigation

JDK-only jtreg tests for the supplied Swing Gallery application. The application
source in `suite/lib/Gallery.java` is unchanged.

The suite contains **15 tests**. Execution completed with **9 PASS, 6 FAIL and
0 harness errors**, confirming five distinct application defects. See
[test results](reports/TEST_RESULTS.md), [requirements coverage](reports/REQUIREMENTS_COVERAGE.md),
[manual results](reports/MANUAL_SCENARIOS.md), and [execution notes](NOTES.md).
The submitter also completed all nine manual scenarios: **6 PASS and 3 FAIL**.
The submission contains six bug reports, including the manual progress-text
readability finding.

## Requirements

- JDK 17 or newer, including `javac` and `jcmd`.
- jtreg on `PATH`, or its executable supplied through `JTREG`.
- A logged-in graphical desktop for tests marked `headful`.
- Run GUI tests sequentially and avoid interacting with their windows.

No JUnit, TestNG, third-party libraries, or build system are required by this suite.

## Run

Open a terminal in the repository root with access to the graphical desktop.
Set the paths to your installed JDK and jtreg (replace the example paths):

```sh
export JAVA_HOME="/path/to/jdk"
export JTREG="/path/to/jtreg/bin/jtreg"
./scripts/check-local.sh
./scripts/run-tests.sh
```

On macOS, an installed JDK 21 can also be selected with
`export JAVA_HOME="$(/usr/libexec/java_home -v 21)"`. If jtreg is already on
`PATH`, setting `JTREG` is optional. The scripts contain no machine-specific paths.

Run one test:

```sh
./scripts/run-tests.sh suite/InfoPopupPositionTest.java
```

Run only tests that do not need a display:

```sh
./scripts/run-tests.sh '-k:!headful'
```

The jtreg harness must be able to read platform information, and GUI tests require
a desktop session. Robot-based tests also require permission for synthetic input.

jtreg writes detailed results to `work/jtreg/` and HTML to `work/jtreport/`.
Application failures should produce `FAILED` test results with actionable messages;
a compiler error, unavailable display, or unrelated deadlock is not confirmation
of a different reported bug. Exit code zero is not expected for a suite that
successfully exposes bugs in the supplied application.

## Tests

| Test | Related bug report | Expected behavior under test | Current execution status |
| --- | --- | --- | --- |
| [SourceIntegrityTest](suite/SourceIntegrityTest.java) | No confirmed bug report | Supplied application remains byte-for-byte unchanged | PASS (jtreg and direct Java) |
| [DesktopEnvironmentTest](suite/DesktopEnvironmentTest.java) | No confirmed bug report | Desktop metadata and named UI components are available | PASS |
| [WindowPlacementTest](suite/WindowPlacementTest.java) | [BUG-04](reports/BUG-04-window-centering.md) | Main frame is centered in the usable screen area | FAIL (centering) |
| [InfoPopupPositionTest](suite/InfoPopupPositionTest.java) | [BUG-03](reports/BUG-03-info-popup-position.md) | Info popup meets the cell's screen-space bottom-left corner | FAIL (popup offset) |
| [ScanProgressEdtTest](suite/ScanProgressEdtTest.java) | [BUG-02](reports/BUG-02-progress-off-edt.md) | Scan-driven progress callbacks run on the EDT | FAIL (loader-thread mutations) |
| [ScanSortDeadlockTest](suite/ScanSortDeadlockTest.java) | [BUG-01](reports/BUG-01-scan-sort-deadlock.md) | Sort completes while the loader publishes an artwork | FAIL (confirmed wait cycle) |
| [SortingPreservedDuringScanTest](suite/SortingPreservedDuringScanTest.java) | [BUG-05](reports/BUG-05-new-items-ignore-sort.md) | Choose size sorting after initial items arrive; subsequent items keep that order | FAIL (wrong order after Sort at 2/3 loaded) |
| [SortAfterRescanTest](suite/SortAfterRescanTest.java) | [BUG-05](reports/BUG-05-new-items-ignore-sort.md) | Selected size ordering survives Rescan | FAIL (wrong order) |
| [PreviewResponsivenessTest](suite/PreviewResponsivenessTest.java) | No confirmed bug report | Uncached preview preparation does not stall the EDT | PASS (731.218 ms; 1000 ms limit) |
| [LifecycleTest](suite/LifecycleTest.java) | No confirmed bug report | Dispose stops the current loader within a grace period | PASS (tested close path) |
| [SelectionInteractionTest](suite/SelectionInteractionTest.java) | No confirmed bug report | Mouse/Left-arrow selection, title, both sort orders, ties and selected-cell visibility | PASS |
| [TooltipInfoDetailsTest](suite/TooltipInfoDetailsTest.java) | No confirmed bug report | Real hover matches Info before/after sort; popup click and blank-space tooltip behavior | PASS |
| [StatusCacheTest](suite/StatusCacheTest.java) | No confirmed bug report | Loaded count, megapixels, cache transitions and cache reset | PASS |
| [StatusDuringScanTest](suite/StatusDuringScanTest.java) | No confirmed bug report | Scanning marker and continuing status refreshes; final progress Done | PASS (max observed gap 252 ms) |
| [RescanStateTest](suite/RescanStateTest.java) | No confirmed bug report | Grid, selection, title, progress, rendered preview and cache reset | PASS |

The related-bug column identifies intended regression coverage. Each associated
test also names its report in a jtreg `@comment` tag, and each report links back
to the current test source. These are project-local BUG identifiers; the jtreg
`@bug` tag is reserved here for actual Java bug-system numbers. The `@comment`
tag is defined in the [jtreg tag specification](https://github.com/openjdk/jtreg/blob/master/src/share/doc/javatest/regtest/tag-spec.html).

A coverage link identifies the intended regression target. The current statuses
come from the [preserved execution](evidence/automated/summary.txt).
Tests without a confirmed bug report provide environment, integrity, lifecycle,
or responsiveness coverage.

The deadlock test uses a child process and a bounded scheduling gate. If the EDT
stalls, it gathers internal thread stacks and attempts `jcmd Thread.print -l`,
then terminates the child. Other tests use bounded EDT calls and a daemon watchdog
as a final safeguard. Test timeouts do not establish a particular root cause.

## Repository contents

- `TEST.ROOT`: jtreg suite root and allowed keywords.
- `suite/`: tests and shared JDK-only helpers.
- `scripts/`: portable local compilation and jtreg launch commands.
- `reports/`: one test-results report, six bug reports, requirements matrix and manual procedures.
- `evidence/automated/`: execution summary and result/source manifest.
- `evidence/automated/tests/`: complete per-test .jtr results.
- `evidence/automated/diagnostics/`: additional evidence grouped by test name.
- `evidence/automated/execution/`: launcher output, jtreg version and run metadata.
- `evidence/manual-2026-11-07-pid-89448/`: separate manual-freeze samples and analysis.
- `NOTES.md`: commands, environment, thresholds, limitations and AI use.

The original PDF is archived outside this repository. Only the required unchanged Java source is copied into the test
library. Build outputs, temporary screenshots, native crash logs, recordings, and platform-specific files are ignored
by Git.
