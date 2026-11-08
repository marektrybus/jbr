import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gallery - a small viewer for procedurally generated artworks.
 *
 * <pre>
 *   java Gallery.java      # 400 artworks
 *   java Gallery.java 40   # custom number of artworks
 * </pre>
 *
 * Requires JDK 17 or newer. No external dependencies.
 *
 * <h2>Test API</h2>
 * This class is in the default package. Its public constructor, declared public methods
 * and component names below are stable and may be used by tests. See the constructor
 * and method documentation for behaviour and threading requirements.
 * The loader thread is named {@code gallery-loader}.
 *
 * <table>
 * <caption>Named components</caption>
 * <thead>
 * <tr><th scope="col">Component name</th><th scope="col">Component</th></tr>
 * </thead>
 * <tbody>
 * <tr><td>{@code gallery.list}</td><td>Thumbnail grid ({@link JList})</td></tr>
 * <tr><td>{@code gallery.rescan}, {@code gallery.sort}, {@code gallery.info}</td>
 *     <td>Toolbar buttons</td></tr>
 * <tr><td>{@code gallery.progress}</td><td>Progress bar</td></tr>
 * <tr><td>{@code gallery.status}</td><td>Status line</td></tr>
 * <tr><td>{@code gallery.infoPopup}</td>
 *     <td>Info popup ({@link JWindow}, owned by the frame)</td></tr>
 * </tbody>
 * </table>
 */
public class Gallery {

    /** One artwork: a square image rendered on demand from a seed. */
    static final class Artwork {
        final int id;
        final String name;
        final long seed;
        final int size;                 // edge length in pixels
        volatile BufferedImage thumbnail;

        Artwork(int id, long seed, int size) {
            this.id = id;
            this.name = String.format("art-%03d", id);
            this.seed = seed;
            this.size = size;
        }

        long pixels() {
            return (long) size * size;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final int THUMB = 96;    // thumbnail edge
    private static final int CELL = 112;    // grid cell width
    private static final int PREVIEW_CACHE = 16;

    private final List<Artwork> catalog = new ArrayList<>();   // guarded by synchronized (catalog)
    private final DefaultListModel<Artwork> listModel = new DefaultListModel<>();
    private final Map<Integer, BufferedImage> previewCache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> eldest) {
            return size() > PREVIEW_CACHE;
        }
    };

    private final JFrame frame = new JFrame("Gallery");
    private final JList<Artwork> list;
    private final PreviewPane preview = new PreviewPane();
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel(" ");
    private final Timer statusTimer;
    private JWindow infoPopup;

    private final int count;
    private boolean sortBySize;
    private volatile int scanGeneration;
    private volatile Thread loader;

