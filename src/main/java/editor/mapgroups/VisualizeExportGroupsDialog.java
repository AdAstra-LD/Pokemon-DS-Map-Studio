package editor.mapgroups;

import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.mapdisplay.MapDisplay;
import editor.mapmatrix.PointComparator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author AdAstra
 */
public class VisualizeExportGroupsDialog extends JDialog {

    public static final int APPROVE_OPTION = 1, CANCEL_OPTION = 0;
    private int returnValue = CANCEL_OPTION;

    private MapEditorHandler handler;
    private boolean updatingSelection;
    private final DefaultListModel<ExportGroupItem> exportGroupsModel = new DefaultListModel<>();
    private final JList<ExportGroupItem> exportGroupsList = new JList<>(exportGroupsModel);
    private final DefaultListModel<CoordinateItem> coordinateModel = new DefaultListModel<>();
    private final JList<CoordinateItem> coordinateList = new JList<>(coordinateModel);
    private final GroupMatrixPanel matrixPanel = new GroupMatrixPanel();
    private final JScrollPane matrixScrollPane = new JScrollPane(matrixPanel);
    private final JLabel groupInfoLabel = new JLabel(" ");
    private final JButton closeButton = new JButton("Close");

    public VisualizeExportGroupsDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Export Groups");
        setModal(true);
        setLayout(new BorderLayout(8, 8));

        exportGroupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        exportGroupsList.setVisibleRowCount(20);
        exportGroupsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updatingSelection) {
                updateSelectedGroup();
            }
        });

        JScrollPane groupsScrollPane = new JScrollPane(exportGroupsList);
        groupsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        groupsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupsPanel.setBorder(BorderFactory.createTitledBorder("Export Groups"));
        groupsPanel.add(groupsScrollPane, BorderLayout.CENTER);
        groupsPanel.setPreferredSize(new Dimension(280, 480));

        matrixScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        matrixScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        matrixScrollPane.getViewport().setBackground(new Color(48, 48, 48));

        coordinateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        coordinateList.setVisibleRowCount(5);
        coordinateList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updatingSelection) {
                CoordinateItem selectedCoordinate = coordinateList.getSelectedValue();
                if (selectedCoordinate != null) {
                    navigateToMap(selectedCoordinate.getCoords());
                    matrixPanel.repaint();
                }
            }
        });

        JScrollPane coordinateScrollPane = new JScrollPane(coordinateList);
        coordinateScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        coordinateScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        coordinateScrollPane.setPreferredSize(new Dimension(1, 120));
        JPanel coordinatesPanel = new JPanel(new BorderLayout());
        coordinatesPanel.setBorder(BorderFactory.createTitledBorder("Map Coordinates"));
        coordinatesPanel.add(coordinateScrollPane, BorderLayout.CENTER);

        JPanel matrixPanelContainer = new JPanel(new BorderLayout(0, 6));
        matrixPanelContainer.add(groupInfoLabel, BorderLayout.NORTH);
        matrixPanelContainer.add(matrixScrollPane, BorderLayout.CENTER);
        matrixPanelContainer.add(coordinatesPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groupsPanel, matrixPanelContainer);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerLocation(280);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeButton.addActionListener(e -> closeDialog());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(closeButton);

        setPreferredSize(new Dimension(940, 620));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void closeDialog() {
        returnValue = CANCEL_OPTION;
        dispose();
    }

    public void init(MapEditorHandler handler) {
        this.handler = handler;
        this.matrixPanel.init(handler);
        loadExportGroups();
    }

    private void loadExportGroups() {
        exportGroupsModel.clear();
        coordinateModel.clear();

        TreeMap<Integer, MapGroup> exportGroups = handler.getMapMatrix().getExportGroups();
        for (MapGroup group : exportGroups.values()) {
            if (group.getIndex() != 0) {
                exportGroupsModel.addElement(new ExportGroupItem(group));
            }
        }

        if (exportGroupsModel.isEmpty()) {
            exportGroupsList.setEnabled(false);
            coordinateList.setEnabled(false);
            matrixPanel.clearGroup();
            groupInfoLabel.setText("No export groups have been assigned.");
            return;
        }

        exportGroupsList.setEnabled(true);
        coordinateList.setEnabled(true);
        if (!selectCurrentMapGroup()) {
            exportGroupsList.setSelectedIndex(0);
        }
    }

    private boolean selectCurrentMapGroup() {
        int currentGroupIndex = handler.getCurrentMap().getExportGroupIndex();
        if (currentGroupIndex == 0) {
            return false;
        }
        for (int i = 0; i < exportGroupsModel.getSize(); i++) {
            ExportGroupItem item = exportGroupsModel.getElementAt(i);
            if (item.getIndex() == currentGroupIndex) {
                exportGroupsList.setSelectedIndex(i);
                exportGroupsList.ensureIndexIsVisible(i);
                return true;
            }
        }
        return false;
    }

    private void updateSelectedGroup() {
        ExportGroupItem selectedGroup = exportGroupsList.getSelectedValue();
        if (selectedGroup == null || handler == null) {
            matrixPanel.clearGroup();
            coordinateModel.clear();
            return;
        }

        matrixPanel.setGroup(selectedGroup.getIndex());
        matrixScrollPane.getViewport().setViewPosition(new Point(0, 0));
        loadCoordinateList(selectedGroup);
        groupInfoLabel.setText(getGroupInfoText(selectedGroup));
    }

    private void loadCoordinateList(ExportGroupItem group) {
        updatingSelection = true;
        try {
            coordinateModel.clear();
            HashMap<Point, MapData> maps = handler.getMapMatrix().getMatrix();
            for (Point coords : group.getCoordList()) {
                MapData mapData = maps.get(coords);
                boolean isCenter = mapData != null && mapData.isExportGroupCenter();
                coordinateModel.addElement(new CoordinateItem(coords, isCenter));
            }
            syncCoordinateSelection(handler.getMapSelected());
        } finally {
            updatingSelection = false;
        }
    }

    private void syncCoordinateSelection(Point mapCoords) {
        for (int i = 0; i < coordinateModel.getSize(); i++) {
            CoordinateItem item = coordinateModel.getElementAt(i);
            if (item.getCoords().equals(mapCoords)) {
                coordinateList.setSelectedIndex(i);
                coordinateList.ensureIndexIsVisible(i);
                return;
            }
        }
        coordinateList.clearSelection();
    }

    private void navigateToMap(Point mapCoords) {
        if (handler == null) {
            return;
        }
        handler.setMapSelected(mapCoords, false);
        handler.getMainFrame().getMapDisplay().setCameraAtMap(mapCoords);
        handler.getMainFrame().getMapDisplay().repaint();
        handler.getMainFrame().getMapMatrixDisplay().repaint();
    }

    private String getGroupInfoText(ExportGroupItem group) {
        Point center = handler.getMapMatrix().getExportGroupCenterCoords(group.getIndex());
        String centerText = center == null
                ? "center not set; export uses the lowest cell"
                : "center (" + center.x + ", " + center.y + ")";
        return "Group " + group.getIndex() + " - " + group.getMapCount() + " maps - " + centerText;
    }

    public int getReturnValue() {
        return returnValue;
    }

    private static final class ExportGroupItem {
        private final MapGroup group;

        private ExportGroupItem(MapGroup group) {
            this.group = group;
        }

        int getIndex() {
            return group.getIndex();
        }

        int getMapCount() {
            return group.getCoordList().size();
        }

        TreeSet<Point> getCoordList() {
            return group.getCoordList();
        }

        @Override
        public String toString() {
            return "Group " + getIndex() + " - " + getMapCount() + " "
                    + (getMapCount() == 1 ? "map" : "maps") + ": "
                    + formatCoordinates(group.getCoordList());
        }

        private static String formatCoordinates(TreeSet<Point> coordList) {
            StringBuilder coordinates = new StringBuilder();
            for (Point point : coordList) {
                if (coordinates.length() > 0) {
                    coordinates.append("; ");
                }
                coordinates.append("(").append(point.x).append(", ").append(point.y).append(")");
            }
            return coordinates.toString();
        }
    }

    private static final class CoordinateItem {
        private final Point coords;
        private final boolean center;

        private CoordinateItem(Point coords, boolean center) {
            this.coords = new Point(coords);
            this.center = center;
        }

        Point getCoords() {
            return new Point(coords);
        }

        @Override
        public String toString() {
            return "(" + coords.x + ", " + coords.y + ")" + (center ? "  center" : "");
        }
    }

    private final class GroupMatrixPanel extends JPanel {
        private final float scale = 2.0f;
        private final int tileSize = MapData.mapThumbnailSize;
        private final Color backgroundColor = new Color(46, 46, 46);
        private final Color emptyCellColor = new Color(58, 58, 58);
        private final Color gridColor = new Color(80, 80, 80);
        private final Color selectedFillColor = new Color(255, 0, 0, 50);
        private final Color centerColor = new Color(255, 210, 40);

        private MapEditorHandler handler;
        private final TreeSet<Point> groupCoords = new TreeSet<>(new PointComparator());
        private final HashMap<Point, BufferedImage> previewCache = new HashMap<>();
        private Point matrixMin = new Point(0, 0);
        private Dimension matrixSize = new Dimension(1, 1);
        private int groupIndex = 0;

        private GroupMatrixPanel() {
            setBackground(backgroundColor);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectMapAt(e.getPoint());
                }
            });
        }

        void init(MapEditorHandler handler) {
            this.handler = handler;
        }

        void setGroup(int groupIndex) {
            this.groupIndex = groupIndex;
            groupCoords.clear();
            for (Map.Entry<Point, MapData> mapEntry : handler.getMapMatrix().getMatrix().entrySet()) {
                if (mapEntry.getValue().getExportGroupIndex() == groupIndex) {
                    groupCoords.add(mapEntry.getKey());
                }
            }
            updateBounds();
            updatePreferredSize();
            refreshPreviewCache();
            revalidate();
            repaint();
        }

        void clearGroup() {
            groupIndex = 0;
            groupCoords.clear();
            previewCache.clear();
            matrixMin = new Point(0, 0);
            matrixSize = new Dimension(1, 1);
            updatePreferredSize();
            revalidate();
            repaint();
        }

        private void updateBounds() {
            if (groupCoords.isEmpty()) {
                matrixMin = new Point(0, 0);
                matrixSize = new Dimension(1, 1);
                return;
            }

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Point point : groupCoords) {
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
            for (Point coords : groupCoords) {
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

        private void selectMapAt(Point clickedPoint) {
            if (handler == null || groupCoords.isEmpty()) {
                return;
            }

            int cellSize = Math.max(1, Math.round(tileSize * scale));
            int mapX = Math.floorDiv(clickedPoint.x, cellSize) + matrixMin.x;
            int mapY = Math.floorDiv(clickedPoint.y, cellSize) + matrixMin.y;
            Point mapCoords = new Point(mapX, mapY);
            if (!groupCoords.contains(mapCoords)) {
                return;
            }

            navigateToMap(mapCoords);
            updatingSelection = true;
            try {
                syncCoordinateSelection(mapCoords);
            } finally {
                updatingSelection = false;
            }
            repaint();
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
            drawGroupMaps(g2d);
            drawSelectedMap(g2d);
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

        private void drawGroupMaps(Graphics2D g2d) {
            if (handler == null) {
                return;
            }
            HashMap<Point, MapData> maps = handler.getMapMatrix().getMatrix();
            for (Point coords : groupCoords) {
                MapData mapData = maps.get(coords);
                if (mapData == null) {
                    continue;
                }
                int x = (coords.x - matrixMin.x) * tileSize;
                int y = (coords.y - matrixMin.y) * tileSize;
                BufferedImage preview = getMapPreview(coords, mapData);
                if (preview != null) {
                    g2d.drawImage(preview, x, y, tileSize, tileSize, null);
                }
                drawAreaOverlay(g2d, mapData, x, y);
                drawCellBorder(g2d, x, y, Color.WHITE, 1);
                if (mapData.isExportGroupCenter()) {
                    drawCellBorder(g2d, x + 3, y + 3, centerColor, 4);
                }
            }
        }

        private BufferedImage getMapPreview(Point coords, MapData mapData) {
            BufferedImage preview = previewCache.get(coords);
            return preview != null ? preview : mapData.getMapThumbnail();
        }

        private void drawAreaOverlay(Graphics2D g2d, MapData mapData, int x, int y) {
            Color areaColor = handler.getMapMatrix().getAreaColors().get(mapData.getAreaIndex());
            if (areaColor == null) {
                return;
            }
            g2d.setColor(new Color(areaColor.getRed(), areaColor.getGreen(), areaColor.getBlue(), 50));
            g2d.fillRect(x, y, tileSize - 1, tileSize - 1);
        }

        private void drawSelectedMap(Graphics2D g2d) {
            if (handler == null) {
                return;
            }
            Point selectedMap = handler.getMapSelected();
            if (!groupCoords.contains(selectedMap)) {
                return;
            }
            int x = (selectedMap.x - matrixMin.x) * tileSize;
            int y = (selectedMap.y - matrixMin.y) * tileSize;
            g2d.setColor(selectedFillColor);
            g2d.fillRect(x, y, tileSize - 1, tileSize - 1);
            drawCellBorder(g2d, x, y, Color.WHITE, 4);
            drawCellBorder(g2d, x - 3, y - 3, Color.RED, 4);
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
