/*
 * @test
 * @summary Status reports loaded count, total megapixels and unique cached previews
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport GalleryUiTestSupport
 * @run main/othervm/timeout=120 StatusCacheTest
 */
/** Requires a graphical desktop. Waits for actual status text transitions, not fixed sleeps. */
public class StatusCacheTest {
    public static void main(String[] args) throws Exception {
        GalleryTestSupport.run(3, gallery -> {
            GalleryTestSupport.scan(gallery, 3);
            GalleryUiTestSupport.awaitStatus(gallery, 0);
            GalleryTestSupport.edt(() -> { GalleryTestSupport.list(gallery).setSelectedIndex(2); return null; });
            GalleryUiTestSupport.awaitStatus(gallery, 1);
            GalleryTestSupport.edt(() -> { GalleryTestSupport.list(gallery).setSelectedIndex(0); return null; });
            GalleryUiTestSupport.awaitStatus(gallery, 2);
            GalleryTestSupport.edt(() -> { GalleryTestSupport.list(gallery).setSelectedIndex(2); return null; });
            GalleryUiTestSupport.awaitStatus(gallery, 2);
            GalleryTestSupport.scan(gallery, 3);
            GalleryUiTestSupport.awaitStatus(gallery, 0);
        });
    }
}
