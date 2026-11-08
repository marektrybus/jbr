# BUG-03: Info popup is detached from the selected cell

**Severity: Minor.** Moving the main frame leaves Info at the wrong screen position.
The popup must meet the selected cell's bottom-left corner, shifted only enough
to fit within the usable screen area.

## Reproduction

[InfoPopupPositionTest](../suite/InfoPopupPositionTest.java):

```sh
./scripts/run-tests.sh suite/InfoPopupPositionTest.java
```

Load one artwork, move the frame to an inset position on its monitor, select the
artwork and open Info on EDT. Convert the cell anchor to screen coordinates and
apply the required screen-boundary clamp before comparing positions.

## Expected and actual behavior

Expected popup top-left: **(64,288)**. Actual: **(0,130)**.
Displacement: (-64,-158), exceeding the test's 2-unit AWT tolerance.

## Evidence

[Complete test result](../evidence/automated/tests/InfoPopupPositionTest.jtr):

```text
cell=(0,0,112,130)
listOnScreen=(64,158)
anchor=(64,288)
usable=(0,30,1920,972)
popup=(0,130,185,78)
```

The expected popup fits within the usable area, so clamping cannot explain the
displacement. The actual position matches the cell-local point without the
list's screen origin. The test fails its position assertion.

## Suspected root cause

showInfo uses a point from getCellBounds as if it were screen-relative, without
first converting it from list coordinates to screen coordinates.

## Environment and limitations

macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0; three 2x displays.
Coordinates are logical AWT units. Scrolling, all screen edges, monitor transitions
and extra Info entry paths have manual procedures in
[MANUAL_SCENARIOS.md](MANUAL_SCENARIOS.md). Toolbar/content/dismissal behavior has
separate coverage in TooltipInfoDetailsTest. No application fix.
