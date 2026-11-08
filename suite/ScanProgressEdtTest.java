/*
 * @test
 * @comment Regression target: BUG-02; report: reports/BUG-02-progress-off-edt.md
 * @summary Scan progress value and completion text are updated on the Swing EDT
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 ScanProgressEdtTest
 */
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Requires a graphical desktop. Listeners observe callbacks without mutating Swing. */
public class ScanProgressEdtTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(1, gallery -> {
            Queue<String> violations = new ConcurrentLinkedQueue<>();
            CountDownLatch done = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger observations = new java.util.concurrent.atomic.AtomicInteger();
            GalleryTestSupport.edt(() -> {
                JProgressBar progress = GalleryTestSupport.component(gallery, "gallery.progress", JProgressBar.class);
                Runnable observe = () -> {
                    observations.incrementAndGet();
                    if (!SwingUtilities.isEventDispatchThread()) {
                        String text = "Swing progress mutation callback on " + Thread.currentThread().getName();
                        violations.add(text);
                        System.out.println(text);
                        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) System.out.println("    at " + frame);
                    }
                };
                progress.addChangeListener(e -> observe.run());
                progress.addPropertyChangeListener("string", e -> {
                    observe.run();
                    if ("Done".equals(e.getNewValue())) done.countDown();
                });
                gallery.rescan();
                return null;
            });
            GalleryTestSupport.check(done.await(15, TimeUnit.SECONDS), "Scan did not report Done within 15 seconds");
            GalleryTestSupport.check(observations.get() > 0, "No progress changes observed; test is inconclusive");
            GalleryTestSupport.check(violations.isEmpty(), "Progress must be updated on EDT; observed " + violations);
        });
    }
}
