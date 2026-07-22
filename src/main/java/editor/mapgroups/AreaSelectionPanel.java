package editor.mapgroups;

import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.mapdisplay.MapDisplay;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import utils.swing.JCheckboxList;

/**
 * Visual area picker shared by the export dialogs: a checkbox list of areas
 * next to a rendered map matrix where clicking a map toggles its whole area.
 * The matrix auto-fits the viewport and supports Ctrl+wheel zooming and
 * middle-button panning.
 *
 * @author AdAstra
 */
public class AreaSelectionPanel extends JPanel {

    private MapEditorHandler handler;
    private final DefaultListModel<JCheckBox> areaModel = new DefaultListModel<>();
    private final JCheckboxList areaList = new JCheckboxList(areaModel);
    private final ArrayList<Integer> areaIndices = new ArrayList<>();
    private final AreaMatrixPanel matrixPanel = new AreaMatrixPanel();
    private final JScrollPane matrixScrollPane = new JScrollPane(matrixPanel);
    private final JLabel infoLabel = new JLabel(" ");

    public AreaSelectionPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        areaList.setVisibleRowCount(20);
        areaList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateHighlight();
            }
        });
        //JCheckboxList toggles the checkbox on mousePressed; refresh afterwards
        areaList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(AreaSelectionPanel.this::selectionChanged);
            }
        });

        JScrollPane areaScrollPane = new JScrollPane(areaList);
        areaScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        areaScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> setAllSelected(true));
        JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> setAllSelected(false));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonRow.add(selectAllButton);
        buttonRow.add(deselectAllButton);

        JPanel areasPanel = new JPanel(new BorderLayout(0, 6));
        areasPanel.setBorder(BorderFactory.createTitledBorder("Areas"));
        areasPanel.add(areaScrollPane, BorderLayout.CENTER);
        areasPanel.add(buttonRow, BorderLayout.SOUTH);
        areasPanel.setPreferredSize(new Dimension(240, 420));

        matrixScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        matrixScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        //The matrix panel is not Scrollable, so without this the wheel
        //scrolls by a barely noticeable few pixels per tick
        matrixScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        matrixScrollPane.getHorizontalScrollBar().setUnitIncrement(12);
        matrixScrollPane.getViewport().setBackground(new Color(48, 48, 48));
        matrixScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                matrixPanel.fitToViewport();
            }
        });
        matrixPanel.setToolTipText("Click a map to toggle its area. "
                + "Ctrl + mouse wheel zooms, middle mouse button pans.");

        JPanel matrixContainer = new JPanel(new BorderLayout(0, 6));
        matrixContainer.setBorder(BorderFactory.createTitledBorder("Map Matrix"));
        matrixContainer.add(infoLabel, BorderLayout.NORTH);
        matrixContainer.add(matrixScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, areasPanel, matrixContainer);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(240);
        add(splitPane, BorderLayout.CENTER);
    }

    public void init(MapEditorHandler handler) {
        this.handler = handler;

        areaIndices.clear();
        areaModel.clear();
        TreeMap<Integer, MapGroup> areas = handler.getMapMatrix().getAreas();
        for (MapGroup area : areas.values()) {
            areaIndices.add(area.getIndex());
            int mapCount = area.getCoordList().size();
            JCheckBox checkBox = new JCheckBox("Area " + area.getIndex() + " - " + mapCount
                    + (mapCount == 1 ? " map" : " maps"));
            checkBox.setSelected(true);
            areaModel.addElement(checkBox);
        }

        matrixPanel.init(handler);
        if (!areaModel.isEmpty()) {
            areaList.setSelectedIndex(0);
        }
        selectionChanged();
    }

    public ArrayList<Integer> getSelectedAreaIndices() {
        ArrayList<Integer> selected = new ArrayList<>();
        for (int i = 0; i < areaModel.getSize(); i++) {
            if (areaModel.get(i).isSelected()) {
                selected.add(areaIndices.get(i));
            }
        }
        return selected;
    }

    private void setAllSelected(boolean selected) {
        for (int i = 0; i < areaModel.getSize(); i++) {
            areaModel.get(i).setSelected(selected);
        }
        areaList.repaint();
        selectionChanged();
    }

    private void selectionChanged() {
        matrixPanel.setSelectedAreas(new HashSet<>(getSelectedAreaIndices()));
        updateHighlight();
    }

    private void updateHighlight() {
        int listIndex = areaList.getSelectedIndex();
        int areaIndex = (listIndex >= 0 && listIndex < areaIndices.size()) ? areaIndices.get(listIndex) : -1;
        matrixPanel.setHighlightedArea(areaIndex);
        updateInfoLabel(areaIndex);
    }

    private void updateInfoLabel(int highlightedArea) {
        if (handler == null || areaModel.isEmpty()) {
            infoLabel.setText("No areas found in this map.");
            return;
        }
        ArrayList<Integer> selected = getSelectedAreaIndices();
        int includedMaps = 0;
        TreeMap<Integer, MapGroup> areas = handler.getMapMatrix().getAreas();
        for (Integer areaIndex : selected) {
            MapGroup area = areas.get(areaIndex);
            if (area != null) {
                includedMaps += area.getCoordList().size();
            }
        }
        String text = selected.size() + " of " + areaModel.getSize() + " areas selected - "
                + includedMaps + (includedMaps == 1 ? " map" : " maps") + " included";
        if (highlightedArea >= 0) {
            text += "  |  viewing Area " + highlightedArea;
        }
        infoLabel.setText(text);
    }

    private void toggleAreaFromMatrix(int areaIndex) {
        int position = areaIndices.indexOf(areaIndex);
        if (position < 0) {
            return;
        }
        JCheckBox checkBox = areaModel.get(position);
        checkBox.setSelected(!checkBox.isSelected());
        areaList.setSelectedIndex(position);
        areaList.ensureIndexIsVisible(position);
        areaList.repaint();
        selectionChanged();
    }

    private final class AreaMatrixPanel extends JPanel {
        //The minimum cell size matches the MainFrame matrix view
        //(mapThumbnailSize * 0.5); auto-fit never zooms in past FIT_MAX_SCALE
        //while Ctrl+wheel can go up to USER_MAX_SCALE
        private static final float MIN_SCALE = 0.5f;
        private static final float FIT_MAX_SCALE = 2.0f;
        private static final float USER_MAX_SCALE = 4.0f;

        private final int tileSize = MapData.mapThumbnailSize;
        private final Color backgroundColor = new Color(46, 46, 46);
        private final Color emptyCellColor = new Color(58, 58, 58);
        private final Color gridColor = new Color(80, 80, 80);
        private final Color excludedOverlayColor = new Color(0, 0, 0, 150);
        private final Color highlightColor = new Color(255, 210, 40);

        private MapEditorHandler handler;
        private final HashMap<Point, BufferedImage> previewCache = new HashMap<>();
        private HashSet<Integer> selectedAreas = new HashSet<>();
        private int highlightedArea = -1;
        private Point matrixMin = new Point(0, 0);
        private Dimension matrixSize = new Dimension(1, 1);

        private float scale = FIT_MAX_SCALE;
        private boolean userZoomed = false;
        private Point panStartScreen;
        private Point panStartViewPosition;

        private AreaMatrixPanel() {
            setBackground(backgroundColor);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        startPan(e);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        toggleAreaAt(e.getPoint());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        stopPan();
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (panStartScreen != null) {
                        pan(e);
                    }
                }
            });
            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    zoomAt(e.getPoint(), e.getPreciseWheelRotation());
                } else {
                    //Let the scroll pane handle plain wheel scrolling
                    matrixScrollPane.dispatchEvent(
                            SwingUtilities.convertMouseEvent(this, e, matrixScrollPane));
                }
            });
        }

        void init(MapEditorHandler handler) {
            this.handler = handler;
            userZoomed = false;
            updateBounds();
            refreshPreviewCache();
            fitToViewport();
            updatePreferredSize();
            revalidate();
            repaint();
        }

        void setSelectedAreas(HashSet<Integer> selectedAreas) {
            this.selectedAreas = selectedAreas;
            repaint();
        }

        void setHighlightedArea(int areaIndex) {
            this.highlightedArea = areaIndex;
            repaint();
        }

        private JViewport getViewport() {
            return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        }

        /**
         * Picks the largest scale that shows the whole matrix in the viewport,
         * clamped so cells never get smaller than in the MainFrame matrix
         * view. Disabled once the user zooms manually.
         */
        void fitToViewport() {
            if (userZoomed) {
                return;
            }
            JViewport viewport = getViewport();
            if (viewport == null) {
                return;
            }
            Dimension extent = viewport.getExtentSize();
            if (extent.width <= 0 || extent.height <= 0) {
                return;
            }
            float scaleX = (float) extent.width / (matrixSize.width * tileSize);
            float scaleY = (float) extent.height / (matrixSize.height * tileSize);
            float fitScale = Math.min(FIT_MAX_SCALE, Math.max(MIN_SCALE, Math.min(scaleX, scaleY)));
            if (Math.abs(fitScale - scale) > 0.01f) {
                scale = fitScale;
                updatePreferredSize();
                revalidate();
                repaint();
            }
        }

        private void zoomAt(Point mousePoint, double wheelRotation) {
            JViewport viewport = getViewport();
            if (viewport == null) {
                return;
            }
            float oldScale = scale;
            float newScale = (float) (scale * Math.pow(1.1, -wheelRotation));
            newScale = Math.min(USER_MAX_SCALE, Math.max(MIN_SCALE, newScale));
            if (newScale == oldScale) {
                return;
            }
            userZoomed = true;
            scale = newScale;
            updatePreferredSize();

            //Resize and reposition synchronously with blitting disabled, so
            //the zoom paints exactly once instead of first blitting the
            //old-scale pixels around and repositioning a frame later
            int previousScrollMode = viewport.getScrollMode();
            viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            try {
                setSize(getPreferredSize());

                //Keep the map under the cursor in place while zooming
                float factor = newScale / oldScale;
                Point viewPosition = viewport.getViewPosition();
                int cursorInViewportX = mousePoint.x - viewPosition.x;
                int cursorInViewportY = mousePoint.y - viewPosition.y;
                Point newViewPosition = new Point(
                        Math.round(mousePoint.x * factor) - cursorInViewportX,
                        Math.round(mousePoint.y * factor) - cursorInViewportY);
                viewport.setViewPosition(clampViewPosition(viewport, newViewPosition));
            } finally {
                viewport.setScrollMode(previousScrollMode);
            }
            revalidate();
            repaint();
        }

        private void startPan(MouseEvent e) {
            JViewport viewport = getViewport();
            if (viewport == null) {
                return;
            }
            panStartScreen = e.getLocationOnScreen();
            panStartViewPosition = viewport.getViewPosition();
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        private void pan(MouseEvent e) {
            JViewport viewport = getViewport();
            if (viewport == null) {
                return;
            }
            Point currentScreen = e.getLocationOnScreen();
            Point newViewPosition = new Point(
                    panStartViewPosition.x - (currentScreen.x - panStartScreen.x),
                    panStartViewPosition.y - (currentScreen.y - panStartScreen.y));
            viewport.setViewPosition(clampViewPosition(viewport, newViewPosition));
        }

        private void stopPan() {
            panStartScreen = null;
            panStartViewPosition = null;
            setCursor(Cursor.getDefaultCursor());
        }

        private Point clampViewPosition(JViewport viewport, Point position) {
            Dimension extent = viewport.getExtentSize();
            Dimension viewSize = getPreferredSize();
            int maxX = Math.max(0, viewSize.width - extent.width);
            int maxY = Math.max(0, viewSize.height - extent.height);
            return new Point(
                    Math.max(0, Math.min(position.x, maxX)),
                    Math.max(0, Math.min(position.y, maxY)));
        }

        private void updateBounds() {
            HashMap<Point, MapData> maps = handler.getMapMatrix().getMatrix();
            if (maps.isEmpty()) {
                matrixMin = new Point(0, 0);
                matrixSize = new Dimension(1, 1);
                return;
            }
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Point point : maps.keySet()) {
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }
            matrixMin = new Point(minX, minY);
            matrixSize = new Dimension(maxX - minX + 1, maxY - minY + 1);
        }

        private void updatePreferredSize() {
            int width = Math.max(1, Math.round(matrixSize.width * tileSize * scale));
            int height = Math.max(1, Math.round(matrixSize.height * tileSize * scale));
            setPreferredSize(new Dimension(width, height));
        }

        private void refreshPreviewCache() {
            previewCache.clear();
            if (handler == null) {
                return;
            }
            MapDisplay mapDisplay = handler.getMainFrame().getMapDisplay();
            for (Point coords : handler.getMapMatrix().getMatrix().keySet()) {
                try {
                    BufferedImage preview = mapDisplay.captureOrthoMapPreview(coords);
                    if (preview != null) {
                        previewCache.put(new Point(coords), preview);
                    }
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void toggleAreaAt(Point clickedPoint) {
            if (handler == null) {
                return;
            }
            int mapX = (int) Math.floor(clickedPoint.x / (tileSize * scale)) + matrixMin.x;
            int mapY = (int) Math.floor(clickedPoint.y / (tileSize * scale)) + matrixMin.y;
            MapData mapData = handler.getMapMatrix().getMatrix().get(new Point(mapX, mapY));
            if (mapData == null) {
                return;
            }
            toggleAreaFromMatrix(mapData.getAreaIndex());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.scale(scale, scale);

            int width = matrixSize.width * tileSize;
            int height = matrixSize.height * tileSize;
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, width, height);

            drawEmptyCells(g2d);
            drawMaps(g2d);
            drawHighlightedArea(g2d);
            g2d.dispose();
        }

        private void drawEmptyCells(Graphics2D g2d) {
            g2d.setColor(emptyCellColor);
            for (int x = 0; x < matrixSize.width; x++) {
                for (int y = 0; y < matrixSize.height; y++) {
                    g2d.fillRect(x * tileSize, y * tileSize, tileSize - 1, tileSize - 1);
                }
            }
            g2d.setColor(gridColor);
            for (int x = 0; x <= matrixSize.width; x++) {
                g2d.drawLine(x * tileSize, 0, x * tileSize, matrixSize.height * tileSize);
            }
            for (int y = 0; y <= matrixSize.height; y++) {
                g2d.drawLine(0, y * tileSize, matrixSize.width * tileSize, y * tileSize);
            }
        }

        private void drawMaps(Graphics2D g2d) {
            if (handler == null) {
                return;
            }
            HashMap<Integer, Color> areaColors = handler.getMapMatrix().getAreaColors();
            for (Map.Entry<Point, MapData> mapEntry : handler.getMapMatrix().getMatrix().entrySet()) {
                Point coords = mapEntry.getKey();
                MapData mapData = mapEntry.getValue();
                int x = (coords.x - matrixMin.x) * tileSize;
                int y = (coords.y - matrixMin.y) * tileSize;

                BufferedImage preview = previewCache.get(coords);
                if (preview == null) {
                    preview = mapData.getMapThumbnail();
                }
                if (preview != null) {
                    g2d.drawImage(preview, x, y, tileSize, tileSize, null);
                }

                int areaIndex = mapData.getAreaIndex();
                Color areaColor = areaColors.get(areaIndex);
                if (selectedAreas.contains(areaIndex)) {
                    if (areaColor != null) {
                        g2d.setColor(new Color(areaColor.getRed(), areaColor.getGreen(), areaColor.getBlue(), 90));
                        g2d.fillRect(x, y, tileSize - 1, tileSize - 1);
                    }
                    drawCellBorder(g2d, x, y, Color.WHITE, 1);
                } else {
                    g2d.setColor(excludedOverlayColor);
                    g2d.fillRect(x, y, tileSize - 1, tileSize - 1);
                }
                drawAreaLabel(g2d, areaIndex, x, y);
            }
        }

        private void drawAreaLabel(Graphics2D g2d, int areaIndex, int x, int y) {
            //Keep the label roughly the same size on screen at every zoom
            float fontSize = 9f / scale;
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, fontSize));
            String label = String.valueOf(areaIndex);
            float baselineX = x + 2f;
            float baselineY = y + 2f + fontSize;
            float shadowOffset = Math.max(0.5f, 1f / scale);
            g2d.setColor(Color.BLACK);
            g2d.drawString(label, baselineX + shadowOffset, baselineY + shadowOffset);
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, baselineX, baselineY);
        }

        private void drawHighlightedArea(Graphics2D g2d) {
            if (handler == null || highlightedArea < 0) {
                return;
            }
            for (Map.Entry<Point, MapData> mapEntry : handler.getMapMatrix().getMatrix().entrySet()) {
                if (mapEntry.getValue().getAreaIndex() == highlightedArea) {
                    int x = (mapEntry.getKey().x - matrixMin.x) * tileSize;
                    int y = (mapEntry.getKey().y - matrixMin.y) * tileSize;
                    drawCellBorder(g2d, x + 1, y + 1, highlightColor, 3);
                }
            }
        }

        private void drawCellBorder(Graphics2D g2d, int x, int y, Color color, int strokeWidth) {
            g2d.setColor(color);
            java.awt.Stroke previousStroke = g2d.getStroke();
            g2d.setStroke(new java.awt.BasicStroke(strokeWidth));
            g2d.drawRect(x, y, tileSize - 1, tileSize - 1);
            g2d.setStroke(previousStroke);
        }
    }
}
