
package editor.smartdrawing;

import editor.handler.MapEditorHandler;
import editor.grid.MapGrid;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tileset.Tile;

/**
 * @author Trifindo
 */
public class SmartGrid {

    public static final int width = 5;
    public static final int height = 3;
    public int[][] sgrid = new int[width][height];

    private static final List<SmartUnit> smartUnits = new ArrayList<SmartUnit>() {
        {
            add(new SmartUnit(true, false, false, true, false, false, false, false));
            add(new SmartUnit(true, false, true, true, false, false, false, false));
            add(new SmartUnit(true, false, true, false, false, false, false, false));
            add(new SmartUnit(true, true, true, true, true, false, true, true));
            add(new SmartUnit(true, true, true, true, false, true, true, true));

            add(new SmartUnit(true, true, false, true, false, false, false, false));
            add(new SmartUnit(true, true, true, true, true, true, true, true));
            add(new SmartUnit(true, true, true, false, false, false, false, false));
            add(new SmartUnit(true, true, true, true, true, true, true, false));
            add(new SmartUnit(true, true, true, true, true, true, false, true));

            add(new SmartUnit(false, true, false, true, false, false, false, false));
            add(new SmartUnit(false, true, true, true, false, false, false, false));
            add(new SmartUnit(false, true, true, false, false, false, false, false));
        }
    };

    private static final List<SmartUnit> invertedSmartUnits = new ArrayList<SmartUnit>() {
        {
            add(new SmartUnit(true, true, true, true, true, false, true, true));//3
            add(new SmartUnit(false, true, true, true, false, false, false, false));//12
            add(new SmartUnit(true, true, true, true, false, true, true, true));//4
            add(new SmartUnit(true, false, false, true, false, false, false, false));//0
            add(new SmartUnit(true, false, true, false, false, false, false, false));//2

            add(new SmartUnit(true, true, true, false, false, false, false, false));//7
            add(new SmartUnit(true, true, true, true, true, true, true, true));//6
            add(new SmartUnit(true, true, false, true, false, false, false, false));//5
            add(new SmartUnit(false, true, false, true, false, false, false, false));//11
            add(new SmartUnit(false, true, true, false, false, false, false, false));//13

            add(new SmartUnit(true, true, true, true, true, true, true, false));//8
            add(new SmartUnit(true, false, true, true, false, false, false, false));//1
            add(new SmartUnit(true, true, true, true, true, true, false, true));//9

        }
    };

    public SmartGrid(int[][] data) {
        this.sgrid = data;
    }

