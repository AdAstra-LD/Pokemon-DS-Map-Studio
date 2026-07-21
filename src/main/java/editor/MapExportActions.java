package editor;

import com.formdev.flatlaf.util.SystemFileChooser;
import editor.converter.ConverterDialog;
import editor.converter.ConverterErrorDialog;
import editor.converter.ExportNsbmdDialog;
import editor.converter.ExportNsbmdResultDialog;
import editor.converter.ExportNsbtxDialog;
import editor.converter.ExportNsbtxResultDialog;
import editor.converter.NsbmdOutputInfoDialog;
import editor.converter.NsbtxOutputInfoDialog;
import editor.game.Game;
import editor.handler.MapEditorHandler;
import editor.tileseteditor.AddTileDialog;
import editor.tileseteditor.ExportTileDialog;
import formats.imd.ExportImdDialog;
import formats.imd.ImdModel;
import formats.imd.ImdOutputInfoDialog;
import formats.mapbin.ExportMapBinDialog;
import formats.mapbin.ExportMapBinInfoDialog;
import formats.nsbtx2.Nsbtx2;
import formats.nsbtx2.NsbtxLoader2;
import formats.obj.ExportMapsObjDialog;
import formats.obj.ExportSingleMapObjDialog;
import formats.obj.ObjWriter;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import tileset.NormalsNotFoundException;
import tileset.TextureNotFoundException;
import utils.Utils;

final class MapExportActions {
    private final MainFrame frame;
    private final MapEditorHandler handler;

    MapExportActions(MainFrameContext context) {
        this.frame = context.frame;
        this.handler = context.handler;
    }

