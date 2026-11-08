# Requirements and coverage

Reference: the supplied JBR QA Home Assignment PDF, Expected behaviour, pages 1-2.
[Recorded execution](TEST_RESULTS.md): 15 tests, 9 PASS, 6 FAIL, 0 harness errors.
Automated checks cover the cases stated below, not every platform or timing.
The submitter completed all nine [manual scenarios](MANUAL_SCENARIOS.md):
6 PASS and 3 FAIL. These are human-reported results; linked artifacts provide
additional evidence for the documented failures.

| Requirement | Automated coverage | Manual coverage / limitations |
| --- | --- | --- |
| Centered window and correct title | WindowPlacementTest; SelectionInteractionTest; RescanStateTest; DesktopEnvironmentTest | Different monitor/window-manager policies: M-01, M-06 |
| Toolbar, left grid, right preview, bottom status/progress | DesktopEnvironmentTest checks named components; RescanStateTest renders the right preview pane | Full visual arrangement: M-01 |
| Startup scan, background incremental loading, Done, responsiveness | StatusDuringScanTest; ScanProgressEdtTest; ScanSortDeadlockTest | Actual main-entry startup and paint/input experience: M-01; human interaction while loading: M-08 |
| Rescan cancellation, grid/preview/cache reset, new scan | RescanStateTest; StatusCacheTest; SortAfterRescanTest | Rapid cancellation, duplicate/stale publications and old-generation progress: M-08 |
| Mouse/arrow selection, preview preparation and scaling | SelectionInteractionTest uses real mouse/Left-arrow input; PreviewResponsivenessTest measures uncached EDT delay; RescanStateTest observes nonempty/empty preview rendering | Other arrow directions, full preview fit across pane sizes and interaction during preparation: M-02 |
| Name/size toggle, name tie-breaking, new-item ordering, persistence, selected item visible | SelectionInteractionTest; SortingPreservedDuringScanTest; SortAfterRescanTest | Selection visibility across long scrollable catalogs: M-03 |
| Info entry, details, position, dismissal | TooltipInfoDetailsTest uses actual Info button and popup click; InfoPopupPositionTest checks moved-frame placement | Double-click, platform shortcut, deactivation, scrolling and all screen-edge clamps: M-04, M-05 |
| Hover tooltip matches Info | TooltipInfoDetailsTest uses actual Robot hover before/after sort, checks all detail fields and blank-area handler | Further cells/scrolling/display paths: M-07 |
| Status count, megapixels, cached previews, scanning marker, continued refresh | StatusCacheTest checks final totals/cache transitions and cache reset; StatusDuringScanTest checks live updates and completion | Exact timer scheduling is not proven from text-change events; status under sustained interaction: M-08 |
| Closing stops work and exits | LifecycleTest checks current loader stops after dispose | Actual main process exit through the system close control: M-09 |

## Confirmed-bug report audit

Each report contains the requested symptoms, reproduction steps, evidence,
suspected root cause and severity. Reports link to regression source and preserved
results where automated coverage exists. BUG-06 links to M-01 and diagnostic
component captures. Historical evidence is not relabeled after test changes.

| Report | Symptoms | Reproduction | Evidence | Suspected cause | Severity |
| --- | --- | --- | --- | --- | --- |
| [BUG-01](BUG-01-scan-sort-deadlock.md) | Frozen EDT | Controlled test and user manual sequence | Java/jcmd dumps and manual native samples | Catalog monitor held across invokeAndWait | Critical |
| [BUG-02](BUG-02-progress-off-edt.md) | Progress callbacks on loader | ScanProgressEdtTest | Callback thread names and setter stacks | Worker mutates Swing progress directly | Major |
| [BUG-03](BUG-03-info-popup-position.md) | Detached popup | Moved-frame test | Expected/actual screen coordinates | Missing local-to-screen conversion | Minor |
| [BUG-04](BUG-04-window-centering.md) | Displaced initial frame | WindowPlacementTest | Usable bounds and final frame position | Centering before pack | Minor |
| [BUG-05](BUG-05-new-items-ignore-sort.md) | New/rescanned items in wrong order | Active-scan and Rescan tests | Model order, active-scan stage output | Publication ignores comparator | Major |
| [BUG-06](BUG-06-progress-text-readability.md) | Blue indicator crosses Done text | M-01 startup with 400 items | Component captures and environment log | Aqua indicator/string presentation interaction | Minor (usability) |

M-01 completion text is functionally correct. BUG-06 is a visual usability finding
on Aqua, not a failure of an explicit PDF contrast or styling threshold. M-01
is recorded as FAIL in the completed manual execution.

Automated PASS means the stated test passed its assertions; manual PASS records
the submitter's acceptance result. A latency test PASS does
not prove that every interaction is prompt, and recorded monitor metadata does
not prove multiple-monitor behavior. No full-coverage claim is made.
