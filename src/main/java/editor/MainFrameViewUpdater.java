package editor;

import com.jogamp.opengl.GLContext;
import editor.game.Game;
import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.heightselector.HeightSelector;
import editor.layerselector.ThumbnailLayerSelector;
import editor.mapdisplay.MapDisplay;
import editor.mapdisplay.ViewMode;
import editor.mapmatrix.MapMatrixDisplay;
import editor.smartdrawing.SmartGridDisplay;
import editor.tileselector.TileSelector;
import editor.tileseteditor.TileDisplay;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import tileset.TilesetRenderer;

final class MainFrameViewUpdater {
    private final MapEditorHandler handler;
    private final HeightSelector heightSelector;
    private final TileSelector tileSelector;
    private final JScrollPane tileListScrollPane;
    private final SmartGridDisplay smartGridDisplay;
    private final ThumbnailLayerSelector thumbnailLayerSelector;
    private final MapDisplay mapDisplay;
    private final JPanel mapDisplayContainer;
    private final TileDisplay tileDisplay;
    private final JScrollPane mapMatrixScrollPane;
    private final MapMatrixDisplay mapMatrixDisplay;
    private final JLabel gameNameLabel;
    private final JLabel gameIconLabel;
    private final JPanel areaColorPanel;
    private final ExportGroupCenterCheckBox exportGroupCenterCheckBox;
    private final JSpinner selectedAreaSpinner;
    private final JPanel exportGroupColorPanel;
    private final JSpinner selectedExportGroupSpinner;
    private final JLabel mapCoordsLabel;
    private final JLabel numPolygonsLabel;
    private final JLabel numMaterialsLabel;
    private final JLabel tileTextLabel;

    MainFrameViewUpdater(MainFrameContext context) {
        this.handler = context.handler;
        this.heightSelector = context.heightSelector;
        this.tileSelector = context.tileSelector;
        this.tileListScrollPane = context.tileListScrollPane;
        this.smartGridDisplay = context.smartGridDisplay;
        this.thumbnailLayerSelector = context.thumbnailLayerSelector;
        this.mapDisplay = context.mapDisplay;
        this.mapDisplayContainer = context.mapDisplayContainer;
        this.tileDisplay = context.tileDisplay;
        this.mapMatrixScrollPane = context.mapMatrixScrollPane;
        this.mapMatrixDisplay = context.mapMatrixDisplay;
        this.gameNameLabel = context.gameNameLabel;
        this.gameIconLabel = context.gameIconLabel;
        this.areaColorPanel = context.areaColorPanel;
        this.exportGroupCenterCheckBox = context.exportGroupCenterCheckBox;
        this.selectedAreaSpinner = context.selectedAreaSpinner;
        this.exportGroupColorPanel = context.exportGroupColorPanel;
        this.selectedExportGroupSpinner = context.selectedExportGroupSpinner;
        this.mapCoordsLabel = context.mapCoordsLabel;
        this.numPolygonsLabel = context.numPolygonsLabel;
        this.numMaterialsLabel = context.numMaterialsLabel;
        this.tileTextLabel = context.tileTextLabel;
    }

    void repaintHeightSelector() {
        heightSelector.repaint();
    }

    void repaintTileSelector() {
        tileSelector.repaint();
    }

    void repaintTileDisplay() {
        tileDisplay.repaint();
    }

    void updateTileSelectorScrollBar() {
        int y = tileSelector.getTileSelectedY() - tileListScrollPane.getHeight() / 2;
        tileListScrollPane.getVerticalScrollBar().setValue(y);
    }

    void updateMapMatrixDisplayScrollBars() {
        Point min = handler.getMapMatrix().getMinCoords();
        Point p = handler.getMapSelected();

        int x = (int) ((p.x - min.x) * MapData.mapThumbnailSize * mapMatrixDisplay.getScale())
                - mapMatrixScrollPane.getWidth() / 2;
        int y = (int) ((p.y - min.y) * MapData.mapThumbnailSize * mapMatrixDisplay.getScale())
                - mapMatrixScrollPane.getHeight() / 2;

        mapMatrixScrollPane.getHorizontalScrollBar().setValue(x);
        mapMatrixScrollPane.getVerticalScrollBar().setValue(y);
    }

    void repaintThumbnailLayerSelector() {
        thumbnailLayerSelector.repaint();
    }

    void repaintMapDisplay() {
        mapDisplay.repaint();
    }

    void updateViewGame() {
        gameNameLabel.setText(Game.gameNames[handler.getGameIndex()]);
        gameIconLabel.setIcon(new ImageIcon(handler.getGame().gameIcons[handler.getGameIndex()]));
    }

