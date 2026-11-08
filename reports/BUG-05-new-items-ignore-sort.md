# BUG-05: Newly loaded artworks ignore the active size ordering

**Severity: Major.** Sorting existing items works, but subsequent items do not
follow the active comparator, including after Rescan. The requirement says the
selected order applies to newly loaded thumbnails and survives Rescan.

## Reproduction

- [SortingPreservedDuringScanTest](../suite/SortingPreservedDuringScanTest.java).
- [SortAfterRescanTest](../suite/SortAfterRescanTest.java).

```sh
./scripts/run-tests.sh suite/SortingPreservedDuringScanTest.java
./scripts/run-tests.sh suite/SortAfterRescanTest.java
```

Scenario A: start scanning three artworks, wait for the first two, then click
Sort while the loader is still active and the catalog incomplete. Verify the
current prefix, wait for the third item and check the final ordering. The button
action is queued as a separate EDT event; no scheduling gate pauses the loader.

Scenario B: load three artworks, select size sorting and verify the order, then
Rescan and check the completed catalog without another Sort click.

## Expected and actual behavior

Both scenarios expect:

```text
[art-002:512, art-000:1536, art-001:2048]
```

Both produce:

```text
[art-000:1536, art-001:2048, art-002:512]
```

The fixture contains a size inversion, so generation order cannot accidentally
satisfy the size-order assertion. No selected preview obscures the scenario.

## Evidence

- [Active-scan result](../evidence/automated/tests/SortingPreservedDuringScanTest.jtr).
- [Rescan result](../evidence/automated/tests/SortAfterRescanTest.jtr).

Scenario A records `Clicking Sort during scan: loaded=2/3`, followed by
`Checking final size/name order after 1 additional artwork(s)`. The active-scan
setup and prefix-order checks pass; the final order assertion fails. Scenario B
also passes its initial sort check and fails after Rescan. Neither failure is
inferred from a scan timeout or the separate catalog/EDT deadlock.

## Suspected root cause

toggleSort sorts the current catalog. scan appends new artworks without applying
the active comparator. This explains both scenarios as one defect.

## Environment and limitations

macOS 26.7.2/ARM64, OpenJDK 21.0.13, jtreg 8.4-dev+0. The preserved execution
contains one result per scenario; no repeated-reproduction rate is claimed.
SelectionInteractionTest separately covers comparator ties, name-order switching
and selection preservation in a small catalog. Per-item intermediate ordering
and long-catalog visibility remain outside these two regression tests. No fix.
