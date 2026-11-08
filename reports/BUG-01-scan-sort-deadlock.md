# BUG-01: Sorting during scanning can deadlock the UI

**Severity: Critical.** The application can stop processing input, painting and
status updates. Scanning and interaction are required to remain responsive.

## Reproduction

[ScanSortDeadlockTest](../suite/ScanSortDeadlockTest.java):

```sh
./scripts/run-tests.sh suite/ScanSortDeadlockTest.java
```

The test starts a scan in a child JVM, uses a bounded scheduling gate to wait
until the loader is publishing through invokeAndWait, then triggers Sort on EDT
before returning to the event queue. Gate waiting time is excluded from the
2-second Sort-completion assertion. The parent captures diagnostics and cleans
up the child even if EDT is deadlocked.

Manual reproduction: launch Gallery, then alternate **Sort** and **Rescan** while
thumbnails load. The submitter reproduced a persistent freeze this way. This is
timing-sensitive; the attempt count and reproduction rate were not recorded.

## Expected and actual behavior

Expected: Sort completes, the loader continues, and the UI remains responsive.
Actual: Sort does not complete within 2 seconds. Thread metadata confirms an
EDT-to-loader monitor dependency cycle; the regression fails at its assertion.

## Evidence

- [Complete test result](../evidence/automated/tests/ScanSortDeadlockTest.jtr).
- [External JDK thread dump](../evidence/automated/diagnostics/ScanSortDeadlockTest/thread-dump.txt).
- [Manual native samples and analysis](../evidence/manual-2026-11-07-pid-89448/ANALYSIS.md).

The test records `Confirmed EDT -> loader monitor -> EDT cycle=true`. The
independent `jcmd Thread.print -l` dump shows EDT blocked in Gallery.toggleSort:266,
waiting for ArrayList monitor `0x00000003afc82668`. gallery-loader holds that same
monitor in Gallery.scan:377 while waiting in EventQueue.invokeAndWait.

The manual samples, captured 38.185 seconds apart, show the same EDT blocked in
monitor entry and gallery-loader waiting on a monitor. They corroborate the
sustained freeze but do not resolve the Java monitor's identity or prove the
precise manual wait cycle. They are separate from the controlled JDK dump.

## Suspected root cause

The loader holds the catalog monitor while synchronously requesting EDT work.
Sort on EDT needs that monitor before EDT can process the loader's queued task.
Neither side can progress. Other EDT catalog readers may share the risk.

## Environment and limitations

macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0; see
[execution environment](../NOTES.md). The preserved automated execution confirms
one controlled reproduction; no statistical or cross-platform rate is claimed.
The manual freeze used ordinary input without a test gate. No application fix.
