/*
 * @test
 * @summary Rescan clears selection, grid, preview and progress; completed new scan has an empty preview cache
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport GalleryUiTestSupport
 * @run main/othervm/timeout=120 RescanStateTest
 */
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/** Requires a graphical desktop. Renders the public preview component offscreen; no reflection. */
public class RescanStateTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(3, gallery -> {
            BufferedImage empty = GalleryTestSupport.edt(() -> GalleryUiTestSupport.previewSnapshot(gallery));
            GalleryTestSupport.scan(gallery, 3);
            GalleryTestSupport.edt(() -> {
                GalleryTestSupport.list(gallery).setSelectedIndex(2);
                BufferedImage selected = GalleryUiTestSupport.previewSnapshot(gallery);
                GalleryTestSupport.check(!Arrays.equals(GalleryUiTestSupport.pixels(empty), GalleryUiTestSupport.pixels(selected)),
                        "Selected artwork did not change the rendered preview");
                return null;
            });
            GalleryUiTestSupport.awaitStatus(gallery, 1);
            GalleryTestSupport.edt(() -> {
                gallery.rescan();
                JList<Gallery.Artwork> list = GalleryTestSupport.list(gallery);
                JProgressBar progress = GalleryTestSupport.component(gallery, "gallery.progress", JProgressBar.class);
                GalleryTestSupport.check(list.getModel().getSize() == 0 && list.getSelectedIndex() == -1,
                        "Rescan must clear the grid and selection before new publication");
                GalleryTestSupport.check("Gallery".equals(gallery.getFrame().getTitle()), "Rescan left a stale selection title");
                GalleryTestSupport.check(progress.getValue() == 0 && !"Done".equals(progress.getString()),
                        "Rescan did not reset progress");
                BufferedImage reset = GalleryUiTestSupport.previewSnapshot(gallery);
                GalleryTestSupport.check(empty.getWidth() == reset.getWidth() && empty.getHeight() == reset.getHeight()
                                && Arrays.equals(GalleryUiTestSupport.pixels(empty), GalleryUiTestSupport.pixels(reset)),
                        "Rescan did not restore the initial empty preview rendering");
                System.out.println("Immediate Rescan reset verified: grid, selection, title, progress and preview");
                return null;
            });
            GalleryUiTestSupport.awaitEdt("the same Rescan finishes with three artworks", () ->
                    !gallery.isScanning() && GalleryTestSupport.list(gallery).getModel().getSize() == 3);
            GalleryUiTestSupport.awaitStatus(gallery, 0);
        });
    }
}
