package tileset;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import editor.smartdrawing.SmartGrid;

/** Portable, additive palette-folder bundles. */
public final class PaletteFolderBundleIO {

    public static final String EXTENSION = "pdsfolder";
    private static final String TILESET_NAME = "folder.pdsts";
    private static final String ROOT_NAME = "root.txt";

    private PaletteFolderBundleIO() {
    }

    public static void write(File output, Tileset source, PaletteFolder root) throws IOException {
        Path temp = Files.createTempDirectory("pdsms-folder-export-");
        try {
            Tileset subset = createSubset(source, root);
            Path tilesetPath = temp.resolve(TILESET_NAME);
            TilesetIO.writeTilesetToFile(tilesetPath.toString(), subset);
            subset.saveImagesToFile(temp.toString());
            Files.write(temp.resolve(ROOT_NAME), root.getPath().getBytes(StandardCharsets.UTF_8));
            zipDirectory(temp, output.toPath());
        } finally {
            deleteTree(temp);
        }
    }

    public static int read(File input, Tileset target) throws Exception {
        Path temp = Files.createTempDirectory("pdsms-folder-import-");
        try {
            unzip(input.toPath(), temp);
            Path tilesetPath = temp.resolve(TILESET_NAME);
            Path rootPath = temp.resolve(ROOT_NAME);
            if (!Files.isRegularFile(tilesetPath) || !Files.isRegularFile(rootPath)) {
                throw new IOException("This is not a valid PDSMS folder bundle.");
            }
            String sourceRoot = new String(Files.readAllBytes(rootPath), StandardCharsets.UTF_8).trim();
            Tileset imported = TilesetIO.readTilesetFromFile(tilesetPath.toString());
            String targetRoot = uniqueRootPath(target, sourceRoot);
            Map<String, String> folderPaths = importFolders(imported, target, sourceRoot, targetRoot);

            int added = 0;
            Map<Integer, Integer> importedTileIndices = new HashMap<>();
            for (int sourceIndex = 0; sourceIndex < imported.size(); sourceIndex++) {
                Tile sourceTile = imported.get(sourceIndex);
                int existingIndex = target.indexOfTileVisualData(sourceTile);
                Tile targetTile;
                if (existingIndex >= 0) {
                    targetTile = target.get(existingIndex);
                } else {
                    targetTile = sourceTile.clone();
                    targetTile.setPaletteFolder(PaletteFolder.UNSORTED);
                    target.importTile(targetTile);
                    existingIndex = target.size() - 1;
                    added++;
                }
                importedTileIndices.put(sourceIndex, existingIndex);
                for (Map.Entry<String, Integer> membership
                        : sourceTile.getPaletteFolderSlots().entrySet()) {
                    String mapped = folderPaths.get(membership.getKey());
                    if (mapped != null) {
                        targetTile.addPaletteFolder(mapped, membership.getValue());
                    }
                }
            }
            for (SmartGrid sourceGrid : imported.getSmartGridArray()) {
                int[][] mapped = new int[SmartGrid.width][SmartGrid.height];
                for (int x = 0; x < SmartGrid.width; x++) {
                    for (int y = 0; y < SmartGrid.height; y++) {
                        Integer targetIndex = importedTileIndices.get(sourceGrid.sgrid[x][y]);
                        mapped[x][y] = targetIndex == null ? -1 : targetIndex;
                    }
                }
                SmartGrid targetGrid = new SmartGrid(mapped);
                String mappedFolder = folderPaths.get(sourceGrid.getPaletteFolder());
                targetGrid.setPaletteFolder(mappedFolder == null ? "" : mappedFolder);
                int insert = target.getSmartGridArray().size();
                for (int i = 0; i < target.getSmartGridArray().size(); i++) {
                    if (targetGrid.getPaletteFolder().equals(
                            target.getSmartGridArray().get(i).getPaletteFolder())) {
                        insert = i + 1;
                    }
                }
                target.getSmartGridArray().add(insert, targetGrid);
            }
            return added;
        } finally {
            deleteTree(temp);
        }
    }