    void updateViewMapInfo() {
        MapData currentMap = handler.getCurrentMap();

        areaColorPanel.setBackground(handler.getMapMatrix().getAreaColors().get(currentMap.getAreaIndex()));
        areaColorPanel.repaint();
        exportGroupColorPanel.setBackground(
                handler.getMapMatrix().getExportgroupColors().get(currentMap.getExportGroupIndex()));
        exportGroupColorPanel.repaint();

        selectedAreaSpinner.setValue(currentMap.getAreaIndex());
        int exportGroupIndex = currentMap.getExportGroupIndex();
        exportGroupCenterCheckBox.setEnabled(exportGroupIndex > 0);
        exportGroupCenterCheckBox.setCenterState(getExportGroupCenterState(currentMap, exportGroupIndex));
        selectedExportGroupSpinner.setValue(exportGroupIndex);

        updateViewGeometryCount();
        updateTileSelectedID();

        Point coords = handler.getMapSelected();
        mapCoordsLabel.setText("(" + coords.x + ", " + coords.y + ")");
    }

    private ExportGroupCenterCheckBox.CenterState getExportGroupCenterState(MapData currentMap, int exportGroupIndex) {
        if (exportGroupIndex <= 0) {
            return ExportGroupCenterCheckBox.CenterState.EMPTY;
        }
        if (currentMap.isExportGroupCenter()) {
            return ExportGroupCenterCheckBox.CenterState.CURRENT_MAP;
        }
        return handler.getMapMatrix().getExportGroupCenterCoords(exportGroupIndex) == null
                ? ExportGroupCenterCheckBox.CenterState.EMPTY
                : ExportGroupCenterCheckBox.CenterState.OTHER_MAP;
    }

    void updateViewGeometryCount() {
        try {
            numPolygonsLabel.setText(String.valueOf(handler.getGrid().getNumPolygons()));
            numMaterialsLabel.setText(String.valueOf(handler.getGrid().getNumMaterials()));
        } catch (Exception ex) {
            ex.printStackTrace();
            numPolygonsLabel.setText("Error!");
            numMaterialsLabel.setText("Error!");
        }
    }

    void updateTileSelectedID() {
        if (handler.getTileset().getTiles().size() > 0) {
            try {
                String tileInfo = handler.getTileIndexSelected() + "   ";
                tileInfo += String.valueOf(handler.getTileSelected().getObjFilename());
                tileTextLabel.setText(tileInfo);
            } catch (Exception ex) {
                ex.printStackTrace();
                tileTextLabel.setText("Error!");
            }
        }
    }

    void updateMapMatrixDisplay() {
        Dimension size = mapMatrixScrollPane.getSize();
        mapMatrixDisplay.updateSize();
        mapMatrixDisplay.revalidate();
        mapMatrixDisplay.updateMapsImage();

        mapMatrixScrollPane.setPreferredSize(size);
        mapMatrixScrollPane.revalidate();
    }

    void renderTilesetThumbnails() {
        GLContext context = mapDisplay.getContext();
        TilesetRenderer tilesetRenderer = new TilesetRenderer(handler.getTileset());
        try {
            tilesetRenderer.renderTiles();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        tilesetRenderer.destroy();
        mapDisplay.setContext(context, false);
    }

    void updateMapDisplaySize() {
        if (mapDisplay.getViewMode() == ViewMode.VIEW_3D_MODE) {
            mapDisplay.setPreferredSize(mapDisplayContainer.getSize());
            mapDisplayContainer.revalidate();
        } else {
            int size = Math.min(mapDisplayContainer.getWidth(), mapDisplayContainer.getHeight());
            mapDisplay.setPreferredSize(new Dimension(size, size));
            mapDisplayContainer.revalidate();
        }
    }

    void updateViewAllMapData() {
        renderTilesetThumbnails();

        handler.setIndexTileSelected(0);
        handler.setSmartGridIndexSelected(0);

        handler.getMapMatrix().updateAllLayersGL();
        handler.getMapMatrix().updateBordersData();
        handler.updateAllMapThumbnails();
        mapMatrixDisplay.updateSize();
        updateMapMatrixDisplay();
        updateViewMapInfo();

        tileSelector.updateLayout();
        tileSelector.repaint();
        mapDisplay.requestUpdate();
        mapDisplay.setCameraAtSelectedMap();
        mapDisplay.repaint();
        tileDisplay.requestUpdate();
        tileDisplay.repaint();

        smartGridDisplay.updateSize();
        smartGridDisplay.repaint();
        thumbnailLayerSelector.drawAllLayerThumbnails();
        thumbnailLayerSelector.repaint();
    }
}
