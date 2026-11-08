/*
 * @test
 * @summary Status keeps updating counts and scanning marker during a normal background scan; progress ends Done
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport GalleryUiTestSupport
 * @run main/othervm/timeout=120 StatusDuringScanTest
 */
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Requires a graphical desktop. Uses the application's default 400 artworks, without test gates. */
public class StatusDuringScanTest {
    private record Snapshot(long time, int count, String text) { }
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(400, gallery -> {
            ArrayList<Snapshot> snapshots = new ArrayList<>(); // Accessed only on EDT.
            Pattern pattern = Pattern.compile("(\\d+) artworks, .* Mpx, (\\d+) previews cached, scanning\\.\\.\\.");
            java.beans.PropertyChangeListener listener = event -> {
                String text = String.valueOf(event.getNewValue());
                Matcher matcher = pattern.matcher(text);
                if (matcher.matches()) {
                    Snapshot snapshot = new Snapshot(System.nanoTime(), Integer.parseInt(matcher.group(1)), text);
                    snapshots.add(snapshot);
                    System.out.println("Status during scan: " + text);
                }
            };
            GalleryTestSupport.edt(() -> {
                GalleryTestSupport.component(gallery, "gallery.status", JLabel.class).addPropertyChangeListener("text", listener);
                return null;
            });
            long started = System.nanoTime();
            try {
                try { GalleryTestSupport.scan(gallery, 400); }
                catch (Exception | AssertionError failure) {
                    System.err.println("Status completion checks not reached; investigate scan/EDT blockage separately.");
                    throw failure;
                }
                long elapsed = System.nanoTime() - started;
                GalleryUiTestSupport.awaitStatus(gallery, 0);
                GalleryTestSupport.edt(() -> {
                    JProgressBar progress = GalleryTestSupport.component(gallery, "gallery.progress", JProgressBar.class);
                    GalleryTestSupport.check(progress.getValue() == 400 && "Done".equals(progress.getString()),
                            "Completed scan must have progress=400 and text Done; got " + progress.getValue() + "/" + progress.getString());
                    GalleryTestSupport.check(snapshots.size() >= 2,
                            "Need at least two scanning status updates; got " + snapshots.size()
                                    + "; scan duration=" + elapsed / 1_000_000 + " ms. A very short scan is inconclusive.");
                    long maxGap = 0;
                    for (int i = 1; i < snapshots.size(); i++) {
                        Snapshot previous = snapshots.get(i - 1), current = snapshots.get(i);
                        GalleryTestSupport.check(current.count() >= previous.count(), "Loaded count regressed in one scan");
                        maxGap = Math.max(maxGap, current.time() - previous.time());
                    }
                    System.out.println("Scan duration=" + elapsed / 1_000_000 + " ms; scanning updates="
                            + snapshots.size() + "; maximum observed status gap=" + maxGap / 1_000_000 + " ms");
                    GalleryTestSupport.check(maxGap <= TimeUnit.SECONDS.toNanos(1),
                            "Status stopped updating for " + maxGap / 1_000_000 + " ms; allowed=1000 ms");
                    return null;
                });
            } finally {
                GalleryTestSupport.edt(() -> {
                    GalleryTestSupport.component(gallery, "gallery.status", JLabel.class).removePropertyChangeListener("text", listener);
                    return null;
                });
            }
        });
    }
}
