package editor;

import editor.grid.MapGrid;
import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.layerselector.ThumbnailLayerSelector;
import editor.mapdisplay.MapDisplay;
import editor.state.MapLayerState;
import editor.state.MapState;
import editor.state.StateHandler;
import java.awt.Point;

final class MapEditActions {
    private final MainFrame frame;
    private final MapEditorHandler handler;
    private final MapDisplay mapDisplay;
    private final ThumbnailLayerSelector thumbnailLayerSelector;
    private final MainFrameViewUpdater viewUpdater;

    MapEditActions(MainFrameContext context) {
        this.frame = context.frame;
        this.handler = context.handler;
        this.mapDisplay = context.mapDisplay;
        this.thumbnailLayerSelector = context.thumbnailLayerSelector;
        this.viewUpdater = context.viewUpdater;
    }

    void moveLayerUp() {
        int curIndex = handler.getActiveLayerIndex();
        int indexUp = curIndex - 1;

        if (indexUp < 0) {
            indexUp += MapGrid.numLayers;
        }

        MapGrid mapGrid = handler.getMapData().getGrid();

        handler.copyLayer(indexUp);
        mapGrid.tileLayers[indexUp] = mapGrid.tileLayers[curIndex];
        mapGrid.heightLayers[indexUp] = mapGrid.heightLayers[curIndex];
        handler.pasteLayer(curIndex);
        handler.refreshLayer(indexUp);

        handler.setActiveTileLayer(indexUp);
    }

    void moveLayerDown() {
        int curIndex = handler.getActiveLayerIndex();
        int indexDown = curIndex + 1;

        indexDown %= MapGrid.numLayers;

        MapGrid mapGrid = handler.getMapData().getGrid();

        handler.copyLayer(indexDown);
        mapGrid.tileLayers[indexDown] = mapGrid.tileLayers[curIndex];
        mapGrid.heightLayers[indexDown] = mapGrid.heightLayers[curIndex];
        handler.pasteLayer(curIndex);
        handler.refreshLayer(indexDown);

        handler.setActiveTileLayer(indexDown);
    }

    void undoMapState() {
        StateHandler mapStateHandler = handler.getMapStateHandler();
        if (mapStateHandler.canGetPreviousState()) {
            MapState pendingState = (MapState) mapStateHandler.getLastState();
            MapState state = (MapState) mapStateHandler.getPreviousState(
                    pendingState.captureCurrentState());
            state.revertState();
            frame.getRedoButton().setEnabled(true);
            if (!mapStateHandler.canGetPreviousState()) {
                frame.getUndoButton().setEnabled(false);
            }
            for (Point mapCoord : state.getAffectedMaps()) {
                MapData mapData = handler.getMapMatrix().getMap(mapCoord);
                if (mapData == null) {
                    continue;
                }
                for (Integer layer : state.getAffectedLayers()) {
                    mapData.getGrid().updateMapLayerGL(layer, handler.useRealTimePostProcessing());
                }
                mapData.updateMapThumbnail();
            }

            if (state.shouldPruneUnusedMaps()) {
                handler.getMapMatrix().removeUnusedMaps();
            }
            if (!handler.mapSelectedExists()) {
                handler.setDefaultMapSelected();

                handler.getMainFrame().getThumbnailLayerSelector().drawAllLayerThumbnails();
                handler.getMainFrame().getThumbnailLayerSelector().repaint();
            }

            mapDisplay.repaint();
            viewUpdater.updateMapMatrixDisplay();
            thumbnailLayerSelector.drawAllLayerThumbnails();
            thumbnailLayerSelector.repaint();
            viewUpdater.updateViewMapInfo();
        }
    }

    void redoMapState() {
        StateHandler mapStateHandler = handler.getMapStateHandler();
        if (mapStateHandler.canGetNextState()) {
            MapState state = (MapState) mapStateHandler.getNextState();
            state.revertState();
            frame.getUndoButton().setEnabled(true);
            for (Point mapCoord : state.getAffectedMaps()) {
                MapData mapData = handler.getMapMatrix().getMap(mapCoord);
                if (mapData == null) {
                    continue;
                }
                for (Integer layer : state.getAffectedLayers()) {
                    mapData.getGrid().updateMapLayerGL(layer, handler.useRealTimePostProcessing());
                }
                mapData.updateMapThumbnail();
            }
            if (state.shouldPruneUnusedMaps()) {
                handler.getMapMatrix().removeUnusedMaps();
            }

            mapDisplay.repaint();
            viewUpdater.updateMapMatrixDisplay();
            thumbnailLayerSelector.drawAllLayerThumbnails();
            thumbnailLayerSelector.repaint();
            if (!mapStateHandler.canGetNextState()) {
                frame.getRedoButton().setEnabled(false);
            }
            viewUpdater.updateViewMapInfo();
        }
    }

    void moveTilesUp() {
        handler.addMapState(new MapLayerState("Move tiles up", handler));
        handler.getGrid().moveTilesUp(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    void moveTilesDown() {
        handler.addMapState(new MapLayerState("Move tiles down", handler));
        handler.getGrid().moveTilesDown(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    void moveTilesLeft() {
        handler.addMapState(new MapLayerState("Move tiles left", handler));
        handler.getGrid().moveTilesLeft(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    void moveTilesRight() {
        handler.addMapState(new MapLayerState("Move tiles right", handler));
        handler.getGrid().moveTilesRight(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    void moveTilesUpZ() {
        handler.addMapState(new MapLayerState("Move tiles up Z", handler));
        handler.getGrid().moveTilesUpZ(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    void moveTilesDownZ() {
        handler.addMapState(new MapLayerState("Move tiles down Z", handler));
        handler.getGrid().moveTilesDownZ(handler.getActiveLayerIndex());
        refreshActiveLayer();
    }

    private void refreshActiveLayer() {
        thumbnailLayerSelector.drawLayerThumbnail(handler.getActiveLayerIndex());
        thumbnailLayerSelector.repaint();
        mapDisplay.updateActiveMapLayerGL();
        mapDisplay.repaint();
    }
}
