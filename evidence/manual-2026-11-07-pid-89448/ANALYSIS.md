# Live manual-freeze capture: PID 89448

Captured on 2026-11-07 while the user's Gallery instance remained unresponsive
after alternating Sort and Rescan. The process was left running.

## Provenance

The process was located through macOS Activity Monitor, where its display name
was `Main`, PID 89448, parent zsh PID 67257. Its executable path was:

```text
/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java
```

Native sample headers identify Java 21.0.13, ARM64, macOS 26.7.2 (25G332), launch
time 16:15:36.805 +0100, and `/usr/bin/sample` as the analysis tool. The named
`gallery-loader` thread identifies the Gallery workload in this JVM. The complete
Java launch command and artwork count were not captured.

Two snapshots were obtained through Activity Monitor's Sample Process and Refresh
controls, then saved through its Save dialog into this directory:

| File | Capture start (Europe/Warsaw) | Observations |
| --- | --- | --- |
| native-sample-01.txt | 16:25:31.139 +0100 | 2182 |
| native-sample-02.txt | 16:26:09.324 +0100 | 802 |

The capture starts are 38.185 seconds apart. These are separate sampling windows,
not continuous monitoring of the intervening period. `manifest.json` records
SHA-256 hashes of both raw files. The `*-key-threads.txt` files are derived
excerpts, with the originals preserved byte-for-byte.

## Observations

In sample 1, lines 652-662 show native thread 1152937, named Java:
AWT-EventQueue-0. All 2182 observations follow:

```text
SharedRuntime::complete_monitor_locking_C
SharedRuntime::monitor_enter_helper
ObjectSynchronizer::enter
ObjectMonitor::enter
ObjectMonitor::EnterI
PlatformEvent::park_nanos
_pthread_cond_wait
```

Lines 790-799 show native thread 1164697, named Java: gallery-loader. All 2182
observations follow:

```text
JVM_MonitorWait
ObjectSynchronizer::wait
ObjectMonitor::wait
PlatformEvent::park
_pthread_cond_wait
```

Sample 2 shows the same thread IDs and the same paths in all 802 observations:
EDT at lines 531-541, loader at lines 669-678.

The EDT is stalled while acquiring a Java object monitor. The loader is waiting
on a Java monitor. The same pattern persists in both snapshots, supporting the
reported sustained UI freeze and distinguishing it from active preview rendering.

## Interpretation and limits

This pattern is consistent with BUG-01's controlled reproduction. However, native
sampling does not resolve the relevant Java frames (shown as unknown binaries)
or expose the object-monitor identity and owner. It therefore does not prove that
the EDT is blocked specifically in Sort versus Rescan/status, or independently
establish that both threads form the same catalog/invokeAndWait dependency cycle.
The [automated jcmd dump](../automated/diagnostics/ScanSortDeadlockTest/thread-dump.txt) provides the
object-identity evidence for that cycle in the controlled regression test. The manual samples are corroborating runtime evidence, not a replacement
for that Java-level dump.

No application source was changed, no scheduling gate was injected into the live
process, and the process was not killed or restarted. No screenshot or measured
manual reproduction rate is claimed.