    void saveAllTilesAsObjWithDialog() {
        if (handler.getTileset().size() > 0) {
            final ExportTileDialog exportTileDialog = new ExportTileDialog(handler.getMainFrame(),
                    "Export Tile Settings");
            exportTileDialog.setLocationRelativeTo(frame);
            exportTileDialog.setVisible(true);
            if (exportTileDialog.getReturnValue() == AddTileDialog.APPROVE_OPTION) {
                float scale = exportTileDialog.getScale();
                boolean flip = exportTileDialog.flip();
                boolean includeVertexColors = exportTileDialog.includeVertexColors();

                final SystemFileChooser fileChooser = new SystemFileChooser();
                if (handler.getLastTileObjDirectoryUsed() != null) {
                    fileChooser.setCurrentDirectory(new File(handler.getLastTileObjDirectoryUsed()));
                }
                fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
                fileChooser.setApproveButtonText("Save");
                fileChooser.setDialogTitle("Select folder for saving all tiles as OBJ");
                final int returnVal = fileChooser.showSaveDialog(frame);
                if (returnVal == SystemFileChooser.APPROVE_OPTION) {
                    handler.setLastTileObjDirectoryUsed(fileChooser.getSelectedFile().getPath());
                    try {
                        ObjWriter objWriter = new ObjWriter(handler.getTileset(), handler.getGrid(),
                                fileChooser.getSelectedFile().getPath(), handler.getGameIndex(), true,
                                includeVertexColors, 1.0f);
                        objWriter.writeAllTilesObj(scale, flip);
                        JOptionPane.showMessageDialog(frame, "Tiles succesfully exported.", "Tiles saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Can't save tiles", "Error saving tiles",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "The tileset is empty", "Error saving tiles",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    boolean saveMapAsObjWithDialog(boolean saveTextures) {
        final ExportSingleMapObjDialog exportMapDialog = new ExportSingleMapObjDialog(frame,
                "Export Single OBJ Map - Settings");
        exportMapDialog.setLocationRelativeTo(frame);
        exportMapDialog.setVisible(true);

        if (exportMapDialog.getReturnValue() == ExportMapsObjDialog.APPROVE_OPTION) {
            boolean includeVertexColors = exportMapDialog.includeVertexColors();
            boolean useExportgroups = exportMapDialog.useExportgroups();
            float tileUpscale = exportMapDialog.getTileUpscaling();

            final SystemFileChooser fileChooser = new SystemFileChooser();
            fileChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)));

            if (handler.getLastMapDirectoryUsed() != null) {
                fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
            }

            fileChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("OBJ (*.obj)", "obj"));
            fileChooser.setApproveButtonText("Save");
            fileChooser.setDialogTitle("Select a name for the OBJ map");
            final int returnVal = fileChooser.showSaveDialog(frame);

            if (returnVal == SystemFileChooser.APPROVE_OPTION) {
                handler.setLastMapDirectoryUsed(fileChooser.getSelectedFile().getParent());
                try {
                    String path = fileChooser.getSelectedFile().getPath();

                    String type;
                    int currentExportGroupIndex = handler.getCurrentMap().getExportGroupIndex();
                    if (useExportgroups && currentExportGroupIndex != 0) {
                        type = "group";

                        HashSet<Integer> groupsToExport = new HashSet<>();
                        groupsToExport.add(currentExportGroupIndex);

                        handler.getMapMatrix().saveMapsAsObj(path, saveTextures, includeVertexColors,
                                groupsToExport, tileUpscale);
                    } else {
                        type = "map";

                        handler.getGrid().saveMapToOBJ(handler.getTileset(), path, saveTextures,
                                includeVertexColors, tileUpscale);
                    }
                    JOptionPane.showMessageDialog(frame, "OBJ " + type + " succesfully exported.",
                            type.substring(0, 1).toUpperCase() + type.substring(1) + " saved",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame, "Can't save file.", "Error saving map",
                            JOptionPane.ERROR_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }

    void saveMapAsBinWithDialog() {
        if (handler.getGame().gameSelected >= Game.BLACK) {
            JOptionPane.showMessageDialog(frame, "Can't save Gen V binary files yet", "Error saving bin map",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            final ExportMapBinDialog exportBinDialog = new ExportMapBinDialog(frame, "Export Bin Map Settings");
            exportBinDialog.setLocationRelativeTo(frame);
            exportBinDialog.setVisible(true);

            if (exportBinDialog.getReturnValue() == ExportMapBinDialog.APPROVE_OPTION) {
                HashSet<Point> maps = new HashSet<>();
                if (exportBinDialog.exportCurrentMapBin()) {
                    maps.add(handler.getMapSelected());
                } else if (exportBinDialog.exportAllMapsBin()) {
                    maps.addAll(handler.getMapMatrix().getMatrix().keySet());
                } else {
                    return;
                }

                ExportMapBinInfoDialog exportInfoDialog = new ExportMapBinInfoDialog(frame);
                exportInfoDialog.init(handler, maps, new File(handler.getMapMatrix().filePath).getParent());
                exportInfoDialog.setLocationRelativeTo(frame);
                exportInfoDialog.setVisible(true);
            }
        }
    }

    boolean saveMapsAsObjWithDialog(boolean saveTextures) {
        final ExportMapsObjDialog exportMapDialog = new ExportMapsObjDialog(frame, "Export OBJ Maps Settings");
        exportMapDialog.setLocationRelativeTo(null);
        exportMapDialog.setVisible(true);

        if (exportMapDialog.getReturnValue() == ExportMapsObjDialog.APPROVE_OPTION) {
            boolean includeVertexColors = exportMapDialog.includeVertexColors();
            boolean exportAllMapsBothModes = exportMapDialog.exportAllMapsBothModes();
            boolean exportAllMapsSeparately = exportMapDialog.exportAllMapsSeparately();
            boolean exportAllMapsJoined = exportMapDialog.exportAllMapsJoined();
            boolean useExportgroups = exportMapDialog.useExportgroups();
            float tileUpscale = exportMapDialog.getTileUpscaling();

            final SystemFileChooser fileChooser = new SystemFileChooser();
            fileChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)));
            if (handler.getLastMapDirectoryUsed() != null) {
                fileChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
            }
            fileChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("OBJ (*.obj)", "obj"));
            fileChooser.setApproveButtonText("Save");
            fileChooser.setDialogTitle("Select a name for saving the maps as OBJ");
            final int returnVal = fileChooser.showSaveDialog(frame);

            String type;
            if (useExportgroups) {
                type = "groups";
            } else {
                type = "maps";
            }

            if (returnVal == SystemFileChooser.APPROVE_OPTION) {
                handler.setLastMapDirectoryUsed(fileChooser.getSelectedFile().getParent());
                try {
                    String path = fileChooser.getSelectedFile().getPath();
                    if (exportAllMapsBothModes) {
                        path = Utils.removeMapCoordsFromName(path);
                        handler.getMapMatrix().saveMapsAsObj(path, saveTextures, includeVertexColors,
                                useExportgroups ? handler.getMapMatrix().getExportGroupIndices() : null, tileUpscale);
                        handler.getMapMatrix().saveMapsAsObjJoined(path, saveTextures, includeVertexColors,
                                tileUpscale);

                        JOptionPane.showMessageDialog(frame, "OBJ " + type
                                        + " succesfully exported in both modes.",
                                type.substring(0, 1).toUpperCase() + type.substring(1) + " saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (exportAllMapsSeparately) {
                        path = Utils.removeMapCoordsFromName(path);
                        handler.getMapMatrix().saveMapsAsObj(path, saveTextures, includeVertexColors,
                                useExportgroups ? handler.getMapMatrix().getExportGroupIndices() : null, tileUpscale);

                        JOptionPane.showMessageDialog(frame, "OBJ " + type + " succesfully exported separately.",
                                type.substring(0, 1).toUpperCase() + type.substring(1) + " saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (exportAllMapsJoined) {
                        path = Utils.removeMapCoordsFromName(path);
                        handler.getMapMatrix().saveMapsAsObjJoined(path, saveTextures, includeVertexColors,
                                tileUpscale);

                        JOptionPane.showMessageDialog(frame, "OBJ maps succesfully exported as one.", "Map saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        int currentExportGroupIndex = handler.getCurrentMap().getExportGroupIndex();
                        if (useExportgroups && currentExportGroupIndex != 0) {
                            HashSet<Integer> groupsToExport = new HashSet<>();
                            groupsToExport.add(currentExportGroupIndex);

                            handler.getMapMatrix().saveMapsAsObj(path, saveTextures, includeVertexColors,
                                    groupsToExport, tileUpscale);
                        } else {
                            handler.getGrid().saveMapToOBJ(handler.getTileset(), path, saveTextures,
                                    includeVertexColors, tileUpscale);
                        }

                        JOptionPane.showMessageDialog(frame, "OBJ map succesfully exported.", "Map saved",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame, "Can't save file.", "Error saving map",
                            JOptionPane.ERROR_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }

    boolean multipleObjsToImdDialog() {
        if (handler.getTileset().size() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "There is no tileset loaded.\n"
                            + "The IMD can be exported but the materials will be set to default.\n",
                    "No tileset loaded", JOptionPane.WARNING_MESSAGE);
        }

        final ExportImdDialog configDialog = new ExportImdDialog(frame);
        configDialog.init(handler);
        configDialog.setLocationRelativeTo(frame);
        configDialog.setVisible(true);

        if (configDialog.getReturnValue() == ExportImdDialog.APPROVE_OPTION) {
            ArrayList<String> fileNames = configDialog.getSelectedObjNames();
            String objFolderPath = configDialog.getObjFolderPath();
            String imdFolderPath = configDialog.getImdFolderPath();

            final ImdOutputInfoDialog outputDialog = new ImdOutputInfoDialog(frame);
            outputDialog.init(handler, fileNames, objFolderPath, imdFolderPath);
            outputDialog.setLocationRelativeTo(frame);
            outputDialog.setVisible(true);

            return true;
        }

        return false;
    }

    boolean singleObjToImdDialog() {
        if (handler.getTileset().size() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "There is no tileset loaded.\n"
                            + "The IMD can be exported but the materials will be set to default.\n",
                    "No tileset loaded", JOptionPane.WARNING_MESSAGE);
        }

        final SystemFileChooser openChooser = new SystemFileChooser();
        openChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath) + ".obj"));
        if (handler.getLastMapDirectoryUsed() != null) {
            openChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }
        openChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("OBJ (*.obj)", "obj"));
        openChooser.setApproveButtonText("Open");
        openChooser.setDialogTitle("Open OBJ Map for converting into IMD");
        final int returnValOpen = openChooser.showOpenDialog(frame);
        if (returnValOpen == SystemFileChooser.APPROVE_OPTION) {
            if (openChooser.getSelectedFile().exists()) {
                String pathOpen = openChooser.getSelectedFile().getPath();

                final SystemFileChooser saveChooser = new SystemFileChooser();
                saveChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)));
                saveChooser.setCurrentDirectory(openChooser.getSelectedFile().getParentFile());
                saveChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("IMD (*.imd)", "imd"));
                saveChooser.setApproveButtonText("Save");
                saveChooser.setDialogTitle("Save");
                final int returnValSave = saveChooser.showSaveDialog(frame);
                if (returnValSave == SystemFileChooser.APPROVE_OPTION) {
                    String pathSave = saveChooser.getSelectedFile().getPath();

                    try {
                        ImdModel model = new ImdModel(pathOpen, pathSave, handler.getTileset().getMaterials());
                        final int numVertices = model.getNumVertices();
                        final int numPolygons = model.getNumPolygons();
                        final int numTris = model.getNumTris();
                        final int numQuads = model.getNumQuads();
                        JOptionPane.showMessageDialog(frame, "IMD map succesfully exported.\n\n"
                                        + "Number of Materials: " + model.getNumMaterials() + "\n"
                                        + "Number of Vertices: " + numVertices + "\n"
                                        + "Number of Polygons: " + numPolygons + "\n"
                                        + "Number of Triangles: " + numTris + "\n"
                                        + "Number of Quads: " + numQuads,
                                "Map saved", JOptionPane.INFORMATION_MESSAGE);
                        final int maxNumPolygons = 1800;
                        final int maxNumTris = 1200;
                        if (numTris > maxNumTris) {
                            JOptionPane.showMessageDialog(frame, "The map might not work properly in game.\n\n"
                                            + "The map contains " + numTris + " triangles" + "\n"
                                            + "Try to use less than " + maxNumTris + " triangles" + "\n"
                                            + "Or try to use quads instead of triangles" + "\n",
                                    "Too many triangles", JOptionPane.INFORMATION_MESSAGE);
                        } else if (numPolygons > maxNumPolygons) {
                            JOptionPane.showMessageDialog(frame, "The map may not work properly in game.\n\n"
                                            + "The map contains " + numPolygons + " polygons" + "\n"
                                            + "Try to use less than " + maxNumPolygons + " polygons",
                                    "Too many polygons", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (ParserConfigurationException | TransformerException ex) {
                        JOptionPane.showMessageDialog(frame, "There was a problem parsing the XML data of the IMD",
                                "Can't export IMD", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "There was a problem exporting the IMD",
                                "Can't export IMD", JOptionPane.ERROR_MESSAGE);
                    } catch (TextureNotFoundException | NormalsNotFoundException ex) {
                        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Can't export IMD",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    return true;
                }
            } else {
                JOptionPane.showMessageDialog(frame, "The selected OBJ file could not be opened",
                        "Can't open OBJ", JOptionPane.ERROR_MESSAGE);
            }
        }

        return false;
    }

    boolean saveMapsAsNsbWithDialog() {
        final ExportNsbmdDialog configDialog = new ExportNsbmdDialog(frame, true);
        configDialog.init(handler);
        configDialog.setLocationRelativeTo(frame);
        configDialog.setVisible(true);

        if (configDialog.getReturnValue() == ExportNsbmdDialog.APPROVE_OPTION) {
            ArrayList<String> fileNames = configDialog.getSelectedImdNames();
            String imdFolderPath = configDialog.getImdFolderPath();
            String nsbFolderPath = configDialog.getNsbFolderPath();

            final NsbmdOutputInfoDialog outputDialog = new NsbmdOutputInfoDialog(frame, true);
            outputDialog.init(handler, fileNames, imdFolderPath, nsbFolderPath, configDialog.includeNsbtxInNsbmd());
            outputDialog.setLocationRelativeTo(frame);
            outputDialog.setVisible(true);
            return true;
        }

        return false;
    }

    boolean saveMapAsNsbWithDialog() {
        final ConverterDialog convDialog = new ConverterDialog(frame);
        convDialog.setLocationRelativeTo(frame);
        convDialog.setVisible(true);
        if (convDialog.getReturnValue() == ConverterDialog.APPROVE_OPTION) {
            boolean includeNsbtx = convDialog.includeNsbtxInNsbmd();
            try {
                final SystemFileChooser openChooser = new SystemFileChooser();
                openChooser.setSelectedFile(
                        new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath) + ".imd"));
                if (handler.getLastMapDirectoryUsed() != null) {
                    openChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
                }
                openChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("IMD (*.imd)", "imd"));
                openChooser.setApproveButtonText("Open");
                openChooser.setDialogTitle("Open IMD Map for converting into NSBMD");
                final int returnVal = openChooser.showOpenDialog(frame);
                if (returnVal == SystemFileChooser.APPROVE_OPTION) {
                    String imdPath;
                    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                        imdPath = openChooser.getSelectedFile().getPath();
                    } else {
                        String cwd = System.getProperty("user.dir");
                        imdPath = new File(cwd).toURI()
                                .relativize(openChooser.getSelectedFile().toPath().toRealPath().toUri()).getPath();
                    }
                    final SystemFileChooser saveChooser = new SystemFileChooser();
                    saveChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)));
                    saveChooser.setCurrentDirectory(openChooser.getSelectedFile().getParentFile());
                    saveChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("NSBMD (*.nsbmd)", "nsbmd"));
                    saveChooser.setApproveButtonText("Save");
                    saveChooser.setDialogTitle("Save");
                    final int returnValSave = saveChooser.showSaveDialog(frame);

                    if (returnValSave == SystemFileChooser.APPROVE_OPTION) {
                        String nsbPath = saveChooser.getSelectedFile().getPath();
                        String filename = new File(nsbPath).getName();

                        try {
                            String converterPath = "converter/g3dcvtr.exe";
                            String[] cmd;
                            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                                if (includeNsbtx) {
                                    cmd = new String[]{converterPath, imdPath, "-eboth", "-o", filename};
                                } else {
                                    cmd = new String[]{converterPath, imdPath, "-emdl", "-o", filename};
                                }
                            } else {
                                if (includeNsbtx) {
                                    cmd = new String[]{"wine", converterPath, imdPath, "-eboth", "-o", filename};
                                } else {
                                    cmd = new String[]{"wine", converterPath, imdPath, "-emdl", "-o", filename};
                                }
                            }

                            if (!Files.exists(Paths.get(converterPath))) {
                                throw new IOException();
                            }

                            Process process = new ProcessBuilder(cmd).start();

                            BufferedReader stdError = new BufferedReader(
                                    new InputStreamReader(process.getErrorStream()));

                            String outputString = "";
                            String line;
                            while ((line = stdError.readLine()) != null) {
                                outputString += line + "\n";
                            }

                            process.waitFor();
                            process.destroy();

                            if (!filename.endsWith("nsbmd")) {
                                filename += ".nsbmd";
                            }
                            if (!nsbPath.endsWith("nsbmd")) {
                                nsbPath += ".nsbmd";
                            }

                            System.out.println(System.getProperty("user.dir"));
                            File srcFile = new File(System.getProperty("user.dir") + File.separator + filename);
                            File dstFile = new File(nsbPath);
                            if (srcFile.exists()) {
                                try {
                                    Files.move(srcFile.toPath(), dstFile.toPath(),
                                            StandardCopyOption.REPLACE_EXISTING);

                                    try {
                                        byte[] nsbmdData = Files.readAllBytes(dstFile.toPath());

                                        ExportNsbmdResultDialog resultDialog = new ExportNsbmdResultDialog(frame);
                                        resultDialog.init(nsbmdData);
                                        resultDialog.setLocationRelativeTo(frame);
                                        resultDialog.setVisible(true);
                                    } catch (IOException ex) {
                                        JOptionPane.showMessageDialog(frame, "NSBMD succesfully exported.",
                                                "NSBMD saved", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                } catch (IOException ex) {
                                    JOptionPane.showMessageDialog(frame,
                                            "File was not moved to the save directory. \n"
                                                    + "Reopen Pokemon DS Map Studio and try again.",
                                            "Problem saving generated file", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                ConverterErrorDialog dialog = new ConverterErrorDialog(frame);
                                dialog.init("There was a problem creating the NSBMD file. \n"
                                                + "The output from the converter is:",
                                        outputString);
                                dialog.setTitle("Problem generating file");
                                dialog.setLocationRelativeTo(frame);
                                dialog.setVisible(true);
                            }
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "The program \"g3dcvtr.exe\" is not found in the \"converter\" folder.\n"
                                            + "Put the program and its *.dll files in the folder and try again.",
                                    "Converter not found", JOptionPane.ERROR_MESSAGE);
                        } catch (InterruptedException ex) {
                            JOptionPane.showMessageDialog(frame, "The model was not converted",
                                    "Problem converting the model", JOptionPane.ERROR_MESSAGE);
                        }
                        return true;
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "There was a problem reading the IMD file",
                        "Error loading the IMD file", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    void saveMapBtxWithDialog() {
        final SystemFileChooser openChooser = new SystemFileChooser();
        openChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath) + ".imd"));
        if (handler.getLastMapDirectoryUsed() != null) {
            openChooser.setCurrentDirectory(new File(handler.getLastMapDirectoryUsed()));
        }
        openChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("IMD (*.imd)", "imd"));
        openChooser.setApproveButtonText("Open");
        openChooser.setDialogTitle("Open IMD Map for converting into NSBTX");
        final int returnVal = openChooser.showOpenDialog(frame);
        if (returnVal == SystemFileChooser.APPROVE_OPTION) {
            String imdPath = openChooser.getSelectedFile().getPath();

            final SystemFileChooser saveChooser = new SystemFileChooser();
            saveChooser.setSelectedFile(new File(Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)));
            saveChooser.setCurrentDirectory(openChooser.getSelectedFile().getParentFile());
            saveChooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("NSBTX (*.nsbtx)", "nsbtx"));
            saveChooser.setApproveButtonText("Save");
            saveChooser.setDialogTitle("Save");
            final int returnValSave = saveChooser.showSaveDialog(frame);

