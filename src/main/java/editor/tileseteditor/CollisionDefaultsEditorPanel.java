package editor.tileseteditor;

import editor.handler.MapEditorHandler;
import editor.tileselector.CollisionDefaultsClipboard;
import formats.collisions.CollisionTypes;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import tileset.Tile;

/** Persistent collision-default editor used by the Tileset Editor. */
public class CollisionDefaultsEditorPanel extends JPanel {

    private MapEditorHandler handler;
    private CollisionTypes collisionTypes;
    private Runnable changeListener;
    private int currentTileIndex = -1;
    private int currentWidth = -1;
    private int currentHeight = -1;

    private final JLabel tileLabel = new JLabel("No tile selected");
    private final JPanel layersPanel = new JPanel();
    private final JScrollPane layersScroll = new JScrollPane(layersPanel);
    private final JButton pasteAllButton = new JButton("Paste All");
    private final ArrayList<JButton> pasteLayerButtons = new ArrayList<>();

    private final JSpinner widthSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 32, 1));
    private final JSpinner heightSpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, 32, 1));
    private final JSpinner anchorXSpinner = new JSpinner(
            new SpinnerNumberModel(0, 0, 0, 1));
    private final JSpinner anchorYSpinner = new JSpinner(
            new SpinnerNumberModel(0, 0, 0, 1));
    private final JCheckBox followDisplaySize = new JCheckBox("Follow folder display size");
    private boolean updatingFootprint;

    public CollisionDefaultsEditorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        tileLabel.setFont(tileLabel.getFont().deriveFont(java.awt.Font.BOLD));
        titleRow.add(tileLabel, BorderLayout.WEST);

        JButton copyAllButton = new JButton("Copy All");
        JButton clearAllButton = new JButton("Clear All");
        copyAllButton.addActionListener(e -> copyAll());
        pasteAllButton.addActionListener(e -> pasteAll());
        clearAllButton.addActionListener(e -> clearAll());
        JPanel allButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        allButtons.add(copyAllButton);
        allButtons.add(pasteAllButton);
        allButtons.add(clearAllButton);
        titleRow.add(allButtons, BorderLayout.EAST);

        JPanel footprint = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        footprint.setBorder(BorderFactory.createTitledBorder("Collision Footprint"));
        footprint.add(followDisplaySize);
        footprint.add(new JLabel("Width:"));
        footprint.add(widthSpinner);
        footprint.add(new JLabel("Height:"));
        footprint.add(heightSpinner);
        footprint.add(new JLabel("Anchor X:"));
        footprint.add(anchorXSpinner);
        footprint.add(new JLabel("Anchor Y:"));
        footprint.add(anchorYSpinner);
        JButton applyFootprint = new JButton("Apply");
        applyFootprint.addActionListener(e -> applyFootprint());
        footprint.add(applyFootprint);

        followDisplaySize.addActionListener(e -> updateFootprintControlState());
        widthSpinner.addChangeListener(e -> updateAnchorMaximum(anchorXSpinner,
                (Integer) widthSpinner.getValue() - 1));
        heightSpinner.addChangeListener(e -> updateAnchorMaximum(anchorYSpinner,
                (Integer) heightSpinner.getValue() - 1));

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(titleRow, BorderLayout.NORTH);
        north.add(footprint, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        layersScroll.setBorder(null);
        layersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        layersScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        layersScroll.getVerticalScrollBar().setUnitIncrement(16);
        layersScroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(layersScroll, BorderLayout.CENTER);
        updateClipboardButtons();
    }

    public void init(MapEditorHandler handler, Runnable changeListener) {
        this.handler = handler;
        this.changeListener = changeListener;
        collisionTypes = new CollisionTypes(handler.getGameIndex());
        refreshSelectedTile(true);
    }

    public void refreshSelectedTile() {
        refreshSelectedTile(false);
    }

    private void refreshSelectedTile(boolean force) {
        if (handler == null || handler.getTileset().size() == 0) {
            currentTileIndex = -1;
            tileLabel.setText("No tile selected");
            layersPanel.removeAll();
            layersPanel.revalidate();
            layersPanel.repaint();
            return;
        }
        Tile tile = handler.getTileSelected();
        int index = handler.getTileIndexSelected();
        int width = tile.getCollisionFootprintWidth();
        int height = tile.getCollisionFootprintHeight();
        tileLabel.setText("Tile " + index + "  " + tile.getObjFilename());
        updateFootprintControls(tile);
        if (force || index != currentTileIndex || width != currentWidth || height != currentHeight) {
            currentTileIndex = index;
            currentWidth = width;
            currentHeight = height;
            rebuildLayers();
        } else {
            layersPanel.repaint();
        }
        updateClipboardButtons();
    }

    private void rebuildLayers() {
        layersPanel.removeAll();
        pasteLayerButtons.clear();
        if (currentTileIndex < 0) {
            return;
        }
        int numLayers = CollisionTypes.numLayersPerGame[handler.getGameIndex()];
        layersPanel.setLayout(new GridLayout(0, Math.min(2, numLayers), 8, 8));
        for (int layer = 0; layer < numLayers; layer++) {
            layersPanel.add(createLayerPanel(layer));
        }
        layersPanel.revalidate();
        layersPanel.repaint();
    }

    private JPanel createLayerPanel(int layer) {
        Tile tile = handler.getTileSelected();
        JComboBox<String> collisionCombo = new JComboBox<>();
        collisionCombo.addItem("No Default (Erase)");
        for (int value = 0; value < CollisionTypes.numCollisions; value++) {
            String name = collisionTypes.getCollisionName(layer, value);
            collisionCombo.addItem(String.format("%02X", value)
                    + (name == null || name.isEmpty() ? "" : "  " + name));
        }
        collisionCombo.setSelectedIndex(1);
        collisionCombo.setToolTipText("Collision painted by left click; right click samples a cell");

        LayerCanvas canvas = new LayerCanvas(layer, collisionCombo);
        int cellSize = Math.max(12, Math.min(80,
                240 / Math.max(tile.getCollisionFootprintWidth(),
                        tile.getCollisionFootprintHeight())));
        canvas.setCellSize(cellSize);

        JButton fillButton = new JButton("Fill Layer");
        JButton clearButton = new JButton("Clear Layer");
        JButton copyButton = new JButton("Copy Layer");
        JButton pasteButton = new JButton("Paste Layer");
        pasteLayerButtons.add(pasteButton);
        fillButton.addActionListener(e -> fillLayer(layer,
                collisionCombo.getSelectedIndex() - 1));
        clearButton.addActionListener(e -> fillLayer(layer, -1));
        copyButton.addActionListener(e -> {
            CollisionDefaultsClipboard.copyLayer(handler.getTileSelected(), layer);
            updateClipboardButtons();
        });
        pasteButton.addActionListener(e -> {
            CollisionDefaultsClipboard.pasteLayer(handler.getTileSelected(), layer);
            fireChanged();
            canvas.repaint();
        });

        JPanel header = new JPanel(new BorderLayout(4, 3));
        header.add(new JLabel("Layer " + layer, SwingConstants.CENTER), BorderLayout.NORTH);
        header.add(collisionCombo, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 4, 3));
        buttons.add(fillButton);
        buttons.add(clearButton);
        buttons.add(copyButton);
        buttons.add(pasteButton);

        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(header, BorderLayout.NORTH);
        JPanel canvasWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        canvasWrapper.add(canvas);
        panel.add(canvasWrapper, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void updateFootprintControls(Tile tile) {
        updatingFootprint = true;
        followDisplaySize.setSelected(!tile.hasCustomCollisionFootprint());
        widthSpinner.setValue(tile.getCollisionFootprintWidth());
        heightSpinner.setValue(tile.getCollisionFootprintHeight());
        updateAnchorMaximum(anchorXSpinner, tile.getCollisionFootprintWidth() - 1);
        updateAnchorMaximum(anchorYSpinner, tile.getCollisionFootprintHeight() - 1);
        anchorXSpinner.setValue(tile.getCollisionFootprintAnchorX());
        anchorYSpinner.setValue(tile.getCollisionFootprintAnchorY());
        updatingFootprint = false;
        updateFootprintControlState();
    }

    private void updateFootprintControlState() {
        boolean enabled = !followDisplaySize.isSelected();
        widthSpinner.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);
        anchorXSpinner.setEnabled(enabled);
        anchorYSpinner.setEnabled(enabled);
    }

    private void updateAnchorMaximum(JSpinner spinner, int maximum) {
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        model.setMaximum(Math.max(0, maximum));
        if ((Integer) model.getValue() > maximum) {
            model.setValue(Math.max(0, maximum));
        }
    }

    private void applyFootprint() {
        if (handler == null || handler.getTileset().size() == 0 || updatingFootprint) {
            return;
        }
        Tile tile = handler.getTileSelected();
        if (followDisplaySize.isSelected()) {
            tile.resetCollisionFootprint();
        } else {
            tile.setCollisionFootprint((Integer) widthSpinner.getValue(),
                    (Integer) heightSpinner.getValue(),
                    (Integer) anchorXSpinner.getValue(),
                    (Integer) anchorYSpinner.getValue());
        }
        currentWidth = -1;
        refreshSelectedTile(true);
        fireChanged();
    }

    private void fillLayer(int layer, int value) {
        Tile tile = handler.getTileSelected();
        int[][] grid = tile.getOrCreateCollisionDefaultGrid(layer);
        for (int[] column : grid) {
            Arrays.fill(column, value);
        }
        tile.pruneEmptyCollisionDefaults();
        fireChanged();
        layersPanel.repaint();
    }

    private void copyAll() {
        if (currentTileIndex >= 0) {
            CollisionDefaultsClipboard.copyAll(handler.getTileSelected(),
                    CollisionTypes.numLayersPerGame[handler.getGameIndex()]);
            updateClipboardButtons();
        }
    }

    private void pasteAll() {
        if (currentTileIndex >= 0 && CollisionDefaultsClipboard.hasAllLayers()) {
            CollisionDefaultsClipboard.pasteAll(handler.getTileSelected(),
                    CollisionTypes.numLayersPerGame[handler.getGameIndex()]);
            fireChanged();
            layersPanel.repaint();
        }
    }

    private void clearAll() {
        if (currentTileIndex >= 0) {
            handler.getTileSelected().getCollisionDefaults().clear();
            fireChanged();
            layersPanel.repaint();
        }
    }

    private void updateClipboardButtons() {
        pasteAllButton.setEnabled(CollisionDefaultsClipboard.hasAllLayers());
        for (JButton button : pasteLayerButtons) {
            button.setEnabled(CollisionDefaultsClipboard.hasLayer());
        }
    }

    private void fireChanged() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private class LayerCanvas extends JPanel {
        private final int layer;
        private final JComboBox<String> collisionCombo;
        private int cellSize;

        LayerCanvas(int layer, JComboBox<String> collisionCombo) {
            this.layer = layer;
            this.collisionCombo = collisionCombo;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setToolTipText("Left click paints; right click samples the cell value");
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleCell(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                        handleCell(e);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void setCellSize(int cellSize) {
            this.cellSize = cellSize;
            Tile tile = handler.getTileSelected();
            setPreferredSize(new Dimension(tile.getCollisionFootprintWidth() * cellSize,
                    tile.getCollisionFootprintHeight() * cellSize));
        }

        private void handleCell(MouseEvent event) {
            Tile tile = handler.getTileSelected();
            int x = event.getX() / cellSize;
            int y = event.getY() / cellSize;
            if (x < 0 || x >= tile.getCollisionFootprintWidth()
                    || y < 0 || y >= tile.getCollisionFootprintHeight()) {
                return;
            }
            if (SwingUtilities.isRightMouseButton(event)) {
                int[][] grid = tile.getCollisionDefaults().get(layer);
                int value = grid == null ? -1 : grid[x][y];
                collisionCombo.setSelectedIndex(value + 1);
                return;
            }
            if (SwingUtilities.isLeftMouseButton(event)) {
                int[][] grid = tile.getOrCreateCollisionDefaultGrid(layer);
                grid[x][y] = collisionCombo.getSelectedIndex() - 1;
                tile.pruneEmptyCollisionDefaults();
                fireChanged();
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (handler == null || handler.getTileset().size() == 0) {
                return;
            }
            Tile tile = handler.getTileSelected();
            int width = tile.getCollisionFootprintWidth();
            int height = tile.getCollisionFootprintHeight();
            int anchorX = tile.getCollisionFootprintAnchorX();
            int anchorY = tile.getCollisionFootprintAnchorY();
            int imageX = anchorX * cellSize;
            int imageY = (anchorY - tile.getPaletteDisplayHeight() + 1) * cellSize;
            graphics.drawImage(tile.getPaletteThumbnail(), imageX, imageY,
                    tile.getPaletteDisplayWidth() * cellSize,
                    tile.getPaletteDisplayHeight() * cellSize, null);

            Graphics2D g2d = (Graphics2D) graphics;
            int[][] grid = tile.getCollisionDefaults().get(layer);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int value = grid == null ? -1 : grid[x][y];
                    if (value >= 0) {
                        Color fill = collisionTypes.getFillColor(layer, value);
                        g2d.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 145));
                        g2d.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                        if (cellSize >= 22) {
                            g2d.setColor(CollisionTypes.getContrastColor(fill));
                            g2d.drawString(String.format("%02X", value),
                                    x * cellSize + cellSize / 2 - 7,
                                    y * cellSize + cellSize / 2 + 5);
                        }
                    }
                    g2d.setColor(new Color(0, 0, 0, 130));
                    g2d.drawRect(x * cellSize, y * cellSize,
                            cellSize - 1, cellSize - 1);
                }
            }
            g2d.setColor(Color.YELLOW);
            g2d.drawRect(anchorX * cellSize + 1, anchorY * cellSize + 1,
                    cellSize - 3, cellSize - 3);
            g2d.drawRect(anchorX * cellSize + 2, anchorY * cellSize + 2,
                    cellSize - 5, cellSize - 5);
        }
    }
}
