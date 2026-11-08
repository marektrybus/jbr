import java.awt.*;
import java.lang.management.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Shared JDK-only support. All component access goes through the EDT. */
public final class GalleryTestSupport {
    private GalleryTestSupport() {}
    public static final long TIMEOUT_MS = 15_000;

    public static <T> T edt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        try {
            return task.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw e;
        } catch (TimeoutException e) {
            dumpThreads();
            throw new AssertionError("EDT did not complete the requested action within "
                    + TIMEOUT_MS + " ms", e);
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static Component find(Container root, String name) {
        if (name.equals(root.getName())) return root;
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container container) {
                Component nested = find(container, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    public static <T extends Component> T component(Gallery gallery, String name, Class<T> type) {
        check(SwingUtilities.isEventDispatchThread(), "Component lookup must run on EDT");
        Component found = find(gallery.getFrame(), name);
        check(type.isInstance(found), "Missing named component " + name + " of type " + type);
        return type.cast(found);
    }

    @SuppressWarnings("unchecked")
    public static JList<Gallery.Artwork> list(Gallery gallery) {
        return (JList<Gallery.Artwork>) component(gallery, "gallery.list", JList.class);
    }

    public static Gallery open(int count) throws Exception {
        check(!GraphicsEnvironment.isHeadless(), "This test requires a graphical desktop");
        return edt(() -> {
            Gallery gallery = new Gallery(count);
            gallery.getFrame().setVisible(true);
            return gallery;
        });
    }

    /** Completes on the list event that publishes the final artwork. */
    public static void scan(Gallery gallery, int count) throws Exception {
        CountDownLatch loaded = new CountDownLatch(1);
        javax.swing.event.ListDataListener listener = new javax.swing.event.ListDataListener() {
            private void changed() {
                if (list(gallery).getModel().getSize() == count) loaded.countDown();
            }
            public void intervalAdded(javax.swing.event.ListDataEvent e) { changed(); }
            public void intervalRemoved(javax.swing.event.ListDataEvent e) { changed(); }
            public void contentsChanged(javax.swing.event.ListDataEvent e) { changed(); }
        };
        edt(() -> {
            ((DefaultListModel<?>) list(gallery).getModel()).addListDataListener(listener);
            gallery.rescan();
            if (count == 0) loaded.countDown();
            return null;
        });
        try {
            check(loaded.await(TIMEOUT_MS, TimeUnit.MILLISECONDS),
                    "Scan did not publish " + count + " artworks within " + TIMEOUT_MS + " ms");
            // Wait for actual loader termination, not just the final model event.
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
            while (gallery.isScanning() && System.nanoTime() < deadline) {
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            }
            check(!gallery.isScanning(), "Loader still alive after final model event");
        } finally {
            edt(() -> {
                ((DefaultListModel<?>) list(gallery).getModel()).removeListDataListener(listener);
                return null;
            });
        }
    }

    public static Rectangle usable(GraphicsConfiguration gc) {
        Rectangle bounds = new Rectangle(gc.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    public static void dumpThreads() {
        for (ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
            System.out.println(thread);
            for (StackTraceElement frame : thread.getStackTrace()) System.out.println("    at " + frame);
            for (MonitorInfo monitor : thread.getLockedMonitors()) System.out.println("    holds " + monitor);
        }
    }

    @FunctionalInterface public interface Scenario { void run(Gallery gallery) throws Exception; }

    public static void run(int count, Scenario action) throws Exception {
        // A daemon watchdog ensures even a blocked cleanup cannot leave this VM alive.
        Thread watchdog = new Thread(() -> {
            try {
                new CountDownLatch(1).await(70, TimeUnit.SECONDS);
                System.err.println("Watchdog expired: collecting threads before exiting test VM");
                dumpThreads();
                Runtime.getRuntime().halt(3);
            } catch (InterruptedException ignored) { }
        }, "test-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        Gallery gallery = null;
        Throwable failure = null;
        boolean cleanedUp = false;
        try {
            gallery = open(count);
            action.run(gallery);
        } catch (Throwable t) {
            failure = t;
        } finally {
            if (gallery != null) {
                Gallery closing = gallery;
                try {
                    edt(() -> { closing.dispose(); return null; });
                    cleanedUp = true;
                }
                catch (Throwable cleanup) {
                    if (failure == null) failure = cleanup;
                    else failure.addSuppressed(cleanup);
                }
            }
            // If EDT is stuck the watchdog must remain armed.
            if (gallery == null || cleanedUp) watchdog.interrupt();
        }
        if (failure instanceof Exception e) throw e;
        if (failure instanceof Error e) throw e;
        if (failure != null) throw new RuntimeException(failure);
    }

}
