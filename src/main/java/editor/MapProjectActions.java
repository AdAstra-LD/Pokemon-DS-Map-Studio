package editor;

import editor.gameselector.GameTsetSelectorDialog2;
import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.mapdisplay.MapDisplay;
import editor.mapgroups.SavePDSMAPAreasDialog;
import editor.mapmatrix.MapMatrix;
import editor.mapmatrix.MapMatrixDisplay;
import editor.mapmatrix.MapMatrixImportDialog;
import editor.smartdrawing.SmartGridDisplay;
import editor.tileselector.TileSelector;
import editor.tileseteditor.TileDisplay;
import formats.imd.ExportImdDialog;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import tileset.TextureNotFoundException;
import tileset.TileMetadataIO;
import tileset.Tileset;
import tileset.TilesetIO;
import utils.Utils;

final class MapProjectActions {
    private final MainFrame frame;
    private final MapEditorHandler handler;
    private final MainFrameBusyRunner busyRunner;
    private final MainFrameViewUpdater viewUpdater;
    private final RecentMapsMenu recentMapsMenu;
    private final JButton undoButton;
    private final JButton redoButton;
    private final MapDisplay mapDisplay;
    private final TileSelector tileSelector;
    private final SmartGridDisplay smartGridDisplay;
    private final TileDisplay tileDisplay;
    private final editor.layerselector.ThumbnailLayerSelector thumbnailLayerSelector;
    private final MapMatrixDisplay mapMatrixDisplay;
    private final AbstractButton useBackImageControl;

    MapProjectActions(MainFrameContext context) {
        this.frame = context.frame;
        this.handler = context.handler;
        this.busyRunner = context.busyRunner;
        this.viewUpdater = context.viewUpdater;
        this.recentMapsMenu = context.recentMapsMenu;
        this.undoButton = context.undoButton;
        this.redoButton = context.redoButton;
        this.mapDisplay = context.mapDisplay;
        this.tileSelector = context.tileSelector;
        this.smartGridDisplay = context.smartGridDisplay;
        this.tileDisplay = context.tileDisplay;
        this.thumbnailLayerSelector = context.thumbnailLayerSelector;
        this.mapMatrixDisplay = context.mapMatrixDisplay;
        this.useBackImageControl = context.useBackImageControl;
    }

