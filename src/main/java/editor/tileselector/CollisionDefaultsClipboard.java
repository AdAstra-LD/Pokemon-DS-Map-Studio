package editor.tileselector;

import java.util.Arrays;
import tileset.Tile;

/** Shared collision-default clipboard used by tile menus and editor panels. */
public final class CollisionDefaultsClipboard {

    private static Snapshot allLayers;
    private static LayerSnapshot oneLayer;

    private CollisionDefaultsClipboard() {
    }

    public static boolean hasAllLayers() {
        return allLayers != null;
    }

    public static boolean hasLayer() {
        return oneLayer != null;
    }

    public static void copyAll(Tile tile, int numLayers) {
        int width = tile.getCollisionFootprintWidth();
        int height = tile.getCollisionFootprintHeight();
        int[][][] grids = new int[numLayers][width][height];
        for (int layer = 0; layer < numLayers; layer++) {
            copyGrid(tile.getCollisionDefaults().get(layer), grids[layer]);
        }
        allLayers = new Snapshot(grids, width, height,
                tile.getCollisionFootprintAnchorX(), tile.getCollisionFootprintAnchorY());
    }

    public static void copyAll(int[][][] grids, int width, int height,
                               int anchorX, int anchorY) {
        allLayers = new Snapshot(cloneGrids(grids), width, height, anchorX, anchorY);
    }

    public static void pasteAll(Tile tile, int numLayers) {
        if (allLayers == null) {
            return;
        }
        for (int layer = 0; layer < numLayers; layer++) {
            int[][] target = createEmptyGrid(tile.getCollisionFootprintWidth(),
                    tile.getCollisionFootprintHeight());
            if (layer < allLayers.grids.length) {
                pasteAligned(allLayers.grids[layer], allLayers.width, allLayers.height,
                        allLayers.anchorX, allLayers.anchorY, target,
                        tile.getCollisionFootprintAnchorX(),
                        tile.getCollisionFootprintAnchorY());
            }
            tile.getCollisionDefaults().put(layer, target);
        }
        tile.pruneEmptyCollisionDefaults();
    }

    public static void pasteAll(int[][][] target, int targetWidth, int targetHeight,
                                int targetAnchorX, int targetAnchorY) {
        if (allLayers == null) {
            return;
        }
        for (int layer = 0; layer < target.length; layer++) {
            for (int[] column : target[layer]) {
                Arrays.fill(column, -1);
            }
            if (layer < allLayers.grids.length) {
                pasteAligned(allLayers.grids[layer], allLayers.width, allLayers.height,
                        allLayers.anchorX, allLayers.anchorY, target[layer],
                        targetAnchorX, targetAnchorY);
            }
        }
    }

    public static void copyLayer(Tile tile, int layer) {
        int width = tile.getCollisionFootprintWidth();
        int height = tile.getCollisionFootprintHeight();
        int[][] grid = createEmptyGrid(width, height);
        copyGrid(tile.getCollisionDefaults().get(layer), grid);
        oneLayer = new LayerSnapshot(grid, width, height,
                tile.getCollisionFootprintAnchorX(), tile.getCollisionFootprintAnchorY());
    }

    public static void pasteLayer(Tile tile, int layer) {
        if (oneLayer == null) {
            return;
        }
        int[][] target = createEmptyGrid(tile.getCollisionFootprintWidth(),
                tile.getCollisionFootprintHeight());
        pasteAligned(oneLayer.grid, oneLayer.width, oneLayer.height,
                oneLayer.anchorX, oneLayer.anchorY, target,
                tile.getCollisionFootprintAnchorX(), tile.getCollisionFootprintAnchorY());
        tile.getCollisionDefaults().put(layer, target);
        tile.pruneEmptyCollisionDefaults();
    }

    private static void copyGrid(int[][] source, int[][] target) {
        for (int x = 0; x < target.length; x++) {
            Arrays.fill(target[x], -1);
            if (source != null && x < source.length && source[x] != null) {
                System.arraycopy(source[x], 0, target[x], 0,
                        Math.min(source[x].length, target[x].length));
            }
        }
    }

    private static void pasteAligned(int[][] source, int sourceWidth, int sourceHeight,
                                     int sourceAnchorX, int sourceAnchorY,
                                     int[][] target, int targetAnchorX, int targetAnchorY) {
        for (int x = 0; x < target.length; x++) {
            for (int y = 0; y < target[x].length; y++) {
                int sourceX = x - targetAnchorX + sourceAnchorX;
                int sourceY = y - targetAnchorY + sourceAnchorY;
                target[x][y] = sourceX >= 0 && sourceX < sourceWidth
                        && sourceY >= 0 && sourceY < sourceHeight
                        ? source[sourceX][sourceY] : -1;
            }
        }
    }

    private static int[][] createEmptyGrid(int width, int height) {
        int[][] grid = new int[width][height];
        for (int[] column : grid) {
            Arrays.fill(column, -1);
        }
        return grid;
    }

    private static int[][][] cloneGrids(int[][][] source) {
        int[][][] copy = new int[source.length][][];
        for (int layer = 0; layer < source.length; layer++) {
            copy[layer] = new int[source[layer].length][];
            for (int x = 0; x < source[layer].length; x++) {
                copy[layer][x] = source[layer][x].clone();
            }
        }
        return copy;
    }

    private static class Snapshot {
        final int[][][] grids;
        final int width;
        final int height;
        final int anchorX;
        final int anchorY;

        Snapshot(int[][][] grids, int width, int height, int anchorX, int anchorY) {
            this.grids = grids;
            this.width = width;
            this.height = height;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }
    }

    private static class LayerSnapshot {
        final int[][] grid;
        final int width;
        final int height;
        final int anchorX;
        final int anchorY;

        LayerSnapshot(int[][] grid, int width, int height, int anchorX, int anchorY) {
            this.grid = grid;
            this.width = width;
            this.height = height;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }
    }
}