            if (returnValSave == SystemFileChooser.APPROVE_OPTION) {
                String nsbPath = saveChooser.getSelectedFile().getPath();
                String filename = new File(nsbPath).getName();

                System.out.println(filename);
                String converterPath = "converter/g3dcvtr.exe";
                String[] cmd = {converterPath, imdPath, "-etex", "-o", filename};
                Process process;
                try {
                    process = new ProcessBuilder(cmd).start();

                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    StringBuilder outputString = new StringBuilder();
                    String line;
                    while ((line = stdError.readLine()) != null) {
                        outputString.append(line).append("\n");
                    }

                    process.waitFor();
                    process.destroy();

                    if (!filename.endsWith("nsbtx")) {
                        filename += ".nsbtx";
                    }
                    if (!nsbPath.endsWith("nsbtx")) {
                        nsbPath += ".nsbtx";
                    }

                    System.out.println(System.getProperty("user.dir"));
                    File srcFile = new File(System.getProperty("user.dir") + "/" + filename);
                    File dstFile = new File(nsbPath);
                    if (srcFile.exists()) {
                        try {
                            Files.move(srcFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            try {
                                byte[] nsbtxData = Files.readAllBytes(dstFile.toPath());
                                Nsbtx2 nsbtx = NsbtxLoader2.loadNsbtx(nsbtxData);

                                ExportNsbtxResultDialog resultDialog = new ExportNsbtxResultDialog(frame, true);
                                resultDialog.init(nsbtx);
                                resultDialog.setLocationRelativeTo(frame);
                                resultDialog.setVisible(true);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, "NSBTX succesfully exported.",
                                        "NSBTX saved", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                    "File was not moved to the save directory. \n"
                                            + "Reopen Pokemon DS Map Studio and try again.",
                                    "Problem saving generated file", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        ConverterErrorDialog dialog = new ConverterErrorDialog(frame);
                        dialog.init("There was a problem creating the NSBTX file. \n"
                                        + "The output from the converter is:",
                                outputString.toString());
                        dialog.setTitle("Problem generating file");
                        dialog.setLocationRelativeTo(frame);
                        dialog.setVisible(true);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame,
                            "The program \"g3dcvtr.exe\" is not found in the \"converter\" folder.\n"
                                    + "Put the program and its *.dll files in the folder and try again.",
                            "Converter not found", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    JOptionPane.showMessageDialog(frame, "The model was not converted",
                            "Problem converting the model", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    void saveAreasAsBtxWithDialog() {
        final ExportNsbtxDialog configDialog = new ExportNsbtxDialog(frame, true);
        configDialog.init(handler);
        configDialog.setLocationRelativeTo(frame);
        configDialog.setVisible(true);

        if (configDialog.getReturnValue() == ExportImdDialog.APPROVE_OPTION) {
            ArrayList<Integer> areaIndices = configDialog.getSelectedAreaIndices();
            String nsbtxFolderPath = configDialog.getNsbtxFolderPath();

            final NsbtxOutputInfoDialog outputDialog = new NsbtxOutputInfoDialog(frame, true);
            outputDialog.init(handler, areaIndices, nsbtxFolderPath);
            outputDialog.setLocationRelativeTo(null);
            outputDialog.setVisible(true);
        }
    }
}
