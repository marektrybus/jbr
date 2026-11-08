# BUG-02: Scan changes Swing progress state from the loader thread

**Severity: Major.** Progress state is mutated outside EDT, violating Swing's
threading requirements even when no visual corruption is immediately apparent.

## Reproduction

[ScanProgressEdtTest](../suite/ScanProgressEdtTest.java):

```sh
./scripts/run-tests.sh suite/ScanProgressEdtTest.java
```

Create Gallery for one artwork, attach change/string-property listeners to the
named progress bar on EDT, then start Rescan. Record callback threads and stacks;
wait for Done before asserting the recorded violations.

## Expected and actual behavior

Expected: scan-driven progress mutations and their synchronous callbacks run on EDT.
Actual: two callbacks run on gallery-loader. The test fails the EDT-thread assertion.

## Evidence

[Complete test result](../evidence/automated/tests/ScanProgressEdtTest.jtr) contains:

```text
Swing progress mutation callback on gallery-loader
```

The value callback stack goes through JProgressBar.setValue -> Gallery.scan:384.
The string callback goes through JProgressBar.setString -> Gallery.scan:386.
The listeners observe the actual setter thread without mutating the component;
the main test thread asserts the recorded failures.

## Suspected root cause

scan() directly calls progress.setValue and progress.setString on its worker
instead of dispatching the Swing state updates to EDT.

## Environment and limitations

macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0; see
[execution environment](../NOTES.md). Evidence establishes the thread violation,
not a particular secondary corruption or visual artifact. No application fix.
