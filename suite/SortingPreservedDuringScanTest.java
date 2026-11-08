/*
 * @test
 * @comment Regression target: BUG-05; report: reports/BUG-05-new-items-ignore-sort.md
 * @summary Size sorting selected during an active scan applies to subsequent artworks
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport SortTestSupport
 * @run main/othervm/timeout=120 SortingPreservedDuringScanTest
 */
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Requires a graphical desktop. No scan scheduling gates or fixed delays are used.
 * A scan/EDT deadlock is a separate failure, not proof of wrong ordering.
 */
public class SortingPreservedDuringScanTest {
    public static void main(String[] args) throws Exception {
        int count = SortTestSupport.fixtureCount();
        int sortAfter = count - 1;
        GalleryTestSupport.check(sortAfter >= 2,
                "Fixture needs at least two initial artworks and a smaller subsequent artwork");
        System.out.println("Fixture count=" + count + "; request Sort after " + sortAfter + " artworks");

        GalleryTestSupport.run(count, gallery -> {
            FutureTask<Integer> sortAction = new FutureTask<>(() -> {
                int loaded = GalleryTestSupport.list(gallery).getModel().getSize();
                GalleryTestSupport.check(loaded >= sortAfter && loaded < count && gallery.isScanning(),
                        "SETUP: Sort must run during an incomplete scan; loaded=" + loaded
                                + "/" + count + "; scanning=" + gallery.isScanning()
                                + ". No during-scan ordering result can be claimed.");
                System.out.println("Clicking Sort during scan: loaded=" + loaded + "/" + count);
                SortTestSupport.toggle(gallery);
                SortTestSupport.assertSizeOrder(gallery);
                return loaded;
            });

            // This flag and the listener are accessed only on the EDT.
            boolean[] sortQueued = {false};
            ListDataListener listener = new ListDataListener() {
                @Override
                public void intervalAdded(ListDataEvent event) {
                    int loaded = GalleryTestSupport.list(gallery).getModel().getSize();
                    if (!sortQueued[0] && loaded >= sortAfter && loaded < count) {
                        sortQueued[0] = true;
                        // Do not invoke Sort inside the model's publication callback:
                        // let that EDT event finish before processing the button action.
                        SwingUtilities.invokeLater(sortAction);
                    }
                }
                @Override public void intervalRemoved(ListDataEvent event) { }
                @Override public void contentsChanged(ListDataEvent event) { }
            };

            GalleryTestSupport.edt(() -> {
                ((DefaultListModel<?>) GalleryTestSupport.list(gallery).getModel())
                        .addListDataListener(listener);
                return null;
            });
            try {
                try {
                    GalleryTestSupport.scan(gallery, count);
                } catch (Exception | AssertionError failure) {
                    System.err.println("Final order assertion not reached: scan/Sort did not complete. "
                            + "A timeout or deadlock is not evidence of incorrect ordering; inspect thread dumps.");
                    throw failure;
                }

                int loadedAtSort;
                try {
                    loadedAtSort = sortAction.get(GalleryTestSupport.TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    if (cause instanceof Exception exception) throw exception;
                    if (cause instanceof Error error) throw error;
                    throw failure;
                } catch (TimeoutException failure) {
                    throw new AssertionError("SETUP: no completed Sort action during the scan; "
                            + "no during-scan ordering result can be claimed", failure);
                }

                GalleryTestSupport.edt(() -> {
                    int loaded = GalleryTestSupport.list(gallery).getModel().getSize();
                    GalleryTestSupport.check(loaded == count && loaded > loadedAtSort,
                            "Expected more artworks after Sort; at Sort=" + loadedAtSort + "; final=" + loaded);
                    System.out.println("Checking final size/name order after " + (loaded - loadedAtSort)
                            + " additional artwork(s)");
                    SortTestSupport.assertSizeOrder(gallery);
                    return null;
                });
            } finally {
                GalleryTestSupport.edt(() -> {
                    ((DefaultListModel<?>) GalleryTestSupport.list(gallery).getModel())
                            .removeListDataListener(listener);
                    return null;
                });
            }
        });
    }
}
