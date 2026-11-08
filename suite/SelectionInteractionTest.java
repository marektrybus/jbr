/*
 * @test
 * @summary Mouse and arrow keys select artworks; sorting preserves selection, title and visibility
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport GalleryUiTestSupport SortTestSupport
 * @run main/othervm/timeout=120 SelectionInteractionTest
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** Requires an unobstructed graphical desktop and OS permission for Robot input. */
public class SelectionInteractionTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(6, gallery -> {
            GalleryTestSupport.scan(gallery, 6);
            GalleryUiTestSupport.activate(gallery);
            Robot robot = new Robot();
            GalleryTestSupport.edt(() -> { assertNameOrder(gallery); return null; });

            GalleryUiTestSupport.click(robot, GalleryUiTestSupport.cellPoint(gallery, 2));
            GalleryUiTestSupport.awaitEdt("mouse selects art-002 and updates title", () -> {
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                return list.getSelectedIndex() == 2 && "Gallery - art-002".equals(gallery.getFrame().getTitle());
            });
            GalleryUiTestSupport.awaitEdt("thumbnail grid has keyboard focus", () -> GalleryTestSupport.list(gallery).isFocusOwner());
            robot.keyPress(KeyEvent.VK_LEFT);
            robot.keyRelease(KeyEvent.VK_LEFT);
            GalleryUiTestSupport.awaitEdt("Left arrow changes selection and updates title", () -> {
                Gallery.Artwork selected = GalleryTestSupport.list(gallery).getSelectedValue();
                return selected != null && selected.id != 2
                        && ("Gallery - " + selected.name).equals(gallery.getFrame().getTitle());
            });
            Gallery.Artwork selected = GalleryTestSupport.edt(() -> GalleryTestSupport.list(gallery).getSelectedValue());
            GalleryTestSupport.edt(() -> {
                SortTestSupport.toggle(gallery);
                SortTestSupport.assertSizeOrder(gallery); // Fixture includes equal sizes: name must break ties.
                assertSelection(gallery, selected);
                SortTestSupport.toggle(gallery);
                assertNameOrder(gallery);
                assertSelection(gallery, selected);
                System.out.println("Mouse/arrow selection, both sort orders, size ties, title and visibility verified for " + selected.name);
                return null;
            });
        });
    }

    private static void assertNameOrder(Gallery gallery) {
        ListModel<Gallery.Artwork> model = GalleryTestSupport.list(gallery).getModel();
        for (int i = 1; i < model.getSize(); i++) {
            GalleryTestSupport.check(model.getElementAt(i - 1).name.compareTo(model.getElementAt(i).name) <= 0,
                    "Expected ascending name order at index " + i);
        }
    }

    private static void assertSelection(Gallery gallery, Gallery.Artwork expected) {
        JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
        GalleryTestSupport.check(list.getSelectedValue() == expected, "Sort changed the selected artwork");
        GalleryTestSupport.check(("Gallery - " + expected.name).equals(gallery.getFrame().getTitle()),
                "Window title does not match the preserved selection");
        Rectangle cell = list.getCellBounds(list.getSelectedIndex(), list.getSelectedIndex());
        GalleryTestSupport.check(list.getVisibleRect().contains(cell), "Selected cell is not fully visible after Sort: " + cell);
    }
}
