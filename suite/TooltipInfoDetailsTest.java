/*
 * @test
 * @summary Real thumbnail hover shows details matching Info before and after sorting; popup click dismisses it
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport GalleryUiTestSupport SortTestSupport
 * @run main/othervm/timeout=120 TooltipInfoDetailsTest
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Requires an unobstructed graphical desktop and OS permission for Robot input. */
public class TooltipInfoDetailsTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(3, gallery -> {
            GalleryTestSupport.scan(gallery, 3);
            GalleryUiTestSupport.activate(gallery);
            Robot robot = new Robot();
            GalleryTestSupport.edt(() -> { GalleryTestSupport.list(gallery).setSelectedIndex(2); return null; });
            checkHoverAndInfo(gallery, robot);
            GalleryTestSupport.edt(() -> { SortTestSupport.toggle(gallery); return null; });
            checkHoverAndInfo(gallery, robot);

            GalleryTestSupport.edt(() -> {
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                Rectangle last = list.getCellBounds(list.getModel().getSize() - 1, list.getModel().getSize() - 1);
                Point empty = new Point(1, list.getHeight() - 1);
                GalleryTestSupport.check(empty.y > last.y + last.height, "Fixture has no empty grid area");
                String tip = list.getToolTipText(new MouseEvent(list, MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(), 0, empty.x, empty.y, 0, false));
                GalleryTestSupport.check(tip == null, "Empty grid space must not describe the nearest thumbnail: " + tip);
                return null;
            });
        });
    }

    private static void checkHoverAndInfo(Gallery gallery, Robot robot) throws Exception {
        JButton info = GalleryTestSupport.edt(() -> GalleryTestSupport.component(gallery, "gallery.info", JButton.class));
        Point away = GalleryTestSupport.edt(() -> info.getLocationOnScreen());
        robot.mouseMove(away.x + 2, away.y + 2);
        int index = GalleryTestSupport.edt(() -> GalleryTestSupport.list(gallery).getSelectedIndex());
        Point hover = GalleryUiTestSupport.cellPoint(gallery, index);
        robot.mouseMove(hover.x, hover.y);
        GalleryUiTestSupport.awaitEdt("hover creates a visible tooltip for the grid", () ->
                GalleryUiTestSupport.visibleTooltip(GalleryTestSupport.list(gallery)) != null);
        String tooltip = GalleryTestSupport.edt(() ->
                GalleryUiTestSupport.visibleTooltip(GalleryTestSupport.list(gallery)).getTipText());
        GalleryTestSupport.edt(() -> {
            JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
            Gallery.Artwork art = list.getSelectedValue();
            GalleryTestSupport.check(tooltip.contains(art.name)
                    && tooltip.contains(art.size + " x " + art.size + " px")
                    && tooltip.contains(String.format("seed %016x", art.seed))
                    && tooltip.contains("#" + (list.getSelectedIndex() + 1) + " of " + list.getModel().getSize() + " loaded"),
                    "Tooltip lacks expected artwork details: " + tooltip);
            return null;
        });
        GalleryUiTestSupport.click(robot, info);
        GalleryUiTestSupport.awaitEdt("Info button opens the named popup", () -> GalleryUiTestSupport.infoPopup(gallery) != null);
        JLabel content = GalleryTestSupport.edt(() -> GalleryUiTestSupport.findType(
                GalleryUiTestSupport.infoPopup(gallery), JLabel.class));
        GalleryTestSupport.edt(() -> {
            GalleryTestSupport.check(content != null && tooltip.equals(content.getText()),
                    "Hovered tooltip and Info must show the same details; tooltip=" + tooltip
                            + "; Info=" + (content == null ? null : content.getText()));
            System.out.println("Verified real-hover tooltip and Info: " + tooltip);
            return null;
        });
        GalleryUiTestSupport.click(robot, content);
        GalleryUiTestSupport.awaitEdt("clicking popup dismisses it", () -> GalleryUiTestSupport.infoPopup(gallery) == null);
    }
}
