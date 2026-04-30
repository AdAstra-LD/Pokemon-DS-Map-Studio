package editor;

import editor.handler.MapEditorHandler;
import editor.heightselector.HeightSelector;
import editor.layerselector.ThumbnailLayerSelector;
import editor.mapdisplay.MapDisplay;
import editor.mapmatrix.MapMatrixDisplay;
import editor.smartdrawing.SmartGridDisplay;
import editor.tileselector.TileSelector;
import editor.tileseteditor.TileDisplay;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;

final class MainFrameContext {
    final MainFrame frame;
    final MapEditorHandler handler;
    final Preferences prefs;
    final JMenu openRecentMapMenu;
    final JMenuItem clearHistoryMenuItem;
    final JSplitPane mainWindowSplitPane;
    final JSplitPane matrixSplitPane;
    final MapDisplay mapDisplay;
    final JPanel mapDisplayContainer;
    final TileDisplay tileDisplay;
    final TileSelector tileSelector;
    final MapMatrixDisplay mapMatrixDisplay;
    final HeightSelector heightSelector;
    final JScrollPane tileListScrollPane;
    final SmartGridDisplay smartGridDisplay;
    final ThumbnailLayerSelector thumbnailLayerSelector;
    final JScrollPane mapMatrixScrollPane;
    final JLabel gameNameLabel;
    final JLabel gameIconLabel;
    final JPanel areaColorPanel;
    final JCheckBox exportGroupCenterCheckBox;
    final JSpinner selectedAreaSpinner;
    final JPanel exportGroupColorPanel;
    final JSpinner selectedExportGroupSpinner;
    final JLabel mapCoordsLabel;
    final JLabel numPolygonsLabel;
    final JLabel numMaterialsLabel;
    final JLabel tileTextLabel;
    final JPanel statusBarPanel;
    final JLabel statusLabel;
    final JButton undoButton;
    final JButton redoButton;
    final AbstractButton useBackImageControl;

    MainFrameBusyRunner busyRunner;
    MainFrameViewUpdater viewUpdater;
    RecentMapsMenu recentMapsMenu;

    MainFrameContext(MainFrame frame, MapEditorHandler handler, Preferences prefs, JMenu openRecentMapMenu,
            JMenuItem clearHistoryMenuItem, JSplitPane mainWindowSplitPane, JSplitPane matrixSplitPane,
            MapDisplay mapDisplay, JPanel mapDisplayContainer, TileDisplay tileDisplay, TileSelector tileSelector,
            MapMatrixDisplay mapMatrixDisplay, HeightSelector heightSelector, JScrollPane tileListScrollPane,
            SmartGridDisplay smartGridDisplay, ThumbnailLayerSelector thumbnailLayerSelector,
            JScrollPane mapMatrixScrollPane, JLabel gameNameLabel, JLabel gameIconLabel, JPanel areaColorPanel,
            JCheckBox exportGroupCenterCheckBox, JSpinner selectedAreaSpinner, JPanel exportGroupColorPanel,
            JSpinner selectedExportGroupSpinner, JLabel mapCoordsLabel, JLabel numPolygonsLabel,
            JLabel numMaterialsLabel, JLabel tileTextLabel, JPanel statusBarPanel, JLabel statusLabel,
            JButton undoButton, JButton redoButton, AbstractButton useBackImageControl) {
        this.frame = frame;
        this.handler = handler;
        this.prefs = prefs;
        this.openRecentMapMenu = openRecentMapMenu;
        this.clearHistoryMenuItem = clearHistoryMenuItem;
        this.mainWindowSplitPane = mainWindowSplitPane;
        this.matrixSplitPane = matrixSplitPane;
        this.mapDisplay = mapDisplay;
        this.mapDisplayContainer = mapDisplayContainer;
        this.tileDisplay = tileDisplay;
        this.tileSelector = tileSelector;
        this.mapMatrixDisplay = mapMatrixDisplay;
        this.heightSelector = heightSelector;
        this.tileListScrollPane = tileListScrollPane;
        this.smartGridDisplay = smartGridDisplay;
        this.thumbnailLayerSelector = thumbnailLayerSelector;
        this.mapMatrixScrollPane = mapMatrixScrollPane;
        this.gameNameLabel = gameNameLabel;
        this.gameIconLabel = gameIconLabel;
        this.areaColorPanel = areaColorPanel;
        this.exportGroupCenterCheckBox = exportGroupCenterCheckBox;
        this.selectedAreaSpinner = selectedAreaSpinner;
        this.exportGroupColorPanel = exportGroupColorPanel;
        this.selectedExportGroupSpinner = selectedExportGroupSpinner;
        this.mapCoordsLabel = mapCoordsLabel;
        this.numPolygonsLabel = numPolygonsLabel;
        this.numMaterialsLabel = numMaterialsLabel;
        this.tileTextLabel = tileTextLabel;
        this.statusBarPanel = statusBarPanel;
        this.statusLabel = statusLabel;
        this.undoButton = undoButton;
        this.redoButton = redoButton;
        this.useBackImageControl = useBackImageControl;
    }
}