    void openMap(String path) {
        MainFrameBusyRunner.BusyTask busyTask = busyRunner.startLoading();
        Thread openMap = new Thread(() -> {
            busyRunner.setGUIBlock(true);
            try {
                String folderPath = new File(path).getParent();
                String fileName = new File(path).getName();
                handler.setLastMapDirectoryUsed(folderPath);

                handler.getMapMatrix().loadGridsFromFile(path);

                System.out.println("oltre");
                handler.getMapMatrix().filePath = path;
                handler.setDefaultMapSelected();

                frame.setTitle(handler.getMapName() + " - " + handler.getVersionName());

                handler.resetMapStateHandler();
                undoButton.setEnabled(false);
                redoButton.setEnabled(false);

                try {
                    Tileset tileset = TilesetIO.readTilesetFromFile(handler.getMapMatrix().tilesetFilePath);
                    handler.setTileset(tileset);
                    System.out.println("Textures loaded from path: " + new File(path).getParent());

                    viewUpdater.renderTilesetThumbnails();
                    mapDisplay.requestUpdate();
                    mapDisplay.setCameraAtSelectedMap();
                    mapDisplay.repaint();
                    smartGridDisplay.updateSize();
                    smartGridDisplay.repaint();

                    handler.setIndexTileSelected(0);
                    handler.setSmartGridIndexSelected(0);

                    handler.getMapMatrix().updateAllLayersGL();
                    handler.getMapMatrix().updateBordersData();
                    handler.updateAllMapThumbnails();
                    mapMatrixDisplay.updateSize();
                    viewUpdater.updateMapMatrixDisplay();
                    viewUpdater.updateViewMapInfo();

                    tileSelector.updateLayout();
                    tileSelector.repaint();
                    tileDisplay.requestUpdate();
                    tileDisplay.repaint();

                    thumbnailLayerSelector.drawAllLayerThumbnails();
                    thumbnailLayerSelector.repaint();
                } catch (IOException | TextureNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error opening map",
                            JOptionPane.ERROR_MESSAGE);
                }

                handler.getMapMatrix().loadBDHCsFromFile(folderPath, fileName);
                handler.getMapMatrix().loadBdhcamsFromFile(folderPath, fileName);
                handler.getMapMatrix().loadBacksoundsFromFile(folderPath, fileName);
                handler.getMapMatrix().loadCollisionsFromFile(folderPath, fileName);
                handler.getMapMatrix().loadBuildingsFromFile(folderPath, fileName);

                viewUpdater.updateViewGame();

                viewUpdater.repaintHeightSelector();
                viewUpdater.repaintTileSelector();
                viewUpdater.repaintMapDisplay();

                frame.setMapOpened(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Can't open file", "Error opening map",
                        JOptionPane.ERROR_MESSAGE);
            } finally {
                busyRunner.setGUIBlock(false);
                busyTask.finish();
            }
        });
        openMap.start();
    }

    void openMapWithDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastMapDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter("Pokemon DS map (*.pdsmap)", MapMatrix.fileExtension));
        fileChooser.setApproveButtonText("Open");
        fileChooser.setDialogTitle("Open Map");
        final int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            recentMapsMenu.addAndPersist(
                    Utils.addExtensionToPath(fileChooser.getSelectedFile().getPath(), MapMatrix.fileExtension));
            openMap(fileChooser.getSelectedFile().getPath());
        }
    }

    void addMapWithDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastMapDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter("Pokemon DS map (*.pdsmap)", MapMatrix.fileExtension));
        fileChooser.setApproveButtonText("Open");
        fileChooser.setDialogTitle("Add Maps from PDSMAP file");
        final int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.getSelectedFile().exists()) {
                handler.setLastMapDirectoryUsed(fileChooser.getSelectedFile().getParent());
                try {
                    HashMap<Point, MapData> maps = MapMatrix.getGridsFromFile(
                            fileChooser.getSelectedFile().getPath(), handler);

                    final MapMatrixImportDialog dialog = new MapMatrixImportDialog(frame);
                    dialog.init(handler, fileChooser.getSelectedFile().getPath(), maps);
                    dialog.setLocationRelativeTo(frame);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "There was a problem importing the maps",
                            "Can't add maps", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    void splitPDSMAPintoAreas(boolean includeMapAtOrigin) {
        final SavePDSMAPAreasDialog configDialog = new SavePDSMAPAreasDialog(frame, false);
        configDialog.init(handler);
        configDialog.setLocationRelativeTo(frame);
        configDialog.setVisible(true);

        if (configDialog.getReturnValue() == ExportImdDialog.APPROVE_OPTION) {
            final int returnVal = JOptionPane.showConfirmDialog(frame,
                    "This operation may create lots of files and can't be undone. Do you wish to proceed?",
                    "Warning", JOptionPane.OK_CANCEL_OPTION);

            if (returnVal == JOptionPane.OK_OPTION) {
                String areaFolderPath = configDialog.getAreaFolderPath();
                List<Integer> selectedAreaIndices = configDialog.getSelectedAreaIndices();
                MainFrameBusyRunner.BusyTask progressTask =
                        busyRunner.startProgress("Saving areas", selectedAreaIndices.size());

                handler.setLastMapDirectoryUsed(areaFolderPath);
                Thread thread = new Thread(() -> {
                    try {
                        busyRunner.setGUIBlock(true);

                        HashMap<Point, MapData> allAreasMap = handler.getMapMatrix().getMatrix();
                        for (int area : selectedAreaIndices) {
                            progressTask.setMessage("Saving area " + area);

                            HashMap<Point, MapData> singleAreaMap = new HashMap<>();

                            Point origin = new Point(0, 0);
                            MapData originMap = allAreasMap.get(origin);
                            if (includeMapAtOrigin && originMap != null) {
                                singleAreaMap.put(origin, originMap);
                            }

                            for (Point point : handler.getMapMatrix().getAreas().get(area).getCoordList()) {
                                singleAreaMap.put(point, allAreasMap.get(point));
                            }
                            Set<HashMap.Entry<Point, MapData>> areaEntrySet = singleAreaMap.entrySet();

                            handler.getMapMatrix().saveAreaToFile(areaFolderPath, areaEntrySet, area);
                            writeTileset();

                            handler.getMapMatrix().saveCollisions(areaEntrySet);
                            handler.getMapMatrix().saveBacksounds(areaEntrySet);
                            handler.getMapMatrix().saveBDHCs(areaEntrySet);
                            handler.getMapMatrix().saveBdhcams(areaEntrySet);
                            handler.getMapMatrix().saveBuildings(areaEntrySet);

                            saveMapThumbnail();

                            recentMapsMenu.addAndPersist(
                                    Utils.addExtensionToPath(areaFolderPath, MapMatrix.fileExtension));
                            progressTask.increment();
                        }
                    } catch (ParserConfigurationException | TransformerException | IOException ex) {
                        JOptionPane.showMessageDialog(frame, "There was a problem saving all the map files",
                                "Error saving map files", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        progressTask.finish();
                        busyRunner.setGUIBlock(false);
                        JOptionPane.showMessageDialog(frame, "Your maps have been split and saved.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                thread.setDaemon(false);
                thread.start();
            }
        }
    }

    void openTileset(String path) {
        String folderPath = new File(path).getParent();

        handler.setLastTilesetDirectoryUsed(folderPath);
        try {
            Tileset tileset = TilesetIO.readTilesetFromFile(path);
            handler.getMapMatrix().tilesetFilePath = path;
            handler.setTileset(tileset);
            System.out.println("Textures loaded from path: " + new File(path).getParent());

            viewUpdater.renderTilesetThumbnails();

            handler.setIndexTileSelected(0);
            handler.setSmartGridIndexSelected(0);

            tileSelector.updateLayout();
            tileSelector.repaint();
            smartGridDisplay.updateSize();
            smartGridDisplay.repaint();
            mapDisplay.requestUpdate();
            mapDisplay.repaint();
            tileDisplay.requestUpdate();
            tileDisplay.repaint();
            thumbnailLayerSelector.drawAllLayerThumbnails();
            thumbnailLayerSelector.repaint();
        } catch (TextureNotFoundException | IOException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error opening tilset", JOptionPane.ERROR_MESSAGE);
        }

        viewUpdater.repaintHeightSelector();
        viewUpdater.repaintTileSelector();
        viewUpdater.repaintMapDisplay();
    }

    void openTilesetWithDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastTilesetDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastTilesetDirectoryUsed()));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pokemon DS Tileset (*.pdsts)", Tileset.fileExtension));
        fileChooser.setApproveButtonText("Open");
        fileChooser.setDialogTitle("Open");
        final int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getPath();
            openTileset(path);
        }
    }

    void importTileMetadataWithDialog() {
        if (handler.getTileset().size() == 0) {
            JOptionPane.showMessageDialog(frame, "The tileset is empty.",
                    "Import Tile Metadata", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fileChooser = createMetadataFileChooser("Import Tile Metadata");
        if (fileChooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = fileChooser.getSelectedFile();
            handler.setLastTilesetDirectoryUsed(file.getParent());
            TileMetadataIO.readFile(file.getPath(), handler.getTileset(), false);
            viewUpdater.renderTilesetThumbnails();
            tileSelector.updateLayout();
            tileSelector.repaint();
            JOptionPane.showMessageDialog(frame,
                    "Tile metadata merged. Existing folders and tile settings were kept.",
                    "Import Tile Metadata", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Can't import metadata:\n" + ex.getMessage(),
                    "Import Tile Metadata", JOptionPane.ERROR_MESSAGE);
        }
    }

    void exportTileMetadataWithDialog() {
        if (handler.getTileset().size() == 0) {
            JOptionPane.showMessageDialog(frame, "The tileset is empty.",
                    "Export Tile Metadata", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fileChooser = createMetadataFileChooser("Export Tile Metadata");
        fileChooser.setSelectedFile(new File("TileMetadata.meta"));
        if (fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File selected = fileChooser.getSelectedFile();
            String path = selected.getPath().toLowerCase().endsWith(".meta")
                    ? selected.getPath() : selected.getPath() + ".meta";
            handler.setLastTilesetDirectoryUsed(selected.getParent());
            TileMetadataIO.writeFile(path, handler.getTileset());
            JOptionPane.showMessageDialog(frame, "Tile metadata exported.",
                    "Export Tile Metadata", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Can't export metadata:\n" + ex.getMessage(),
                    "Export Tile Metadata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser createMetadataFileChooser(String title) {
        JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastTilesetDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastTilesetDirectoryUsed()));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Pokemon DS Map Studio tile metadata (*.meta)", "meta"));
        fileChooser.setDialogTitle(title);
        return fileChooser;
    }

    void openBackImgWithDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastMapDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        fileChooser.setApproveButtonText("Open");
        fileChooser.setDialogTitle("Open Background Image");
        final int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(fileChooser.getSelectedFile());

                mapDisplay.setBackImage(img);
                mapDisplay.setBackImageEnabled(true);
                useBackImageControl.setSelected(true);

                mapDisplay.repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Can't open file", "Error opening image",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void newMap() {
        final int returnVal = JOptionPane.showConfirmDialog(frame, "Do you want to close current map?",
                "Create new map", JOptionPane.YES_NO_OPTION);
        if (returnVal == JOptionPane.YES_OPTION) {
            final GameTsetSelectorDialog2 dialog = new GameTsetSelectorDialog2(frame);
            dialog.init(handler);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);

            if (dialog.getReturnValue() == GameTsetSelectorDialog2.ACCEPTED) {
                handler.setIndexTileSelected(0);
                handler.setSmartGridIndexSelected(0);

                handler.setMapMatrix(new MapMatrix(handler));
                handler.setMapSelected(new Point(0, 0));

                handler.resetMapStateHandler();
                undoButton.setEnabled(false);
                redoButton.setEnabled(false);

                tileSelector.updateLayout();
                tileSelector.repaint();

                smartGridDisplay.updateSize();
                smartGridDisplay.repaint();

                mapDisplay.requestUpdate();
                mapDisplay.setCameraAtSelectedMap();
                viewUpdater.repaintMapDisplay();
                tileDisplay.requestUpdate();
                tileDisplay.repaint();
                thumbnailLayerSelector.drawAllLayerThumbnails();
                thumbnailLayerSelector.repaint();

                handler.updateAllMapThumbnails();
                mapMatrixDisplay.updateSize();
                viewUpdater.updateMapMatrixDisplay();

                viewUpdater.updateViewGame();

                frame.setTitle(handler.getVersionName());
            }
        }
    }

    void saveMap() {
        MainFrameBusyRunner.BusyTask busyTask = busyRunner.startLoading();
        Thread thread = new Thread(() -> {
            try {
                busyRunner.setGUIBlock(true);

                Set<Map.Entry<Point, MapData>> entrySet = handler.getMapMatrix().getMatrix().entrySet();
                handler.getMapMatrix().saveGridsToFile(handler.getMapMatrix().filePath, entrySet);

                frame.setTitle(handler.getMapName() + " - " + handler.getVersionName());

                writeTileset();

                handler.getMapMatrix().saveCollisions(entrySet);
                handler.getMapMatrix().saveBacksounds(entrySet);
                handler.getMapMatrix().saveBDHCs(entrySet);
                handler.getMapMatrix().saveBdhcams(entrySet);
                handler.getMapMatrix().saveBuildings(entrySet);

                saveMapThumbnail();
            } catch (ParserConfigurationException | TransformerException | IOException ex) {
                JOptionPane.showMessageDialog(frame, "There was a problem saving all the map files",
                        "Error saving map files", JOptionPane.ERROR_MESSAGE);
            } finally {
                busyRunner.setGUIBlock(false);
                busyTask.finish();
            }
        });
        thread.start();
    }

    void saveMapWithDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        if (handler.getLastMapDirectoryUsed() != null) {
            fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pokemon DS map (*.pdsmap)", MapMatrix.fileExtension));
        fileChooser.setApproveButtonText("Save");
        fileChooser.setDialogTitle("Save");
        final int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            handler.setLastMapDirectoryUsed(fileChooser.getSelectedFile().getParent());

            MainFrameBusyRunner.BusyTask busyTask = busyRunner.startLoading();
            Thread thread = new Thread(() -> {
                try {
                    busyRunner.setGUIBlock(true);

                    String path = fileChooser.getSelectedFile().getPath();
                    Set<Map.Entry<Point, MapData>> entrySet = handler.getMapMatrix().getMatrix().entrySet();

                    handler.getMapMatrix().saveGridsToFile(path, entrySet);
                    handler.getMapMatrix().filePath = path;
                    frame.setTitle(handler.getMapName() + " - " + handler.getVersionName());

                    writeTileset();
                    saveMapThumbnail();

                    handler.getMapMatrix().saveCollisions(entrySet);
                    handler.getMapMatrix().saveBacksounds(entrySet);
                    handler.getMapMatrix().saveBDHCs(entrySet);
                    handler.getMapMatrix().saveBdhcams(entrySet);
                    handler.getMapMatrix().saveBuildings(entrySet);

                    recentMapsMenu.addAndPersist(Utils.addExtensionToPath(path, MapMatrix.fileExtension));
                } catch (ParserConfigurationException | TransformerException | IOException ex) {
                    JOptionPane.showMessageDialog(frame, "There was a problem saving all the map files",
                            "Error saving map files", JOptionPane.ERROR_MESSAGE);
                } finally {
                    busyTask.finish();
                    busyRunner.setGUIBlock(false);
                }
            });
            thread.start();
        }
    }

    void saveTilesetWithDialog() {
        if (handler.getTileset().size() > 0) {
            final JFileChooser fileChooser = new JFileChooser();
            if (handler.getLastTilesetDirectoryUsed() != null) {
                fileChooser.setCurrentDirectory(new File(handler.getLastTilesetDirectoryUsed()));
            }
            fileChooser.setFileFilter(
                    new FileNameExtensionFilter("Pokemon DS tileset (*.pdsts)", Tileset.fileExtension));
            fileChooser.setApproveButtonText("Save");
            fileChooser.setDialogTitle("Save Tileset");
            final int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                handler.setLastMapDirectoryUsed(fileChooser.getSelectedFile().getParent());
                try {
                    File file = fileChooser.getSelectedFile();
                    String path = file.getParent();
                    String filename = Utils.removeExtensionFromPath(file.getName()) + "." + Tileset.fileExtension;
                    TilesetIO.writeTilesetToFile(path + File.separator + filename, handler.getTileset());
                    handler.getTileset().saveImagesToFile(path);

                    saveTilesetThumbnail(path + File.separator + "TilesetThumbnail.png");

                    JOptionPane.showMessageDialog(frame,
                            "Tileset and tile metadata successfully exported.", "Tileset saved",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Can't save file", "Error saving tileset",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "The tileset is empty", "Error saving tileset",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void writeTileset() throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
        File file = new File(handler.getMapMatrix().filePath);
        String path = file.getParent();

        String filename = Utils.removeExtensionFromPath(file.getName()) + "." + Tileset.fileExtension;
        TilesetIO.writeTilesetToFile(path + File.separator + filename, handler.getTileset());
        handler.getTileset().saveImagesToFile(path);

        saveTilesetThumbnail(path + File.separator + "TilesetThumbnail.png");
    }

    void saveTilesetThumbnail(String path) throws IOException {
        BufferedImage img = tileSelector.getTilesetImage();
        if (img != null) {
            File file = new File(path);
            ImageIO.write(img, "png", file);
        }
    }

    void saveMapThumbnail() throws IOException {
        mapDisplay.requestScreenshot();
        mapDisplay.display();

        String path = new File(handler.getMapMatrix().filePath).getParent();
        File file = new File(path + File.separator + "MapThumbnail.png");
        ImageIO.write(mapDisplay.getScreenshot(), "png", file);
    }
}
