import java.util.*;
import javax.swing.*;

public final class SortTestSupport {
    private SortTestSupport() {}
    public static void assertSizeOrder(Gallery gallery) {
        ListModel<Gallery.Artwork> model = GalleryTestSupport.list(gallery).getModel();
        ArrayList<Gallery.Artwork> actual = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) actual.add(model.getElementAt(i));
        ArrayList<Gallery.Artwork> expected = new ArrayList<>(actual);
        expected.sort(Comparator.comparingInt((Gallery.Artwork a) -> a.size).thenComparing(a -> a.name));
        GalleryTestSupport.check(expected.size() > 1, "Fixture must contain multiple artworks");
        GalleryTestSupport.check(actual.equals(expected),
                "Newly loaded artworks must follow selected size/name order; expected="
                        + describe(expected) + "; actual=" + describe(actual));
    }
    public static String describe(List<Gallery.Artwork> values) {
        return values.stream().map(a -> a.name + ":" + a.size).toList().toString();
    }
    public static void toggle(Gallery gallery) {
        GalleryTestSupport.component(gallery, "gallery.sort", JButton.class).doClick(0);
    }
    public static int fixtureCount() {
        Random random = new Random(20240601L);
        int previous = -1;
        for (int i = 0; i < 30; i++) {
            random.nextLong();
            int size = 512 * (1 + random.nextInt(5));
            if (size < previous) return i + 1;
            previous = size;
        }
        throw new AssertionError("Fixture has no size inversion; choose a different fixture");
    }
}
