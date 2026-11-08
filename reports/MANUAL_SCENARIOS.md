# Manual acceptance scenarios

All nine scenarios were executed by the submitter: **6 PASS and 3 FAIL**.
The table records the submitter's manual results alongside the reproduction
procedures. Failed scenarios link to bug reports and available diagnostic
evidence. PASS results are human-reported; no per-scenario screenshots or
measurement logs are claimed where none are linked.

These checks supplement the automated suite on the available macOS desktop and
monitor configuration. They do not establish results on other platforms or display
setups. A failed scenario remains FAIL even if other checks within it pass.

Launch the actual entry point so startup scanning is included:

```sh
"$JAVA_HOME/bin/java" suite/lib/Gallery.java 40
```

Use 400 artworks when a longer scan or a scrollable catalog is needed. Restart
the application between independent scenarios. Do not classify a hang as a
particular bug without a thread dump or equivalent supporting evidence.

| ID | Procedure | Expected behavior | Status |
| --- | --- | --- | --- |
| M-01 Startup and layout | Launch Gallery normally. Observe frame position, toolbar, left thumbnail grid, right preview, progress and status. Watch the initial scan without pressing Rescan. | Window centered in the usable screen area; title Gallery; thumbnails appear one by one automatically; progress reaches Done; UI remains responsive. | FAIL: reproduced main-entry startup with 400 items. Completion value/text PASS; readability issue reproduced in component rendering, [BUG-06](BUG-06-progress-text-readability.md). See [evidence](../evidence/manual-2026-11-08-startup/OBSERVATION.md). Frame placement is covered by [BUG-04](BUG-04-window-centering.md). |
| M-02 Selection and preview fit | Click the smallest then largest artwork. Use each arrow direction in the grid. Resize the frame and drag the split divider to several aspect ratios. Record selection/title and input latency. | Selection responds promptly; title matches selected artwork; preview appears when ready, retains square proportions, fits the pane and is not clipped. | PASS |
| M-03 Sort with scrolling | Launch with 400 items and wait for Done. Select a cell near the bottom. Toggle size/name sort several times. Check equal-size name ties through Info. | The same artwork remains selected and visible, and the full catalog follows the selected comparator. | PASS |
| M-04 All Info entry paths | Select an artwork. Open Info with the toolbar, close it by clicking its content, repeat with double-click and Ctrl+I (Cmd+I on macOS) while the grid has focus. Switch to another application while Info is open. | Each entry opens the same details. Popup click and main-window deactivation dismiss it. | PASS |
| M-05 Popup after scrolling/moving | Launch a scrollable catalog, select a visible cell after scrolling, open Info, then repeat after moving the frame. Repeat near each usable screen edge. | Popup meets the cell's bottom-left screen corner, shifted only enough to stay in the usable area. | FAIL — [BUG-03: Info popup position](BUG-03-info-popup-position.md). |
| M-06 Multiple monitors and scale | Move Gallery across the available monitors, including a monitor with negative virtual coordinates. Repeat popup placement and preview resizing. Record each monitor's bounds, insets and scale. | Placement uses correct screen coordinates and usable bounds; preview stays fitted. | PASS |
| M-07 Tooltip interaction | Hover several thumbnails before/after sorting and scrolling. Move through gaps and blank list space. Compare name, dimensions, seed and catalog position with Info. | Correct details for the hovered cell; no details for blank space; Info and tooltip agree. | PASS |
| M-08 Rescan stress and status | While loading, alternate Sort and Rescan, then stop interacting. Also Rescan after selecting previews. Record counts, Mpx, cached-preview count, scanning marker, progress and title. | Previous scan canceled; no duplicate/stale entries or old Done/progress; preview/cache cleared; active sort retained; status keeps updating and settles to correct final totals. | FAIL — reproduced a freeze matching the manual reproduction in [BUG-01: scan/sort deadlock](BUG-01-scan-sort-deadlock.md); see [native samples and analysis](../evidence/manual-2026-11-07-pid-89448/ANALYSIS.md). |
| M-09 Normal window close | Launch through main, close with the window's system close control during scanning and during/after a preview selection. Inspect Java processes from another terminal. | Window closes, background work stops, and this application process exits. | PASS |
