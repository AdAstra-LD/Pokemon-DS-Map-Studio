
package tileset;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the tileset metadata sidecar file: palette folders, tile
 * names, layout template slots and smart collision defaults. The sidecar
 * lives next to the tileset as "name.pdsts.meta" so the .pdsts format stays
 * fully compatible with stock Pokemon DS Map Studio.
 *
 * Line based text format (fields separated by '|'):
 *   folder|path|columns|rows|collapsed|pinned
 *   tile|index|name|folder|slot|displayWidth|displayHeight|
 *        footprintWidth|footprintHeight|anchorX|anchorY|collisionDefaults
 *
 * collisionDefaults entries are separated by ';'. Their per-cell encoding is
 * "layer,cellX,cellY,hexValue" with cellY = 0 at the top of the tile image.
 * The old v1 encoding "layer:hexValue" (whole tile) is still read and
 * expanded to every cell.
 */
public class TileMetadataIO {

    public static final String fileExtension = "meta";
    private static final String HEADER = "# Pokemon DS Map Studio tile metadata v4";

    public static String getMetadataPath(String tilesetPath) {
        return tilesetPath + "." + fileExtension;
    }

    /**
     * Writes the sidecar next to the tileset file. When the tileset has no
     * metadata at all, an existing sidecar is deleted instead.
     */
    public static void write(String tilesetPath, Tileset tset) {
        File file = new File(getMetadataPath(tilesetPath));
        if (!hasContent(tset)) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }
        try {
            writeFile(file.getPath(), tset);
        } catch (IOException ex) {
            System.err.println("Could not save tile metadata: " + ex.getMessage());
        }
    }

    /** Writes a standalone metadata file chosen by the user. */
    public static void writeFile(String metadataPath, Tileset tset) throws IOException {
        try (PrintWriter out = new PrintWriter(metadataPath, StandardCharsets.UTF_8.name())) {
            out.println(HEADER);
            for (PaletteFolder folder : tset.getPaletteFolders()) {
                out.println("folder|" + escape(folder.getPath())
                        + "|" + folder.getColumns()
                        + "|" + folder.getRows()
                        + "|" + (folder.isCollapsed() ? 1 : 0)
                        + "|" + (folder.isPinned() ? 1 : 0)
                        + "|" + (folder.isGridLinesVisible() ? 1 : 0));
            }
            for (int i = 0; i < tset.size(); i++) {
                Tile tile = tset.get(i);
                if (!tile.hasPaletteMetadata()) {
                    continue;
                }
                StringBuilder coll = new StringBuilder();
                for (Map.Entry<Integer, int[][]> entry : tile.getCollisionDefaults().entrySet()) {
                    int[][] grid = entry.getValue();
                    for (int x = 0; x < grid.length; x++) {
                        for (int y = 0; y < grid[x].length; y++) {
                            if (grid[x][y] < 0) {
                                continue;
                            }
                            if (coll.length() > 0) {
                                coll.append(';');
                            }
                            coll.append(entry.getKey()).append(',').append(x).append(',').append(y)
                                    .append(',').append(String.format("%02X", grid[x][y]));
                        }
                    }
                }
                out.println("tile|" + i
                        + "|" + escape(tile.getPaletteName())
                        + "|" + escape(tile.getPaletteFolder())
                        + "|" + tile.getPaletteSlot()
                        + "|" + (tile.hasPaletteDisplaySize() ? tile.getPaletteDisplayWidth() : 0)
                        + "|" + (tile.hasPaletteDisplaySize() ? tile.getPaletteDisplayHeight() : 0)
                        + "|" + tile.getStoredCollisionFootprintWidth()
                        + "|" + tile.getStoredCollisionFootprintHeight()
                        + "|" + tile.getStoredCollisionFootprintAnchorX()
                        + "|" + tile.getStoredCollisionFootprintAnchorY()
                        + "|" + coll);
                for (Map.Entry<String, Integer> membership
                        : tile.getPaletteFolderSlots().entrySet()) {
                    out.println("membership|" + i + "|" + escape(membership.getKey())
                            + "|" + membership.getValue());
                }
            }
            if (out.checkError()) {
                throw new IOException("Could not finish writing the metadata file.");
            }
        }
    }

    private static boolean hasContent(Tileset tset) {
        if (!tset.getPaletteFolders().isEmpty()) {
            return true;
        }
        for (Tile tile : tset.getTiles()) {
            if (tile.hasPaletteMetadata()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads the sidecar next to the tileset file into the tileset's tiles and
     * folder list. Missing or malformed files/lines are silently skipped, so
     * tilesets without metadata keep working exactly as before.
     */
    public static void read(String tilesetPath, Tileset tset) {
        File file = new File(getMetadataPath(tilesetPath));
        if (!file.exists()) {
            return;
        }
        try {
            readFile(file.getPath(), tset, false);
        } catch (IOException ex) {
            System.err.println("Could not load tile metadata: " + ex.getMessage());
        }
    }

    /** Loads a standalone metadata file, optionally replacing current metadata. */
    public static void readFile(String metadataPath, Tileset tset, boolean replace)
            throws IOException {
        List<String> lines = Files.readAllLines(new File(metadataPath).toPath(),
                StandardCharsets.UTF_8);
        boolean recognized = false;
        for (String line : lines) {
            if (line.startsWith("# Pokemon DS Map Studio tile metadata v")
                    || line.startsWith("folder|") || line.startsWith("tile|")
                    || line.startsWith("membership|")) {
                recognized = true;
                break;
            }
        }
        if (!recognized) {
            throw new IOException("The selected file is not PDSMS tile metadata.");
        }
        if (replace) {
            clear(tset);
        }
        for (String line : lines) {
            try {
                parseLine(line, tset, !replace);
            } catch (RuntimeException ex) {
                System.err.println("Skipping bad tile metadata line: " + line);
            }
        }
    }

    private static void clear(Tileset tset) {
        tset.getPaletteFolders().clear();
        for (Tile tile : tset.getTiles()) {
            tile.getCollisionDefaults().clear();
            tile.resetCollisionFootprint();
            tile.setPaletteName("");
            tile.setPaletteFolder(PaletteFolder.UNSORTED);
            tile.setPaletteSlot(-1);
            tile.setPaletteDisplayWidth(0);
            tile.setPaletteDisplayHeight(0);
        }
    }

    private static void parseLine(String line, Tileset tset, boolean merge) {
        String[] fields = line.split("\\|", -1);
        if (fields.length >= 4 && fields[0].equals("folder")) {
            String path = unescape(fields[1]);
            PaletteFolder existing = tset.getPaletteFolder(path);
            PaletteFolder folder = tset.getOrCreatePaletteFolder(path);
            if (merge && existing != null) {
                return;
            }
            int columns = Integer.parseInt(fields[2]);
            folder.setColumns(columns);
            if (!folder.getPath().isEmpty() && columns == 0) {
                //v3 used columns=0 to hide the grid and accidentally discarded layout slots.
                folder.setColumns(PaletteFolder.DEFAULT_COLUMNS);
                folder.setGridLinesVisible(false);
            }
            if (fields.length >= 5) {
                folder.setRows(Integer.parseInt(fields[3]));
                folder.setCollapsed(fields[4].equals("1"));
                if (fields.length >= 6) {
                    folder.setPinned(fields[5].equals("1"));
                }
                if (fields.length >= 7) {
                    folder.setGridLinesVisible(fields[6].equals("1"));
                }
            } else {
                folder.setRows(PaletteFolder.DEFAULT_ROWS);
                folder.setCollapsed(fields[3].equals("1"));
            }
        } else if (fields.length >= 4 && fields[0].equals("membership")) {
            int index = Integer.parseInt(fields[1]);
            if (index >= 0 && index < tset.size()) {
                String folderPath = unescape(fields[2]);
                tset.getOrCreatePaletteFolderWithParents(folderPath);
                tset.get(index).addPaletteFolder(folderPath, Integer.parseInt(fields[3]));
            }
        } else if (fields.length >= 6 && fields[0].equals("tile")) {
            int index = Integer.parseInt(fields[1]);
            if (index < 0 || index >= tset.size()) {
                return;
            }
            Tile tile = tset.get(index);
            if (!merge || tile.getPaletteName().isEmpty()) {
                tile.setPaletteName(unescape(fields[2]));
            }
            String folderPath = unescape(fields[3]);
            if (!folderPath.isEmpty()) {
                //Folders referenced by tiles always exist in the folder list
                tset.getOrCreatePaletteFolder(folderPath);
            }
            int importedSlot = Integer.parseInt(fields[4]);
            if (merge) {
                tile.addPaletteFolder(folderPath, importedSlot);
            } else {
                tile.setPaletteFolder(folderPath);
                tile.setPaletteSlot(importedSlot);
            }
            int collisionFieldIndex = 5;
            if (fields.length >= 12) {
                if (!merge || !tile.hasPaletteDisplaySize()) {
                    tile.setPaletteDisplayWidth(Integer.parseInt(fields[5]));
                    tile.setPaletteDisplayHeight(Integer.parseInt(fields[6]));
                }
                int footprintWidth = Integer.parseInt(fields[7]);
                int footprintHeight = Integer.parseInt(fields[8]);
                if (footprintWidth > 0 && footprintHeight > 0
                        && (!merge || !tile.hasCustomCollisionFootprint())) {
                    tile.setCollisionFootprint(footprintWidth, footprintHeight,
                            Integer.parseInt(fields[9]), Integer.parseInt(fields[10]));
                }
                collisionFieldIndex = 11;
            } else if (fields.length >= 8) {
                if (!merge || !tile.hasPaletteDisplaySize()) {
                    tile.setPaletteDisplayWidth(Integer.parseInt(fields[5]));
                    tile.setPaletteDisplayHeight(Integer.parseInt(fields[6]));
                }
                collisionFieldIndex = 7;
            }
            if (!fields[collisionFieldIndex].isEmpty()) {
                for (String chunk : fields[collisionFieldIndex].split(";")) {
                    if (chunk.contains(":")) {
                        //v1 whole-tile entries ("layer:hex", comma separated)
                        for (String pair : chunk.split(",")) {
                            String[] parts = pair.split(":");
                            if (parts.length != 2) {
                                continue;
                            }
                            int value = Integer.parseInt(parts[1], 16) & 0xFF;
                            int[][] grid = tile.getOrCreateCollisionDefaultGrid(Integer.parseInt(parts[0]));
                            for (int[] column : grid) {
                                java.util.Arrays.fill(column, value);
                            }
                        }
                    } else {
                        //v2 per-cell entry: "layer,cellX,cellY,hex"
                        String[] parts = chunk.split(",");
                        if (parts.length != 4) {
                            continue;
                        }
                        int[][] grid = tile.getOrCreateCollisionDefaultGrid(Integer.parseInt(parts[0]));
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        if (x >= 0 && x < grid.length && y >= 0 && y < grid[0].length) {
                            grid[x][y] = Integer.parseInt(parts[3], 16) & 0xFF;
                        }
                    }
                }
            }
        }
    }

    //'|' separates fields and would corrupt the line format
    private static String escape(String s) {
        return s.replace("|", "%7C");
    }

    private static String unescape(String s) {
        return s.replace("%7C", "|").replace("\u00C2\u00A6", "|");
    }
}