    /**
     * Builds the UI for the given number of artworks without showing the window.
     * Must be called on the event dispatch thread (EDT).
     *
     * @param count number of artworks to generate per scan
     */
    public Gallery(int count) {
        this.count = count;
        list = new JList<>(listModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int i = locationToIndex(e.getPoint());
                if (i < 0 || !getCellBounds(i, i).contains(e.getPoint())) {
                    return null;
                }
                return describe(getModel().getElementAt(i));
            }
        };
        ToolTipManager.sharedInstance().registerComponent(list);
        buildUi();
        statusTimer = new Timer(250, e -> refreshStatus());
        statusTimer.start();
    }

    /**
     * Returns the main window.
     *
     * @return the main window
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns whether the loader thread is alive.
     *
     * @return {@code true} while the loader thread is alive
     */
    public boolean isScanning() {
        Thread t = loader;
        return t != null && t.isAlive();
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        list.setName("gallery.list");
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        list.setFixedCellWidth(CELL);
        list.setFixedCellHeight(CELL + 18);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ThumbnailRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showPreview(list.getSelectedValue());
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showInfo();
                }
            }
        });
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcut), "gallery.info");
        list.getActionMap().put("gallery.info", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showInfo();
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(4 * CELL + 40, 500));
        preview.setPreferredSize(new Dimension(500, 500));

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(button("Rescan", "gallery.rescan", this::rescan));
        bar.add(button("Sort", "gallery.sort", this::toggleSort));
        bar.add(button("Info", "gallery.info", this::showInfo));

        progress.setName("gallery.progress");
        progress.setStringPainted(true);
        status.setName("gallery.status");
        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        south.add(progress, BorderLayout.WEST);
        south.add(status, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, preview);
        split.setResizeWeight(0);

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(bar, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);      // centre on the screen
        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdown();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                hideInfo();
            }
        });
    }

    private static JButton button(String text, String name, Runnable action) {
        JButton b = new JButton(text);
        b.setName(name);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void refreshStatus() {
        int n;
        long px = 0;
        synchronized (catalog) {
            n = catalog.size();
            for (Artwork a : catalog) {
                px += a.pixels();
            }
        }
        status.setText(String.format("%d artworks, %.1f Mpx, %d previews cached%s",
                n, px / 1e6, previewCache.size(), isScanning() ? ", scanning..." : ""));
    }

    private String describe(Artwork a) {
        int position;
        synchronized (catalog) {
            position = catalog.indexOf(a) + 1;
        }
        return String.format("<html><b>%s</b><br>%d x %d px (%.1f Mpx)<br>seed %016x<br>#%d of %d loaded</html>",
                a.name, a.size, a.size, a.pixels() / 1e6, a.seed, position, listModel.size());
    }

    private void showPreview(Artwork a) {
        if (a == null) {
            preview.setImage(null);
            frame.setTitle("Gallery");
            return;
        }
        preview.setImage(previewCache.computeIfAbsent(a.id, id -> render(a, a.size)));
        frame.setTitle("Gallery - " + a.name);
    }

    private void toggleSort() {
        sortBySize = !sortBySize;
        Comparator<Artwork> order = sortBySize
                ? Comparator.comparingInt((Artwork a) -> a.size).thenComparing(a -> a.name)
                : Comparator.comparing(a -> a.name);
        Artwork selected = list.getSelectedValue();
        List<Artwork> items;
        synchronized (catalog) {
            catalog.sort(order);
            items = new ArrayList<>(catalog);
        }
        listModel.clear();
        listModel.addAll(items);
        if (selected != null) {
            list.setSelectedValue(selected, true);
        }
    }

    /**
     * Opens the details popup for the selected artwork, directly below its thumbnail.
     * Must be called on the EDT.
     */
    public void showInfo() {
        int i = list.getSelectedIndex();
        if (i < 0) {
            return;
        }
        hideInfo();
        Artwork a = listModel.get(i);

        JLabel content = new JLabel(describe(a));
        content.setOpaque(true);
        content.setBackground(new Color(255, 255, 225));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hideInfo();
            }
        });

        JWindow popup = new JWindow(frame);
        popup.setName("gallery.infoPopup");
        popup.setFocusableWindowState(false);
        popup.getContentPane().add(content);
        popup.pack();

        Rectangle cell = list.getCellBounds(i, i);
        Point p = new Point(cell.x, cell.y + cell.height);     // bottom-left corner of the cell
        // keep the popup inside the usable area of the screen that shows the main window
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle screen = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        screen.x += insets.left;
        screen.y += insets.top;
        screen.width -= insets.left + insets.right;
        screen.height -= insets.top + insets.bottom;
        Dimension size = popup.getSize();
        p.x = Math.max(screen.x, Math.min(p.x, screen.x + screen.width - size.width));
        p.y = Math.max(screen.y, Math.min(p.y, screen.y + screen.height - size.height));
        popup.setLocation(p);
        popup.setVisible(true);
        infoPopup = popup;
    }

    private void hideInfo() {
        if (infoPopup != null) {
            infoPopup.dispose();
            infoPopup = null;
        }
    }

    // ---------------------------------------------------------------- scanning

    /**
     * Starts a new scan, cancelling a running scan first.
     * Must be called on the EDT.
     */
    public void rescan() {
        cancelScan();
        int generation = ++scanGeneration;
        listModel.clear();
        previewCache.clear();
        preview.setImage(null);
        synchronized (catalog) {
            catalog.clear();
        }
        progress.setMaximum(count);
        progress.setValue(0);
        progress.setString(null);
        Thread t = new Thread(() -> scan(generation), "gallery-loader");
        t.setDaemon(true);
        loader = t;
        t.start();
    }

    private void cancelScan() {
        Thread t = loader;
        if (t != null) {
            t.interrupt();
        }
        loader = null;
    }

    private void scan(int generation) {
        Random rnd = new Random(20240601L);
        for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
            Artwork a = new Artwork(i, rnd.nextLong(), 512 * (1 + rnd.nextInt(5)));
            a.thumbnail = makeThumbnail(a);
            try {
                // Append to the catalog and to the list model together, so that the two
                // never disagree about what has been loaded.
                synchronized (catalog) {
                    if (generation != scanGeneration) {
                        return;
                    }
                    catalog.add(a);
                    SwingUtilities.invokeAndWait(() -> listModel.addElement(a));
                }
            } catch (InterruptedException e) {
                return;
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e.getCause());
            }
            progress.setValue(i + 1);
        }
        progress.setString("Done");
    }

    /** Renders at double resolution and scales down, which gives smoother thumbnails. */
    private static BufferedImage makeThumbnail(Artwork a) {
        BufferedImage src = render(a, 2 * THUMB);
        BufferedImage dst = new BufferedImage(THUMB, THUMB, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, THUMB, THUMB, null);
        g.dispose();
        return dst;
    }

    /** Procedural artwork: a sum of sine waves mapped through a seeded palette. */
    static BufferedImage render(Artwork a, int px) {
        Random r = new Random(a.seed);
        int waves = 24 + r.nextInt(16);
        double[] fx = new double[waves];
        double[] fy = new double[waves];
        double[] ph = new double[waves];
        for (int k = 0; k < waves; k++) {
            fx[k] = (r.nextDouble() * 2 - 1) * 12;
            fy[k] = (r.nextDouble() * 2 - 1) * 12;
            ph[k] = r.nextDouble() * Math.PI * 2;
        }
        float hue = r.nextFloat();
        float hue2 = hue + 0.3f + r.nextFloat() * 0.4f;
        int[] palette = new int[256];
        for (int i = 0; i < 256; i++) {
            float t = i / 255f;
            palette[i] = Color.HSBtoRGB(hue + (hue2 - hue) * t, 0.55f + 0.4f * t,
                    0.35f + 0.6f * (float) Math.sin(t * Math.PI));
        }
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[px];
        for (int y = 0; y < px; y++) {
            double v = (double) y / px;
            for (int x = 0; x < px; x++) {
                double u = (double) x / px;
                double s = 0;
                for (int k = 0; k < waves; k++) {
                    s += Math.sin(fx[k] * u + fy[k] * v + ph[k]);
                }
                int idx = (int) ((s / waves + 1) * 127.5);
                row[x] = palette[Math.max(0, Math.min(255, idx))];
            }
            img.setRGB(0, y, px, 1, row, 0, px);
        }
        return img;
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Stops background work and closes the window.
     * Must be called on the EDT.
     */
    public void dispose() {
        shutdown();
        frame.dispose();
    }

    private void shutdown() {
        statusTimer.stop();
        cancelScan();
        hideInfo();
    }

    /**
     * Starts the application with the given artwork count, defaulting to 400.
     *
     * @param args optional artwork count as the first argument
     * @throws Exception if application startup fails
     */
    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 400;
        SwingUtilities.invokeAndWait(() -> {
            Gallery gallery = new Gallery(n);
            gallery.getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            gallery.getFrame().setVisible(true);
            gallery.rescan();
        });
    }

    // ---------------------------------------------------------------- components

    private static final class ThumbnailRenderer extends JPanel implements ListCellRenderer<Artwork> {
        private Artwork art;

        ThumbnailRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Artwork> l, Artwork value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            art = value;
            setBackground(isSelected ? l.getSelectionBackground() : l.getBackground());
            setForeground(isSelected ? l.getSelectionForeground() : l.getForeground());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (art == null) {
                return;
            }
            int x = (getWidth() - THUMB) / 2;
            BufferedImage t = art.thumbnail;
            if (t != null) {
                g.drawImage(t, x, 8, null);
            }
            g.setColor(getForeground());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(art.name, (getWidth() - fm.stringWidth(art.name)) / 2, 8 + THUMB + fm.getAscent() + 2);
        }
    }

    private static final class PreviewPane extends JComponent {
        private BufferedImage image;

        void setImage(BufferedImage img) {
            image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (image == null) {
                g.setColor(Color.LIGHT_GRAY);
                String s = "Select an artwork";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
                return;
            }
            int w = getWidth() - 16;
            int h = getHeight() - 16;
            double scale = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
            int dw = (int) (image.getWidth() * scale);
            int dh = (int) (image.getHeight() * scale);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);
        }
    }
}
