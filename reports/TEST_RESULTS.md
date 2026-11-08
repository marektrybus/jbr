# Test results

The suite contains **15 tests**. Execution completed with **9 passed and 6 failed
tests, confirming five distinct application defects**. There were no harness
errors or pending tests. The two sorting regressions cover different scenarios
of BUG-05.

Separately, M-01 completion rendering with 400 artworks reproduced a visual
readability issue on macOS Aqua: [BUG-06](BUG-06-progress-text-readability.md).
Its [component captures](../evidence/manual-2026-11-08-startup/OBSERVATION.md)
are diagnostic evidence, not an additional jtreg result. The automated totals
above remain unchanged; the submission contains six bug reports in total.

Execution: 2026-11-08, 10:40:15.323-10:40:36.399 Europe/Warsaw. Environment:
macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0, graphical desktop with three
2x displays. See [NOTES.md](../NOTES.md) for exact paths, geometry and assumptions.

Command:

```sh
./scripts/run-tests.sh
```

The runner exited with code 2 because the regression tests exposed application
defects. [Summary](../evidence/automated/summary.txt),
[runner output](../evidence/automated/execution/runner.log), and
[result/source hashes](../evidence/automated/results.json) accompany the original
.jtr files. Gallery.java is unchanged; no source fix or threshold adjustment was
used to obtain these results.

| Test / evidence | Result | Finding |
| --- | --- | --- |
| [DesktopEnvironmentTest](../evidence/automated/tests/DesktopEnvironmentTest.jtr) | PASS | Desktop accessible and basic named components present |
| [SourceIntegrityTest](../evidence/automated/tests/SourceIntegrityTest.jtr) | PASS | Supplied Gallery bytes preserved |
| [LifecycleTest](../evidence/automated/tests/LifecycleTest.jtr) | PASS | Current loader stops after dispose |
| [PreviewResponsivenessTest](../evidence/automated/tests/PreviewResponsivenessTest.jtr) | PASS | 731.218 ms EDT delay; 1000 ms threshold |
| [RescanStateTest](../evidence/automated/tests/RescanStateTest.jtr) | PASS | Grid, selection, title, progress, preview and cache reset |
| [SelectionInteractionTest](../evidence/automated/tests/SelectionInteractionTest.jtr) | PASS | Mouse/Left-arrow input; sort ties, selection, title and visibility |
| [StatusCacheTest](../evidence/automated/tests/StatusCacheTest.jtr) | PASS | Correct counts/Mpx; cache 0 -> 1 -> 2 -> 2 -> 0 |
| [StatusDuringScanTest](../evidence/automated/tests/StatusDuringScanTest.jtr) | PASS | Six live status changes; maximum gap 252 ms; progress Done |
| [TooltipInfoDetailsTest](../evidence/automated/tests/TooltipInfoDetailsTest.jtr) | PASS | Actual hover agrees with Info before/after Sort; popup click dismisses it |
| [ScanSortDeadlockTest](../evidence/automated/tests/ScanSortDeadlockTest.jtr) | FAIL | [BUG-01](BUG-01-scan-sort-deadlock.md): confirmed EDT/loader wait cycle |
| [ScanProgressEdtTest](../evidence/automated/tests/ScanProgressEdtTest.jtr) | FAIL | [BUG-02](BUG-02-progress-off-edt.md): progress mutations on gallery-loader |
| [InfoPopupPositionTest](../evidence/automated/tests/InfoPopupPositionTest.jtr) | FAIL | [BUG-03](BUG-03-info-popup-position.md): expected (64,288), actual (0,130) |
| [WindowPlacementTest](../evidence/automated/tests/WindowPlacementTest.jtr) | FAIL | [BUG-04](BUG-04-window-centering.md): expected (459,223), actual (919,416) |
| [SortAfterRescanTest](../evidence/automated/tests/SortAfterRescanTest.jtr) | FAIL | [BUG-05](BUG-05-new-items-ignore-sort.md): size order lost after Rescan |
| [SortingPreservedDuringScanTest](../evidence/automated/tests/SortingPreservedDuringScanTest.jtr) | FAIL | [BUG-05](BUG-05-new-items-ignore-sort.md): Sort at 2/3 loaded, wrong order after third item |

## Measurements and interpretation

The active-scan sorting test records `Clicking Sort during scan: loaded=2/3` and
`Checking final size/name order after 1 additional artwork(s)`. Setup and initial
prefix-order checks pass; the final model-order assertion fails. The result is
not a timeout, completed-scan setup or the separate catalog/EDT deadlock.

The deadlock test confirms the wait cycle with internal ownership metadata and
an [external jcmd dump](../evidence/automated/diagnostics/ScanSortDeadlockTest/thread-dump.txt). Its child process
is cleaned up without asking a blocked EDT to dispose the window.

Preview baseline: median 0.024 ms, p95 0.038 ms, maximum 0.169 ms. The 2560-pixel
art-003 selection delays the sentinel by 731.218 ms, within the declared 1000 ms
limit. Sampled stacks show synchronous rendering on EDT; this observation is not
reported as a failing regression.

The 400-artwork status workload completes in 1725 ms. Six scanning-status changes
have a maximum gap of 252 ms. Final status is 400 artworks, 1072.2 Mpx, zero cached
previews, and progress 400/Done. StatusCacheTest separately checks cache counts
while selecting two distinct artworks, revisiting one and rescanning.

Actual mouse/Left-arrow selection is verified with title updates and both sort
orders. TooltipInfoDetailsTest uses real hover and verifies name, dimensions,
seed and catalog position against Info. The selected artwork moves from position
3/3 to 1/3 after Sort, and both displays reflect the change. RescanStateTest checks
the rendered preview against its empty-state image, together with model/title/
progress/cache reset.

## Coverage boundary

The [requirements matrix](REQUIREMENTS_COVERAGE.md) identifies partial coverage.
The submitter completed all nine [manual scenarios](MANUAL_SCENARIOS.md), with
6 PASS and 3 FAIL. They cover startup, all arrow keys, resizing,
scrolling, extra Info entry paths, screen edges, deactivation, monitor transitions,
rapid cancellation generations and actual process exit. Manual results are
human-reported; they do not imply additional automated checks or unrecorded
measurements. The submitter's manual freeze has separate
[native samples](../evidence/manual-2026-11-07-pid-89448/ANALYSIS.md).

This is one preserved suite execution. No statistical reproduction rate,
exhaustive coverage or cross-platform validation is claimed.