    private static Tileset createSubset(Tileset source, PaletteFolder root) {
        Tileset subset = new Tileset();
        String prefix = root.getPath() + "/";
        for (PaletteFolder folder : source.getPaletteFolders()) {
            if (folder.getPath().equals(root.getPath()) || folder.getPath().startsWith(prefix)) {
                subset.getPaletteFolders().add(copyFolder(folder));
            }
        }
        LinkedHashSet<Integer> includedTiles = new LinkedHashSet<>();
        ArrayList<SmartGrid> includedGrids = new ArrayList<>();
        for (SmartGrid grid : source.getSmartGridArray()) {
            String path = grid.getPaletteFolder();
            if (path.equals(root.getPath()) || path.startsWith(prefix)) {
                includedGrids.add(grid);
                for (int x = 0; x < SmartGrid.width; x++) {
                    for (int y = 0; y < SmartGrid.height; y++) {
                        if (grid.sgrid[x][y] >= 0 && grid.sgrid[x][y] < source.size()) {
                            includedTiles.add(grid.sgrid[x][y]);
                        }
                    }
                }
            }
        }
        for (int sourceIndex = 0; sourceIndex < source.size(); sourceIndex++) {
            Tile sourceTile = source.get(sourceIndex);
            for (String path : sourceTile.getPaletteFolderSlots().keySet()) {
                if (path.equals(root.getPath()) || path.startsWith(prefix)) {
                    includedTiles.add(sourceIndex);
                    break;
                }
            }
        }
        Map<Integer, Integer> tileIndices = new HashMap<>();
        for (int sourceIndex : includedTiles) {
            Tile sourceTile = source.get(sourceIndex);
            Tile copy = sourceTile.clone();
            copy.setPaletteFolder(PaletteFolder.UNSORTED);
            for (Map.Entry<String, Integer> membership
                    : sourceTile.getPaletteFolderSlots().entrySet()) {
                String path = membership.getKey();
                if (path.equals(root.getPath()) || path.startsWith(prefix)) {
                    copy.addPaletteFolder(path, membership.getValue());
                }
            }
            subset.importTile(copy);
            tileIndices.put(sourceIndex, subset.size() - 1);
        }
        for (SmartGrid sourceGrid : includedGrids) {
            int[][] mapped = new int[SmartGrid.width][SmartGrid.height];
            for (int x = 0; x < SmartGrid.width; x++) {
                for (int y = 0; y < SmartGrid.height; y++) {
                    Integer subsetIndex = tileIndices.get(sourceGrid.sgrid[x][y]);
                    mapped[x][y] = subsetIndex == null ? -1 : subsetIndex;
                }
            }
            SmartGrid copy = new SmartGrid(mapped);
            copy.setPaletteFolder(sourceGrid.getPaletteFolder());
            subset.getSmartGridArray().add(copy);
        }
        return subset;
    }

    private static PaletteFolder copyFolder(PaletteFolder source) {
        PaletteFolder copy = new PaletteFolder(source.getPath());
        copy.setColumns(source.getColumns());
        copy.setRows(source.getRows());
        copy.setCollapsed(source.isCollapsed());
        copy.setPinned(false);
        copy.setPinnedViewportHeight(source.getPinnedViewportHeight());
        copy.setGridLinesVisible(source.isGridLinesVisible());
        copy.setSmartDrawingsCollapsed(source.areSmartDrawingsCollapsed());
        return copy;
    }

    private static Map<String, String> importFolders(Tileset source, Tileset target,
            String sourceRoot, String targetRoot) {
        Map<String, String> paths = new HashMap<>();
        String prefix = sourceRoot + "/";
        for (PaletteFolder folder : source.getPaletteFolders()) {
            if (!folder.getPath().equals(sourceRoot) && !folder.getPath().startsWith(prefix)) {
                continue;
            }
            String suffix = folder.getPath().substring(sourceRoot.length());
            String path = targetRoot + suffix;
            PaletteFolder added = target.getOrCreatePaletteFolderWithParents(path);
            added.setColumns(folder.getColumns());
            added.setRows(folder.getRows());
            added.setCollapsed(folder.isCollapsed());
            added.setPinnedViewportHeight(folder.getPinnedViewportHeight());
            added.setGridLinesVisible(folder.isGridLinesVisible());
            added.setSmartDrawingsCollapsed(folder.areSmartDrawingsCollapsed());
            paths.put(folder.getPath(), path);
        }
        return paths;
    }

    private static String uniqueRootPath(Tileset target, String sourceRoot) {
        if (target.getPaletteFolder(sourceRoot) == null) {
            return sourceRoot;
        }
        String parent = Tileset.getParentFolderPath(sourceRoot);
        String leaf = parent == null ? sourceRoot : sourceRoot.substring(parent.length() + 1);
        String base = leaf + " (Imported)";
        String candidate = parent == null ? base : parent + "/" + base;
        int number = 2;
        while (target.getPaletteFolder(candidate) != null) {
            String numbered = base + " " + number++;
            candidate = parent == null ? numbered : parent + "/" + numbered;
        }
        return candidate;
    }

    private static void zipDirectory(Path directory, Path output) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(output.toFile())))) {
            Files.walk(directory).filter(Files::isRegularFile).forEach(path -> {
                try {
                    ZipEntry entry = new ZipEntry(directory.relativize(path).toString().replace('\\', '/'));
                    zip.putNextEntry(entry);
                    Files.copy(path, zip);
                    zip.closeEntry();
                } catch (IOException ex) {
                    throw new ZipRuntimeException(ex);
                }
            });
        } catch (ZipRuntimeException ex) {
            throw ex.ioException;
        }
    }

    private static void unzip(Path input, Path directory) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(input.toFile())))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path output = directory.resolve(entry.getName()).normalize();
                if (!output.startsWith(directory)) {
                    throw new IOException("Unsafe path in folder bundle.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    Files.copy(zip, output);
                }
                zip.closeEntry();
            }
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walk(root).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException ignored) {
        }
    }

    private static class ZipRuntimeException extends RuntimeException {
        final IOException ioException;

        ZipRuntimeException(IOException ioException) {
            this.ioException = ioException;
        }
    }
}
