package editor;

import editor.about.AboutDialog;
import editor.buildingeditor2.BuildingEditorChooser;
import editor.game.Game;
import editor.handler.MapEditorHandler;
import editor.layerselector.ThumbnailLayerSelector;
import editor.mapdisplay.MapDisplay;
import editor.mapgroups.VisualizeExportGroupsDialog;
import editor.mapmatrix.MapMatrixDisplay;
import editor.settings.SettingsDialog;
import editor.smartdrawing.SmartGridDisplay;
import editor.tileselector.TileSelector;
import editor.tileseteditor.TilesetEditorDialog;
import formats.animationeditor.AnimationEditorDialog;
import formats.backsound.BacksoundEditorDialog;
import formats.bdhc.BdhcEditorDialog;
import formats.bdhcam.BdhcamEditorDialog;
import formats.collisions.CollisionsEditorDialog;
import formats.collisions.bw.CollisionsEditorDialogBW;
import formats.nsbtx.NsbtxEditorDialog;
import formats.nsbtx2.NsbtxEditorDialog2;
import javax.swing.JOptionPane;

final class ToolDialogLauncher {
    private final MainFrame frame;
    private final MapEditorHandler handler;
    private final MapDisplay mapDisplay;
    private final TileSelector tileSelector;
    private final editor.tileseteditor.TileDisplay tileDisplay;
    private final SmartGridDisplay smartGridDisplay;
    private final ThumbnailLayerSelector thumbnailLayerSelector;
    private final MapMatrixDisplay mapMatrixDisplay;
    private final MainFrameViewUpdater viewUpdater;

    ToolDialogLauncher(MainFrameContext context) {
        this.frame = context.frame;
        this.handler = context.handler;
        this.mapDisplay = context.mapDisplay;
        this.tileSelector = context.tileSelector;
        this.tileDisplay = context.tileDisplay;
        this.smartGridDisplay = context.smartGridDisplay;
        this.thumbnailLayerSelector = context.thumbnailLayerSelector;
        this.mapMatrixDisplay = context.mapMatrixDisplay;
        this.viewUpdater = context.viewUpdater;
    }

    void showPreferences() {
        SettingsDialog settingsDialog = new SettingsDialog(frame);
        settingsDialog.setVisible(true);
    }

    void openTilesetEditor() {
        final TilesetEditorDialog dialog = new TilesetEditorDialog(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        if (handler.getTileset().size() > 0) {
            handler.getTileset().removeUnusedTextures();
            dialog.fixIndices();
            tileSelector.updateLayout();
            handler.getMapMatrix().updateAllLayersGL();
            handler.getMapMatrix().updateBordersData();
            handler.updateAllMapThumbnails();
            mapMatrixDisplay.updateSize();
            viewUpdater.updateMapMatrixDisplay();
            mapDisplay.requestUpdate();
            mapDisplay.repaint();
            tileDisplay.requestUpdate();
            tileDisplay.repaint();
            smartGridDisplay.updateSize();
            smartGridDisplay.repaint();
            thumbnailLayerSelector.drawAllLayerThumbnails();
            thumbnailLayerSelector.repaint();
        }

        frame.repaint();
    }

    void openExportGroupsList() {
        final VisualizeExportGroupsDialog dialog = new VisualizeExportGroupsDialog(frame, true);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        frame.repaint();
    }

    void openCollisionsEditor() {
        mapDisplay.requestScreenshot();
        mapDisplay.setOrthoView();
        mapDisplay.setCameraAtSelectedMap();
        boolean gridEnabled = mapDisplay.isGridEnabled();
        mapDisplay.disableGridView();
        mapDisplay.display();
        final CollisionsEditorDialog dialog = new CollisionsEditorDialog(frame);
        dialog.init(handler, mapDisplay.getScreenshot());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        mapDisplay.setGridEnabled(gridEnabled);
        mapDisplay.display();
    }

    void openBdhcEditor() {
        if (Game.isGenV(handler.getGameIndex())) {
            CollisionsEditorDialogBW dialog = new CollisionsEditorDialogBW(frame);
            dialog.init(handler);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);

            mapDisplay.requestUpdate();
            mapDisplay.repaint();
        } else {
            mapDisplay.requestScreenshot();
            mapDisplay.setOrthoView();
            mapDisplay.setCameraAtSelectedMap();
            boolean useGrid = mapDisplay.isGridEnabled();
            mapDisplay.disableGridView();
            mapDisplay.display();
            final BdhcEditorDialog dialog = new BdhcEditorDialog(frame);
            dialog.init(handler, mapDisplay.getScreenshot());
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            mapDisplay.setGridEnabled(useGrid);
            mapDisplay.requestUpdate();
            mapDisplay.display();
        }
    }

