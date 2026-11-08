/*
 * @test
 * @comment Regression target: BUG-04; report: reports/BUG-04-window-centering.md
 * @summary Gallery opens centered in its usable screen area
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 WindowPlacementTest
 */
import java.awt.*;

/** Requires a graphical desktop whose window manager permits explicit placement. */
public class WindowPlacementTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(1, gallery -> {
            new Robot().waitForIdle();
            GalleryTestSupport.edt(() -> {
                Rectangle actual = gallery.getFrame().getBounds();
                Rectangle usable = GalleryTestSupport.usable(gallery.getFrame().getGraphicsConfiguration());
                int expectedX = usable.x + (usable.width - actual.width) / 2;
                int expectedY = usable.y + (usable.height - actual.height) / 2;
                System.out.println("frame=" + actual + "; usable=" + usable
                        + "; expectedTopLeft=" + new Point(expectedX, expectedY) + "; tolerance=4 AWT units");
                GalleryTestSupport.check(Math.abs(actual.x - expectedX) <= 4
                                && Math.abs(actual.y - expectedY) <= 4,
                        "Window must be centered in usable screen: expected top-left "
                                + new Point(expectedX, expectedY) + " but got " + actual.getLocation());
                return null;
            });
        });
    }
}
