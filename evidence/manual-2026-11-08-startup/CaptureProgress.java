import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Diagnostic capture of unmodified Gallery's real main-entry startup. */
public class CaptureProgress {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        Gallery.main(new String[] { "400" });
        long deadline = System.nanoTime() + 30_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean captured = GalleryTestSupport.edt(() -> {
                for (Frame frame : Frame.getFrames()) {
                    if (!(frame instanceof JFrame window) || !"Gallery".equals(window.getTitle())) continue;
                    JProgressBar bar = (JProgressBar) GalleryTestSupport.find(window, "gallery.progress");
                    if (bar == null || bar.getValue() != 400 || !"Done".equals(bar.getString())) continue;
                    JLabel status = (JLabel) GalleryTestSupport.find(window, "gallery.status");
                    if (!status.getText().startsWith("400 artworks,") || status.getText().contains("scanning")) continue;
                    capture(window.getContentPane(), output.resolve("completed-window-content.png"));
                    capture(bar.getParent(), output.resolve("completed-status-panel.png"));
                    capture(bar, output.resolve("completed-progress.png"));
                    System.out.println("Captured at: " + java.time.ZonedDateTime.now());
                    System.out.println("Entry point: Gallery.main(new String[] {\"400\"})");
                    System.out.println("Java: " + System.getProperty("java.runtime.version"));
                    System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                    System.out.println("Look and feel: " + UIManager.getLookAndFeel().getClass().getName());
                    System.out.println("Progress UI: " + bar.getUI().getClass().getName());
                    System.out.println("Progress: " + bar.getValue() + "/" + bar.getMaximum() + "; text=" + bar.getString());
                    System.out.println("Status: " + status.getText());
                    System.out.println("Progress size: " + bar.getSize());
                    System.out.println("Foreground: " + bar.getForeground() + "; background: " + bar.getBackground());
                    System.out.println("Selection foreground: " + UIManager.getColor("ProgressBar.selectionForeground"));
                    System.out.println("Selection background: " + UIManager.getColor("ProgressBar.selectionBackground"));
                    System.out.println("Screen transform: " + window.getGraphicsConfiguration().getDefaultTransform());
                    System.out.println("Capture method: Swing component printAll on EDT, native logical dimensions; not an OS screenshot.");
                    return true;
                }
                return false;
            });
            if (captured) System.exit(0);
            Thread.sleep(50);
        }
        System.err.println("Completed progress not observed within 30 seconds");
        System.exit(1);
    }

    private static void capture(Component component, Path path) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.printAll(graphics); }
        finally { graphics.dispose(); }
        ImageIO.write(image, "png", path.toFile());
    }
}
