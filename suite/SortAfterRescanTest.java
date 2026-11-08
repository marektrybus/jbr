/*
 * @test
 * @comment Regression target: BUG-05; report: reports/BUG-05-new-items-ignore-sort.md
 * @summary Size sorting remains active after Rescan
 * @key headful
 * @library /suite/lib
 * @build Gallery GalleryTestSupport SortTestSupport
 * @run main/othervm/timeout=120 SortAfterRescanTest
 */
/** Requires a graphical desktop. Uses no selection to avoid masking by preview rendering. */
public class SortAfterRescanTest {
    public static void main(String[] args) throws Exception {
        int count = SortTestSupport.fixtureCount();
        GalleryTestSupport.run(count, gallery -> {
            GalleryTestSupport.scan(gallery, count);
            GalleryTestSupport.edt(() -> {
                SortTestSupport.toggle(gallery);
                SortTestSupport.assertSizeOrder(gallery);
                return null;
            });
            GalleryTestSupport.scan(gallery, count);
            GalleryTestSupport.edt(() -> { SortTestSupport.assertSizeOrder(gallery); return null; });
        });
    }
}
