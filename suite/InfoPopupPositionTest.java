/*
 * @test
 * @comment Regression target: BUG-03; report: reports/BUG-03-info-popup-position.md
 * @summary Info popup follows the selected cell in screen coordinates after moving the frame
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport
 * @run main/othervm/timeout=120 InfoPopupPositionTest
 */
import java.awt.*;
import javax.swing.*;

/** Requires a graphical desktop and a window manager permitting explicit placement. */
public class InfoPopupPositionTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(1, gallery -> {
            GalleryTestSupport.scan(gallery, 1);
            GalleryTestSupport.edt(() -> {
                Rectangle screen = GalleryTestSupport.usable(gallery.getFrame().getGraphicsConfiguration());
                gallery.getFrame().setLocation(screen.x + 60, screen.y + 70);
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                list.setSelectedIndex(0);
                return null;
            });
            new Robot().waitForIdle();
            GalleryTestSupport.edt(() -> {
                gallery.showInfo();
                JWindow popup = null;
                for (Window window : gallery.getFrame().getOwnedWindows()) {
                    if ("gallery.infoPopup".equals(window.getName()) && window.isVisible()) popup = (JWindow) window;
                }
                GalleryTestSupport.check(popup != null, "Info did not open a visible named popup");
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                Rectangle cell = list.getCellBounds(0, 0);
                Point anchor = new Point(cell.x, cell.y + cell.height);
                SwingUtilities.convertPointToScreen(anchor, list);
                Rectangle usable = GalleryTestSupport.usable(gallery.getFrame().getGraphicsConfiguration());
                Point expected = new Point(
                        Math.max(usable.x, Math.min(anchor.x, usable.x + usable.width - popup.getWidth())),
                        Math.max(usable.y, Math.min(anchor.y, usable.y + usable.height - popup.getHeight())));
                Point actual = popup.getLocationOnScreen();
                System.out.println("cell=" + cell + "; listOnScreen=" + list.getLocationOnScreen()
                        + "; anchor=" + anchor + "; usable=" + usable + "; popup=" + popup.getBounds()
                        + "; expected=" + expected + "; tolerance=2 AWT units");
                GalleryTestSupport.check(Math.abs(actual.x - expected.x) <= 2
                                && Math.abs(actual.y - expected.y) <= 2,
                        "Info popup must meet the selected cell's bottom-left screen corner (clamped): expected "
                                + expected + " but got " + actual);
                return null;
            });
        });
    }
}
