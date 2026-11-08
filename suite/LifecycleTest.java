/*
 * @test
 * @summary Disposing Gallery stops its current loader within a bounded grace period
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 LifecycleTest
 */
import java.lang.management.*;
import java.util.concurrent.*;

/** Requires a graphical desktop. Does not assert immediate interrupt termination. */
public class LifecycleTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(40, gallery -> {
            GalleryTestSupport.edt(() -> {
                gallery.rescan();
                gallery.dispose();
                GalleryTestSupport.check(!gallery.getFrame().isDisplayable(), "Disposed frame remains displayable");
                return null;
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            boolean alive;
            do {
                alive = false;
                for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)) {
                    if (info.getThreadName().equals("gallery-loader")) alive = true;
                }
                if (!alive) break;
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            } while (System.nanoTime() < deadline);
            if (alive) GalleryTestSupport.dumpThreads();
            GalleryTestSupport.check(!alive, "gallery-loader still alive 3 s after dispose()");
        });
    }
}