    void openBacksoundEditor() {
        if (handler.getGame().gameSelected == Game.HEART_GOLD || handler.getGame().gameSelected == Game.SOUL_SILVER) {
            mapDisplay.requestScreenshot();
            mapDisplay.setOrthoView();
            mapDisplay.setCameraAtSelectedMap();
            boolean useGrid = mapDisplay.isGridEnabled();
            mapDisplay.disableGridView();
            mapDisplay.display();
            final BacksoundEditorDialog dialog = new BacksoundEditorDialog(frame);
            dialog.init(handler, mapDisplay.getScreenshot());
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            mapDisplay.setGridEnabled(useGrid);
            mapDisplay.display();
        } else {
            JOptionPane.showMessageDialog(frame, "Only HGSS have Backsound files",
                    "Backsound Editor not available", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void openBdhcamEditor() {
        if (handler.getGame().gameSelected > Game.PEARL && handler.getGame().gameSelected < Game.BLACK) {
            mapDisplay.requestScreenshot();
            mapDisplay.setOrthoView();
            mapDisplay.setCameraAtSelectedMap();
            boolean useGrid = mapDisplay.isGridEnabled();
            mapDisplay.disableGridView();
            mapDisplay.display();
            final BdhcamEditorDialog dialog = new BdhcamEditorDialog(frame);
            dialog.init(handler, mapDisplay.getScreenshot());
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            mapDisplay.setGridEnabled(useGrid);
            mapDisplay.requestUpdate();
            mapDisplay.repaint();

        } else {
            JOptionPane.showMessageDialog(frame, "Only Platinum and HGSS have BDCAM files available",
                    "BDHCAM editor is not available", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    void openNsbtxEditor() {
        final NsbtxEditorDialog dialog = new NsbtxEditorDialog(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    void openNsbtxEditor2() {
        final NsbtxEditorDialog2 dialog = new NsbtxEditorDialog2(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    void openBuildingEditor2() {
        BuildingEditorChooser.loadGame(handler);
    }

    void openAnimationEditor() {
        final AnimationEditorDialog dialog = new AnimationEditorDialog(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    void openKeyboardInfoDialog() {
        final editor.keyboard.KeyboardInfoDialog2 dialog = new editor.keyboard.KeyboardInfoDialog2(frame);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    void changeGame() {
        final editor.gameselector.GameChangerDialog dialog = new editor.gameselector.GameChangerDialog(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        if (dialog.getReturnValue() == editor.gameselector.GameSelectorDialog.ACCEPTED) {
            viewUpdater.updateViewGame();

            handler.getMapMatrix().updateAllLayersGL();
            mapDisplay.repaint();

            viewUpdater.updateViewGeometryCount();
        }
    }

    void openAboutDialog() {
        final AboutDialog dialog = new AboutDialog(frame);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    void openBwCollisionsEditor() {
        CollisionsEditorDialogBW dialog = new CollisionsEditorDialogBW(frame);
        dialog.init(handler);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        mapDisplay.requestUpdate();
        mapDisplay.repaint();
    }
}
