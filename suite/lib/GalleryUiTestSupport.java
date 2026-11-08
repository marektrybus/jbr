import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.concurrent.*;
import javax.swing.*;

/** JDK-only input and condition helpers; Swing access is always dispatched to EDT. */
public final class GalleryUiTestSupport {
    private GalleryUiTestSupport() { }

    public static void awaitEdt(String description, Callable<Boolean> condition) throws Exception {
        GalleryTestSupport.check(!SwingUtilities.isEventDispatchThread(), "Wait must run outside EDT");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (GalleryTestSupport.edt(condition)) return;
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("Condition not met within 5 seconds: " + description);
    }

    public static void activate(Gallery gallery) throws Exception {
        GalleryTestSupport.edt(() -> {
            JFrame frame = gallery.getFrame();
            Rectangle usable = GalleryTestSupport.usable(frame.getGraphicsConfiguration());
            frame.setLocation(usable.x + 20, usable.y + 20);
            frame.toFront();
            frame.requestFocus();
            return null;
        });
        awaitEdt("Gallery is the active window", () -> gallery.getFrame().isActive());
    }

    public static void click(Robot robot, Component component) throws Exception {
        Point center = GalleryTestSupport.edt(() -> {
            Point p = component.getLocationOnScreen();
            p.translate(component.getWidth() / 2, component.getHeight() / 2);
            return p;
        });
        click(robot, center);
    }

    public static Point cellPoint(Gallery gallery, int index) throws Exception {
        return GalleryTestSupport.edt(() -> {
            JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
            list.ensureIndexIsVisible(index);
            Rectangle cell = list.getCellBounds(index, index);
            Point p = new Point(cell.x + cell.width / 2, cell.y + cell.height / 2);
            SwingUtilities.convertPointToScreen(p, list);
            return p;
        });
    }

    public static void click(Robot robot, Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static JWindow infoPopup(Gallery gallery) {
        for (Window window : gallery.getFrame().getOwnedWindows()) {
            if ("gallery.infoPopup".equals(window.getName()) && window.isShowing()) return (JWindow) window;
        }
        return null;
    }

    public static <T extends Component> T findType(Container root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) return type.cast(c);
            if (c instanceof Container container) {
                T nested = findType(container, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    public static JToolTip visibleTooltip(JList<?> list) {
        for (Window window : Window.getWindows()) {
            JToolTip tip = findType(window, JToolTip.class);
            if (tip != null && tip.isShowing() && tip.getComponent() == list) return tip;
        }
        return null;
    }

    public static BufferedImage previewSnapshot(Gallery gallery) {
        JSplitPane split = findType(gallery.getFrame(), JSplitPane.class);
        GalleryTestSupport.check(split != null, "Missing split pane containing the preview");
        Component pane = split.getRightComponent();
        GalleryTestSupport.check(pane != null && pane.getWidth() > 0 && pane.getHeight() > 0,
                "Preview pane has no visible area");
        BufferedImage image = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { pane.printAll(graphics); }
        finally { graphics.dispose(); }
        return image;
    }

    public static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    public static String expectedStatus(Gallery gallery, int cacheCount) {
        ListModel<Gallery.Artwork> model = GalleryTestSupport.list(gallery).getModel();
        long pixels = 0;
        for (int i = 0; i < model.getSize(); i++) {
            long size = model.getElementAt(i).size;
            pixels += size * size;
        }
        return String.format("%d artworks, %.1f Mpx, %d previews cached", model.getSize(), pixels / 1e6, cacheCount);
    }

    public static void awaitStatus(Gallery gallery, int cacheCount) throws Exception {
        String expected = GalleryTestSupport.edt(() -> expectedStatus(gallery, cacheCount));
        awaitEdt("status equals " + expected, () -> expected.equals(
                GalleryTestSupport.component(gallery, "gallery.status", JLabel.class).getText()));
        System.out.println("Verified status: " + expected);
    }
}
