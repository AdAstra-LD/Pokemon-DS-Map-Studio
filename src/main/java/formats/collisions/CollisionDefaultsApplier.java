package formats.collisions;

import editor.grid.MapGrid;
import tileset.Tile;
import tileset.Tileset;

import java.util.Arrays;
import java.util.Map;

/** Resolves per-tile collision defaults into a map's collision layers. */
public final class CollisionDefaultsApplier {

    private CollisionDefaultsApplier() {
    }

    /**
     * Collects defaults in map grid coordinates, where y = 0 is the bottom
     * map row. Higher terrain layers override lower terrain layers.
     */
    public static int[][][] collectDefaults(Tileset tileset, MapGrid grid, int numCollisionLayers) {
        int[][][] values = new int[numCollisionLayers][Collisions.cols][Collisions.rows];
        for (int[][] layerValues : values) {
            for (int[] column : layerValues) {
                Arrays.fill(column, -1);
            }
        }

        for (int terrainLayer = 0; terrainLayer < grid.tileLayers.length; terrainLayer++) {
            for (int tileX = 0; tileX < Collisions.cols; tileX++) {
                for (int tileY = 0; tileY < Collisions.rows; tileY++) {
                    int tileIndex = grid.tileLayers[terrainLayer][tileX][tileY];
                    if (tileIndex < 0 || tileIndex >= tileset.size()) {
                        continue;
                    }
                    Tile tile = tileset.get(tileIndex);
                    int anchorX = tile.getCollisionFootprintAnchorX();
                    int anchorY = tile.getCollisionFootprintAnchorY();
                    for (Map.Entry<Integer, int[][]> entry : tile.getCollisionDefaults().entrySet()) {
                        int collisionLayer = entry.getKey();
                        if (collisionLayer < 0 || collisionLayer >= numCollisionLayers) {
                            continue;
                        }
                        int[][] defaults = entry.getValue();
                        for (int x = 0; x < defaults.length; x++) {
                            for (int y = 0; y < defaults[x].length; y++) {
                                int value = defaults[x][y];
                                int mapX = tileX + x - anchorX;
                                int mapY = tileY + anchorY - y;
                                if (value >= 0 && mapX >= 0 && mapX < Collisions.cols
                                        && mapY >= 0 && mapY < Collisions.rows) {
                                    values[collisionLayer][mapX][mapY] = value;
                                }
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    /** Applies all defined defaults and returns the number of changed cells. */
    public static int apply(Tileset tileset, MapGrid grid, Collisions collisions) {
        int[][][] values = collectDefaults(tileset, grid, collisions.getNumLayers());
        int changedCells = 0;
        for (int layer = 0; layer < values.length; layer++) {
            for (int x = 0; x < Collisions.cols; x++) {
                for (int mapY = 0; mapY < Collisions.rows; mapY++) {
                    int value = values[layer][x][mapY];
                    if (value < 0) {
                        continue;
                    }
                    int collisionY = Collisions.rows - 1 - mapY;
                    if (collisions.getValue(layer, x, collisionY) != value) {
                        collisions.setValue(value, layer, x, collisionY);
                        changedCells++;
                    }
                }
            }
        }
        return changedCells;
    }
}
