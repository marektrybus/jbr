/*
 * @test
 * @summary Record the actual AWT desktop environment and check Gallery's basic layout
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 DesktopEnvironmentTest
 */
import java.awt.*;
import javax.swing.*;

/** Requires a logged-in graphical session. Does not require Robot permissions. */
public class DesktopEnvironmentTest {
    public static void main(String[] args) throws Exception {
        System.out.println("OS=" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("arch=" + System.getProperty("os.arch"));
        System.out.println("JDK=" + System.getProperty("java.runtime.version"));
        System.out.println("headless=" + GraphicsEnvironment.isHeadless());
        GalleryTestSupport.check(!GraphicsEnvironment.isHeadless(), "A graphical desktop is required");
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            System.out.println("display=" + device.getIDstring() + " bounds=" + gc.getBounds()
                    + " usable=" + GalleryTestSupport.usable(gc) + " scale=" + gc.getDefaultTransform());
        }
        GalleryTestSupport.run(1, gallery -> GalleryTestSupport.edt(() -> {
            for (String name : new String[]{"gallery.rescan", "gallery.sort", "gallery.info"}) {
                GalleryTestSupport.component(gallery, name, JButton.class);
            }
            GalleryTestSupport.list(gallery);
            GalleryTestSupport.component(gallery, "gallery.status", JLabel.class);
            GalleryTestSupport.component(gallery, "gallery.progress", JProgressBar.class);
            GalleryTestSupport.check(gallery.getFrame().isShowing(), "Gallery frame is not showing");
            GalleryTestSupport.check("Gallery".equals(gallery.getFrame().getTitle()), "Unexpected initial title");
            return null;
        }));
    }
}