    public SmartGrid() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                sgrid[i][j] = -1;
            }
        }

        /*
        smartUnits.add(new SmartUnit(true, false, false, true, false, false, false, false));
        smartUnits.add(new SmartUnit(true, false, true, true, false, false, false, false));
        smartUnits.add(new SmartUnit(true, false, true, false, false, false, false, false));
        smartUnits.add(new SmartUnit(true, true, true, true, true, false, true, true));
        smartUnits.add(new SmartUnit(true, true, true, true, false, true, true, true));

        smartUnits.add(new SmartUnit(true, true, false, true, false, false, false, false));
        smartUnits.add(new SmartUnit(true, true, true, true, true, true, true, true));
        smartUnits.add(new SmartUnit(true, true, true, false, false, false, false, false));
        smartUnits.add(new SmartUnit(true, true, true, true, true, true, true, false));
        smartUnits.add(new SmartUnit(true, true, true, true, true, true, false, true));

        smartUnits.add(new SmartUnit(false, true, false, true, false, false, false, false));
        smartUnits.add(new SmartUnit(false, true, true, true, false, false, false, false));
        smartUnits.add(new SmartUnit(false, true, true, false, false, false, false, false));
         */
    }

    public void replaceTilesUsingIndices(int[] indices) {
        int[][] oldGrid = cloneGrid();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int index = oldGrid[i][j];
                if (index == -1) {
                    sgrid[i][j] = -1;
                } else {
                    sgrid[i][j] = indices[index];
                }
            }
        }
    }

    public int[][] cloneGrid() {
        int[][] copy = new int[width][height];
        for (int i = 0; i < width; i++) {
            System.arraycopy(sgrid[i], 0, copy[i], 0, height);
        }
        return copy;
    }

    /** Returns every tile ID used by this Smart Drawing template. */
    public Set<Integer> getTileIndices() {
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (sgrid[i][j] >= 0) {
                    indices.add(sgrid[i][j]);
                }
            }
        }
        return indices;
    }

    public boolean containsTile(int tileIndex) {
        return tileIndex >= 0 && getTileIndices().contains(tileIndex);
    }

    /**
     * Resolves an ordered one-cell-wide path. The selected template tile sets
     * the side/orientation at the beginning of the stroke, then straight and
     * corner pieces follow the path as it turns.
     */
    public int[][] resolvePath(List<Point> cells, int selectedTileIndex,
                               int outputWidth, int outputHeight) {
        return resolvePath(cells, selectedTileIndex, outputWidth, outputHeight, false);
    }

    public int[][] resolvePath(List<Point> cells, int selectedTileIndex,
                               int outputWidth, int outputHeight, boolean invert) {
        int[][] resolved = new int[outputWidth][outputHeight];
        for (int[] column : resolved) {
            Arrays.fill(column, -1);
        }
        if (cells == null || cells.isEmpty()) {
            return resolved;
        }

        LinkedHashSet<Point> unique = new LinkedHashSet<>();
        for (Point cell : cells) {
            if (cell != null && cell.x >= 0 && cell.x < outputWidth
                    && cell.y >= 0 && cell.y < outputHeight) {
                unique.add(new Point(cell));
            }
        }
        ArrayList<Point> path = new ArrayList<>(unique);
        if (path.isEmpty()) {
            return resolved;
        }

        int selectedSlot = findTileSlot(selectedTileIndex);
        int[] inward = getPathInwardVector(selectedSlot);
        if (path.size() == 1 || inward == null) {
            int tile = invert
                    ? tileAtSlotOrFallback(invertedSlot(selectedSlot), selectedTileIndex)
                    : selectedTileIndex;
            for (Point cell : path) {
                resolved[cell.x][cell.y] = tile;
            }
            return resolved;
        }

        int[] initialDirection = direction(path.get(0), path.get(1));
        int leftX = -initialDirection[1];
        int leftY = initialDirection[0];
        boolean interiorOnLeft = leftX * inward[0] + leftY * inward[1] >= 0;
        if (invert) {
            interiorOnLeft = !interiorOnLeft;
        }

        for (int i = 0; i < path.size(); i++) {
            Point cell = path.get(i);
            int slot;
            if (i == 0) {
                //Left uses the chosen seed; right uses its exact counterpart.
                resolved[cell.x][cell.y] = invert
                        ? tileAtSlotOrFallback(invertedSlot(selectedSlot), selectedTileIndex)
                        : selectedTileIndex;
                continue;
            } else if (i == path.size() - 1) {
                int[] incoming = direction(path.get(i - 1), cell);
                slot = edgeSlot(incoming, interiorOnLeft);
            } else {
                int[] incoming = direction(path.get(i - 1), cell);
                int[] outgoing = direction(cell, path.get(i + 1));
                int cross = incoming[0] * outgoing[1] - incoming[1] * outgoing[0];
                if (cross == 0) {
                    slot = edgeSlot(outgoing, interiorOnLeft);
                } else {
                    int outer = outerCornerSlot(-incoming[0], -incoming[1],
                            outgoing[0], outgoing[1]);
                    boolean turnsTowardInterior = interiorOnLeft ? cross > 0 : cross < 0;
                    slot = turnsTowardInterior ? outer : innerCornerForOuter(outer);
                }
            }
            resolved[cell.x][cell.y] = tileAtSlotOrFallback(slot, selectedTileIndex);
        }
        return resolved;
    }

    private static int invertedSlot(int slot) {
        switch (slot) {
            case 0: return 3;
            case 1: return 11;
            case 2: return 4;
            case 3: return 0;
            case 4: return 2;
            case 5: return 7;
            case 7: return 5;
            case 8: return 10;
            case 9: return 12;
            case 10: return 8;
            case 11: return 1;
            case 12: return 9;
            default: return slot;
        }
    }

    private int findTileSlot(int tileIndex) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (sgrid[x][y] == tileIndex) {
                    return y * width + x;
                }
            }
        }
        return -1;
    }

    private int tileAtSlotOrFallback(int slot, int fallback) {
        if (slot < 0 || slot >= width * height) {
            return fallback;
        }
        int tile = sgrid[slot % width][slot / width];
        return tile >= 0 ? tile : fallback;
    }

    /** Interior direction in map coordinates, where positive Y is north/up. */
    private static int[] getPathInwardVector(int slot) {
        switch (slot) {
            case 0:  return new int[]{1, -1};
            case 1:  return new int[]{0, -1};
            case 2:  return new int[]{-1, -1};
            case 3:  return new int[]{-1, 1};
            case 4:  return new int[]{1, 1};
            case 5:  return new int[]{1, 0};
            case 7:  return new int[]{-1, 0};
            case 8:  return new int[]{-1, -1};
            case 9:  return new int[]{1, -1};
            case 10: return new int[]{1, 1};
            case 11: return new int[]{0, 1};
            case 12: return new int[]{-1, 1};
            default: return null;
        }
    }

    private static int[] direction(Point from, Point to) {
        return new int[]{Integer.compare(to.x, from.x), Integer.compare(to.y, from.y)};
    }

    private static int edgeSlot(int[] direction, boolean interiorOnLeft) {
        int normalX = interiorOnLeft ? -direction[1] : direction[1];
        int normalY = interiorOnLeft ? direction[0] : -direction[0];
        if (normalX > 0) return 5;
        if (normalX < 0) return 7;
        if (normalY > 0) return 11;
        return 1;
    }

    private static int outerCornerSlot(int ax, int ay, int bx, int by) {
        boolean left = ax < 0 || bx < 0;
        boolean right = ax > 0 || bx > 0;
        boolean up = ay > 0 || by > 0;
        boolean down = ay < 0 || by < 0;
        if (right && down) return 0;
        if (left && down) return 2;
        if (right && up) return 10;
        if (left && up) return 12;
        return 6;
    }

    private static int innerCornerForOuter(int outerSlot) {
        switch (outerSlot) {
            case 0: return 3;
            case 2: return 4;
            case 10: return 8;
            case 12: return 9;
            default: return 6;
        }
    }

    /**
     * Resolves an arbitrary occupied-cell mask into this template's tile IDs.
     * Cells outside the mask are returned as -1.
     */
    public int[][] resolveMask(boolean[][] mask, boolean invert) {
        if (mask == null || mask.length == 0 || mask[0].length == 0) {
            return new int[0][0];
        }
        int[][] resolved = new int[mask.length][mask[0].length];
        for (int[] column : resolved) {
            Arrays.fill(column, -1);
        }
        resolveInto(resolved, mask, mask, invert, false);
        return resolved;
    }

    public void useSmartFill(MapEditorHandler handler, int x, int y, boolean invert) {
        int[][] screen = handler.getActiveTileLayer();
        int prevC = screen[x][y];
        boolean[][] gridToEdit = new boolean[MapGrid.cols][MapGrid.rows];
        for (int i = 0; i < MapGrid.cols; i++) {
            for (int j = 0; j < MapGrid.rows; j++) {
                gridToEdit[i][j] = screen[i][j] == prevC;
            }
        }

        for (int i = 0; i < MapGrid.cols; i++) {
            for (int j = 0; j < MapGrid.rows; j++) {
                int tileIndex = screen[i][j];
                if (tileIndex != -1 && tileIndex != prevC) {
                    Tile tile = handler.getTileset().get(tileIndex);
                    int xSize = tile.getWidth() - Math.max(0, i + tile.getWidth() - MapGrid.cols);
                    int ySize = tile.getHeight() - Math.max(0, j + tile.getHeight() - MapGrid.rows);
                    for (int m = 0; m < xSize; m++) {
                        for (int n = 0; n < ySize; n++) {
                            gridToEdit[i + m][j + n] = false;
                        }
                    }
                }
            }
        }

        boolean[][] connectedRegion = computeConnectedRegion(gridToEdit, x, y);
        //Use the full eligible mask for neighbour checks to preserve the
        //legacy treatment of diagonally touching cells, but only rewrite the
        //four-way connected region that was clicked.
        resolveInto(screen, gridToEdit, connectedRegion, invert, true);
    }

    private void resolveInto(int[][] target, boolean[][] topology, boolean[][] cellsToWrite,
                             boolean invert, boolean outsideConnected) {
        List<SmartUnit> units = invert ? invertedSmartUnits : smartUnits;
        int w = topology.length;
        int h = topology[0].length;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!cellsToWrite[x][y]) {
                    continue;
                }
                SmartUnit unit = generateSmartUnit(topology, x, y, outsideConnected);
                int index = unit.fullCrossNeighbours()
                        ? indexOfSameCorner(units, unit)
                        : indexOfSameCross(units, unit);
                if (index < 0) {
                    index = 6;
                }
                target[x][y] = sgrid[index % width][index / width];
            }
        }
    }

    private static boolean[][] computeConnectedRegion(boolean[][] eligible, int startX, int startY) {
        int w = eligible.length;
        int h = eligible[0].length;
        boolean[][] connected = new boolean[w][h];
        if (startX < 0 || startX >= w || startY < 0 || startY >= h || !eligible[startX][startY]) {
            return connected;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        connected[startX][startY] = true;
        int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cell = queue.removeFirst();
            for (int[] delta : deltas) {
                int nx = cell[0] + delta[0];
                int ny = cell[1] + delta[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h
                        && eligible[nx][ny] && !connected[nx][ny]) {
                    connected[nx][ny] = true;
                    queue.addLast(new int[]{nx, ny});
                }
            }
        }
        return connected;
    }

    private SmartUnit generateSmartUnit(boolean[][] screen, int x, int y, boolean outsideConnected) {
        int w = screen.length;
        int h = screen[0].length;
        boolean[] corners = new boolean[4];
        boolean[] cross = new boolean[4];
        cross[0] = hasSameNeighbour(screen, x, y - 1, w, h, outsideConnected);
        cross[1] = hasSameNeighbour(screen, x, y + 1, w, h, outsideConnected);
        cross[2] = hasSameNeighbour(screen, x - 1, y, w, h, outsideConnected);
        cross[3] = hasSameNeighbour(screen, x + 1, y, w, h, outsideConnected);

        corners[0] = hasSameNeighbour(screen, x - 1, y - 1, w, h, outsideConnected);
        corners[1] = hasSameNeighbour(screen, x + 1, y - 1, w, h, outsideConnected);
        corners[2] = hasSameNeighbour(screen, x - 1, y + 1, w, h, outsideConnected);
        corners[3] = hasSameNeighbour(screen, x + 1, y + 1, w, h, outsideConnected);

        return new SmartUnit(cross, corners);
    }

    private boolean hasSameNeighbour(boolean[][] screen, int x, int y, int w, int h,
                                     boolean outsideConnected) {
        if (x < 0 || x >= w || y < 0 || y >= h) {
            return outsideConnected;
        } else {
            return screen[x][y];
        }
    }

    private int indexOfSameCross(List<SmartUnit> units, SmartUnit unit) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).sameCross(unit)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfSameCorner(List<SmartUnit> units, SmartUnit unit) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).sameCorners(unit)) {
                return i;
            }
        }
        return -1;
    }

    private static class SmartUnit {

        public boolean[] cross;
        public boolean[] corners;

        public SmartUnit(
                boolean t, boolean b, boolean l, boolean r,
                boolean tl, boolean tr, boolean bl, boolean br) {

            cross = new boolean[4];
            corners = new boolean[4];
            cross[0] = t;
            cross[1] = b;
            cross[2] = l;
            cross[3] = r;
            corners[0] = tl;
            corners[1] = tr;
            corners[2] = bl;
            corners[3] = br;

        }

        public SmartUnit(boolean[] cross, boolean[] corners) {
            this.cross = cross;
            this.corners = corners;
        }

        public boolean fullCrossNeighbours() {
            return cross[0] && cross[1] && cross[2] && cross[3];
        }

        public boolean sameCross(SmartUnit unit) {
            for (int i = 0; i < cross.length; i++) {
                if (this.cross[i] != unit.cross[i]) {
                    return false;
                }
            }
            return true;
        }

        public boolean sameCorners(SmartUnit unit) {
            for (int i = 0; i < corners.length; i++) {
                if (this.corners[i] != unit.corners[i]) {
                    return false;
                }
            }
            return true;
        }

        public void printUnit() {
            System.out.println(corners[0] + " " + cross[0] + " " + corners[1]);
            System.out.println(cross[2] + " " + "    " + " " + cross[3]);
            System.out.println(corners[2] + " " + cross[1] + " " + corners[3]);
        }

    }

}
