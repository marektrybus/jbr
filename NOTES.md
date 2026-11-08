# Submission notes

## Scope and result

The submission contains 15 JDK-only jtreg tests, six bug reports, a requirements
coverage matrix, completed manual acceptance scenarios, and preserved runtime evidence.
The recorded execution completed with **9 PASS, 6 FAIL and 0 harness errors**.
The six failing tests confirm five distinct defects; both sorting regressions
cover BUG-05. See [TEST_RESULTS.md](reports/TEST_RESULTS.md).
The submitter completed nine manual scenarios: 6 PASS and 3 FAIL, including
the separate progress-text readability finding, BUG-06.

Gallery.java is byte-for-byte identical to the supplied file:

```text
SHA-256: 06c13fe21b278bcf8192b35f45041aef6698ed1d4fa64a0ab9a6c869cff1f3bb
```

No application fixes, patches, third-party test frameworks or libraries are included.

## Execution environment

- Execution: 2026-11-08, 10:40:15.323-10:40:36.399 Europe/Warsaw (UTC+01:00).
- OS: macOS 26.7.2, build 25G332; ARM64/aarch64; Apple M5 Pro.
- Runtime JDK: Homebrew OpenJDK 21.0.13.
- JDK home: `/opt/homebrew/Cellar/openjdk@21/21.0.13/libexec/openjdk.jdk/Contents/Home`.
- jtreg: 8.4-dev+0; JT Harness 6.0 ea b24.
- jtreg executable used for the recorded run: `$HOME/tools/jtreg/build/images/jtreg/bin/jtreg`.
- Logged-in macOS graphical desktop; graphical tests marked `headful`.
- Three displays, all with 2x default transforms. Bounds/usable rectangles:
  - Display 1: (-1728,-22,1728,1117) / (-1728,11,1728,1084).
  - Display 3: (0,0,1920,1080) / (0,30,1920,971).
  - Display 5: (1920,0,1920,1080) / (1920,30,1920,1050).
- Exact per-test geometry is retained in the .jtr files. Insets can change between
  processes; each position assertion uses the bounds observed in its own process.
- Dock/menu settings and window-manager policies were not independently inspected.
  No Windows, Linux, different-window-manager or single-monitor result is claimed.

## Commands and artifacts

From the repository root, with JAVA_HOME and JTREG configured for your installed
tools as described in [README.md](README.md):

```sh
./scripts/check-local.sh
./scripts/run-tests.sh
./scripts/run-tests.sh suite/SortingPreservedDuringScanTest.java
```

The full-suite command produced the submitted results. The final command illustrates
how to reproduce an individual regression. The launcher sets JAVA_HOME on macOS
when absent and accepts JTREG or a jtreg executable on PATH. Commands need a local
desktop environment that permits JVM platform queries and Swing windows.

check-local.sh compiles all sources with `javac --release 17 -Xlint:all` and runs
the source-integrity check. Compilation succeeds; seven warnings belong to the
unchanged supplied application, with none in the authored tests at this baseline.
Compilation against Java 17 APIs on JDK 21 is not execution on a JDK 17 VM.

[Automated evidence](evidence/automated/) contains a root summary and manifest
with file paths and result/test-source/helper hashes. `tests/` holds all 15
original .jtr results; `diagnostics/ScanSortDeadlockTest/` holds the extracted JDK
thread dump; `execution/` holds launcher output, jtreg version and run metadata. Working classes and HTML reports
are generated under ignored `work/`. Test failures are expected for the reported
application defects; runner exit code 2 reflects those failures.

## Synchronization, thresholds and cleanup

- Preview responsiveness: max(1000 ms, 10 * baseline EDT p95) allows for machine
  speed and scheduling variation while detecting multi-second stalls. Baseline
  p95 was 0.038 ms; selection delay was 731.218 ms, within the 1000 ms limit.
  The threshold was not adjusted after observing the result.
- Status refresh: a 1000 ms maximum observed gap allows four nominal 250 ms timer
  periods for scheduling variation. The measured maximum was 252 ms. Text-change
  events demonstrate continuing updates, not the timer's exact schedule.
- Safety timeouts: EDT requests are bounded at 15 s, the cleanup watchdog at 70 s,
  and jtreg actions at 120 s. These prevent hung tests; they are not acceptable UI
  response times. Scenario-specific bounds are documented in the test code.
- Geometry: the frame/popup tolerances of 4/2 logical AWT units are test assumptions
  for native placement and rounding, not PDF requirements or cross-platform guarantees.

Swing access runs on EDT. Tests synchronize on events or bounded conditions and
propagate callback failures to the test thread. Listeners are removed and windows
closed in finally blocks. The deadlock regression uses a separate child JVM so
cleanup does not depend on blocked EDT; gate waiting is excluded from its 2 s
Sort check, and thread ownership/stacks establish the cycle rather than timeout
alone. GUI tests run sequentially to avoid competing for focus and input.

## Coverage and limitations

[REQUIREMENTS_COVERAGE.md](reports/REQUIREMENTS_COVERAGE.md) maps every Expected
behaviour bullet to automated checks and remaining cases. The suite includes real
mouse/Left-arrow selection, title and sort preservation, comparator ties, real
hover/Info agreement, cache/status totals, live status refresh, Rescan state reset,
positioning, responsiveness, lifecycle and concurrency regressions.

[MANUAL_SCENARIOS.md](reports/MANUAL_SCENARIOS.md) covers main-entry startup,
other arrow keys, preview resizing, long-catalog scrolling, extra Info entry paths,
deactivation, screen-edge/monitor transitions, rapid cancellation generations and
actual application-process exit. All nine scenarios were executed by the submitter
and their human-reported results are recorded; linked artifacts provide additional
evidence for failures. No exhaustive or cross-platform coverage is claimed. Repeated-reproduction
rates, every-item intermediate ordering and all cancellation races are unmeasured.

Manually reproduced a freeze by alternating Sort and Rescan. Two
[native samples](evidence/manual-2026-11-07-pid-89448/ANALYSIS.md), taken 38.185 s
apart, show persistent EDT monitor-entry blockage and loader waiting. They do not
identify the Java monitor or prove the exact manual wait cycle; the controlled
regression's JDK dump independently demonstrates its catalog/invokeAndWait cycle.

## AI use and human review

Codex helped read the assignment/source, propose hypotheses, draft JDK-only tests
and helpers, collect/interpret runtime evidence, and prepare reports. It was useful
for concurrency setup, thread-dump analysis, source-integrity checks and tracing
requirements to tests. Source-review hypotheses were verified against runtime
results before being reported as confirmed defects.

The submitter reviewed the code interactively and challenged test assumptions.
Some AI-generated tests (e.g. sorting setup selected Sort on a Gallery constructed without
starting its scan) have been changed to not test unrealistic/artifical scenarios.

The submitter independently checked the suite against the requirements and
identified coverage gaps, including selection, hover tooltips and status/cache
counts. They specified which areas needed additional tests or manual scenarios.
This review led to the corresponding automated checks, their execution, and the
requirements coverage matrix and manual procedures included in the submission.

The submitter also questioned the artificial deadlock scheduling gate and obtained
a natural manual freeze. The report distinguishes controlled Java-level evidence
from the manual native samples. The popup's arbitrary two-unit tolerance was
identified as an assumption, documented rather than presented as a requirement.
Environment failures were not treated as application failures, and the passing
preview measurement was not turned into a failure by adjusting its threshold.

AI output was reviewed and corrected rather than accepted as proof. The submitter
remains responsible for understanding, verifying and defending the submission.
No corrected application was used to validate the tests.
