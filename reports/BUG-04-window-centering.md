# BUG-04: Main frame is not centered in the usable screen area

**Severity: Minor.** The initial frame opens far from the center of its monitor's
usable area, contrary to the initial-window placement requirement.

## Reproduction

[WindowPlacementTest](../suite/WindowPlacementTest.java):

```sh
./scripts/run-tests.sh suite/WindowPlacementTest.java
```

Construct and show Gallery, drain pending AWT events with Robot.waitForIdle, then
compare the final frame position with the center calculated from its actual size
and its GraphicsConfiguration's usable bounds.

## Expected and actual behavior

Expected top-left: **(459,223)**. Actual: **(919,416)**.
Displacement: (+460,+193), far beyond the 4-unit tolerance.

## Evidence

[Complete test result](../evidence/automated/tests/WindowPlacementTest.jtr):

```text
frame=(919,416,1001,586)
usable=(0,30,1920,972)
expectedTopLeft=(459,223)
```

The frame fits within the observed usable rectangle but is not centered there.
The discrepancy is much larger than a decoration or rounding difference. The
test fails its centering assertion.

## Suspected root cause

buildUi calls setLocationRelativeTo(null) before pack establishes the final
window dimensions. Centering is based on a size different from the final frame.
Window-manager placement policies are an additional environment consideration.

## Environment and limitations

macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0; three 2x displays.
[Desktop metadata](../evidence/automated/tests/DesktopEnvironmentTest.jtr) records bounds
and insets. A single-monitor confirmation and other window managers are not
included; portability is not inferred from this configuration. No application fix.
