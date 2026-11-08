/*
 * @test
 * @summary Uncached preview preparation leaves the Swing event queue responsive
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 PreviewResponsivenessTest
 */
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/**
 * Requires a graphical desktop. Measures EDT sentinel delay during uncached selection.
 * Threshold: max(1000 ms, 10 times baseline p95). A fast-machine PASS is possible.
 */
public class PreviewResponsivenessTest {
    public static void main(String[] args) throws Exception {
        int count = largeFixtureCount();
        GalleryTestSupport.run(count, gallery -> {
            GalleryTestSupport.scan(gallery, count);
            long[] baseline = new long[100];
            for (int i = 0; i < baseline.length; i++) {
                long submitted = System.nanoTime();
                GalleryTestSupport.edt(() -> null);
                baseline[i] = System.nanoTime() - submitted;
            }
            Arrays.sort(baseline);
            long p95 = baseline[94];
            long threshold = Math.max(TimeUnit.SECONDS.toNanos(1), p95 * 10);
            System.out.printf(Locale.ROOT, "baseline median=%.3f ms p95=%.3f ms max=%.3f ms; threshold=%.3f ms%n",
                    baseline[49] / 1e6, p95 / 1e6, baseline[99] / 1e6, threshold / 1e6);
            AtomicReference<Thread> edtThread = new AtomicReference<>();
            GalleryTestSupport.edt(() -> { edtThread.set(Thread.currentThread()); return null; });
            CountDownLatch entered = new CountDownLatch(1);
            AtomicLong submittedAt = new AtomicLong();
            CompletableFuture<Long> delay = new CompletableFuture<>();
            AtomicBoolean selecting = new AtomicBoolean();
            ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();
            Queue<String> samples = new ConcurrentLinkedQueue<>();
            AtomicInteger sampleCount = new AtomicInteger();
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            sampler.scheduleAtFixedRate(() -> {
                if (!selecting.get() || sampleCount.incrementAndGet() > 40) return;
                ThreadInfo info = bean.getThreadInfo(edtThread.get().getId(), Integer.MAX_VALUE);
                if (info != null) samples.add(Arrays.toString(info.getStackTrace()));
            }, 0, 50, TimeUnit.MILLISECONDS);
            FutureTask<Void> selection = new FutureTask<>(() -> {
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                int largest = 0;
                for (int i = 1; i < list.getModel().getSize(); i++) {
                    if (list.getModel().getElementAt(i).size > list.getModel().getElementAt(largest).size) largest = i;
                }
                System.out.println("Selecting uncached " + list.getModel().getElementAt(largest).name
                        + ", size=" + list.getModel().getElementAt(largest).size);
                // Publish the sentinel before starting the selection. It can only execute
                // when this EDT action returns, so asynchronous preparation remains fast.
                submittedAt.set(System.nanoTime());
                SwingUtilities.invokeLater(() -> delay.complete(System.nanoTime() - submittedAt.get()));
                selecting.set(true);
                entered.countDown();
                try { list.setSelectedIndex(largest); }
                finally { selecting.set(false); }
                return null;
            });
            try {
                SwingUtilities.invokeLater(selection);
                GalleryTestSupport.check(entered.await(10, TimeUnit.SECONDS), "Selection action did not start");
                long elapsed = delay.get(45, TimeUnit.SECONDS);
                selection.get(1, TimeUnit.SECONDS); // Propagate exceptions thrown on the EDT.
                System.out.printf(Locale.ROOT, "EDT sentinel delay during selection=%.3f ms%n", elapsed / 1e6);
                for (String sample : samples) System.out.println("EDT selection stack: " + sample);
                GalleryTestSupport.check(elapsed <= threshold,
                        "Uncached preview blocked the EDT for " + elapsed / 1_000_000
                                + " ms; allowed=" + threshold / 1_000_000 + " ms; see render stacks above");
            } finally {
                sampler.shutdownNow();
                GalleryTestSupport.check(sampler.awaitTermination(3, TimeUnit.SECONDS), "Sampler did not stop");
            }
        });
    }
    private static int largeFixtureCount() {
        Random random = new Random(20240601L);
        for (int i = 0; i < 30; i++) {
            random.nextLong();
            if (512 * (1 + random.nextInt(5)) == 2560) return i + 1;
        }
        throw new AssertionError("Fixture has no 2560px artwork");
    }
}
