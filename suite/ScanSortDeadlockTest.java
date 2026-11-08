/*
 * @test
 * @comment Regression target: BUG-01; report: reports/BUG-01-scan-sort-deadlock.md
 * @summary Sort and scan publication must not deadlock the Swing EDT
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 ScanSortDeadlockTest
 */
import java.io.*;
import java.lang.management.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

/**
 * Requires a graphical desktop. Uses a child VM because a real EDT deadlock
 * cannot be cleaned up by posting dispose() to the same EDT.
 * The test gate orders two legitimate actions; gate waiting time is not UI latency.
 */
public class ScanSortDeadlockTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("child")) {
            child();
            return;
        }
        String java = Path.of(System.getProperty("test.jdk", System.getProperty("java.home")), "bin", "java").toString();
        String classpath = System.getProperty("test.class.path", System.getProperty("java.class.path"));
        Process child = new ProcessBuilder(java, "-cp", classpath,
                ScanSortDeadlockTest.class.getName(), "child").redirectErrorStream(true).start();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        Future<?> output = reader.submit(() -> {
            try (BufferedReader lines = child.inputReader()) {
                String line;
                while ((line = lines.readLine()) != null) {
                    System.out.println("child: " + line);
                    if (line.equals("CAPTURE_DEADLOCK")) {
                        captureJcmd(child.pid());
                        child.getOutputStream().write('\n');
                        child.getOutputStream().flush();
                    }
                }
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
        try {
            GalleryTestSupport.check(child.waitFor(45, TimeUnit.SECONDS),
                    "Child did not terminate within 45 s; see captured output");
            output.get(10, TimeUnit.SECONDS);
            GalleryTestSupport.check(child.exitValue() == 0,
                    "Sort must complete while scanning. Child exit=" + child.exitValue()
                            + "; inspect the EDT/loader cycle and jcmd output above");
        } finally {
            if (child.isAlive()) {
                child.destroy();
                if (!child.waitFor(2, TimeUnit.SECONDS)) child.destroyForcibly().waitFor();
            }
            reader.shutdownNow();
        }
    }

    private static void captureJcmd(long pid) {
        Path output = Path.of("deadlock-jcmd.txt");
        try {
            String jcmd = Path.of(System.getProperty("test.jdk", System.getProperty("java.home")), "bin", "jcmd").toString();
            Process command = new ProcessBuilder(jcmd, Long.toString(pid), "Thread.print", "-l")
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try {
                if (!command.waitFor(8, TimeUnit.SECONDS)) {
                    command.destroyForcibly().waitFor();
                    System.out.println("jcmd timed out; internal ThreadMXBean dump is the fallback");
                }
                System.out.println("JDK diagnostic output:\n" + Files.readString(output));
            } finally {
                if (command.isAlive()) command.destroyForcibly();
            }
        } catch (Exception e) { System.out.println("jcmd unavailable: " + e); }
    }

    private static void child() throws Exception {
        Gallery gallery = GalleryTestSupport.open(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FutureTask<Void> action = new FutureTask<>(() -> {
            gallery.rescan();
            started.countDown();
            GalleryTestSupport.check(release.await(12, TimeUnit.SECONDS), "Test gate timed out");
            GalleryTestSupport.component(gallery, "gallery.sort", JButton.class).doClick(0);
            return null;
        });
        boolean deadlocked = false;
        try {
            SwingUtilities.invokeLater(action);
            GalleryTestSupport.check(started.await(10, TimeUnit.SECONDS), "EDT did not enter scan action");
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            boolean ordered = false;
            while (System.nanoTime() < deadline) {
                for (ThreadInfo info : bean.dumpAllThreads(true, true)) {
                    if (info.getThreadName().equals("gallery-loader") && waitsForEdt(info)) {
                        System.out.println("Publication is waiting for EDT; held monitors=" + info.getLockedMonitors().length);
                        ordered = true;
                        break;
                    }
                }
                if (ordered || !gallery.isScanning()) break;
                java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2));
            }
            System.out.println("Controlled invokeAndWait interleaving observed=" + ordered);
            release.countDown();
            try {
                action.get(2, TimeUnit.SECONDS);
                GalleryTestSupport.edt(() -> null);
            } catch (TimeoutException e) {
                ThreadInfo loader = null;
                ThreadInfo edt = null;
                for (ThreadInfo info : bean.dumpAllThreads(true, true)) {
                    if (info.getThreadName().equals("gallery-loader")) loader = info;
                    if (info.getThreadName().startsWith("AWT-EventQueue")) edt = info;
                }
                deadlocked = loader != null && edt != null && waitsForEdt(loader)
                        && edt.getThreadState() == Thread.State.BLOCKED
                        && edt.getLockOwnerId() == loader.getThreadId();
                System.out.println("Confirmed EDT -> loader monitor -> EDT cycle=" + deadlocked);
                GalleryTestSupport.dumpThreads();
                System.out.println("CAPTURE_DEADLOCK");
                System.out.flush();
                // Parent gathers external JDK diagnostics, then acknowledges via stdin.
                System.in.read();
                System.err.println("FAIL: Sort did not complete within 2 s; confirmed deadlock=" + deadlocked);
                System.exit(1);
            }
        } finally {
            release.countDown();
            if (!deadlocked) GalleryTestSupport.edt(() -> { gallery.dispose(); return null; });
        }
    }

    private static boolean waitsForEdt(ThreadInfo info) {
        for (StackTraceElement frame : info.getStackTrace()) {
            if (frame.getMethodName().equals("invokeAndWait")
                    && (frame.getClassName().equals("java.awt.EventQueue")
                    || frame.getClassName().equals("javax.swing.SwingUtilities"))) return true;
        }
        return false;
    }
}
