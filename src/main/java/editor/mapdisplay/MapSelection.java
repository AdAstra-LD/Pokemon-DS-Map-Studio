
package editor.mapdisplay;

import editor.grid.MapGrid;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Set;

/**
 * Mask based tile selection inside a single map. Cell coordinates use the
 * map grid convention: x = 0..31 left to right, y = 0..31 where y = 0 is the
 * bottom row (same as MapGrid tile layers).
 */
public class MapSelection {

    public static final int cols = MapGrid.cols;
    public static final int rows = MapGrid.rows;

    private final Point mapCoords;
    private boolean[][] mask = new boolean[cols][rows];
    private boolean rectangular = false;

    public MapSelection(Point mapCoords) {
        this.mapCoords = new Point(mapCoords);
    }

    public Point getMapCoords() {
        return mapCoords;
    }

    public boolean[][] getMask() {
        return mask;
    }

    public boolean isRectangular() {
        return rectangular;
    }

    public boolean isEmpty() {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (mask[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows && mask[x][y];
    }

    public void clear() {
        mask = new boolean[cols][rows];
        rectangular = false;
    }

    public void setRect(Point a, Point b) {
        clear();
        int minX = Math.max(0, Math.min(a.x, b.x));
        int maxX = Math.min(cols - 1, Math.max(a.x, b.x));
        int minY = Math.max(0, Math.min(a.y, b.y));
        int maxY = Math.min(rows - 1, Math.max(a.y, b.y));
        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                mask[i][j] = true;
            }
        }
        rectangular = true;
    }

    public void setEllipse(Point a, Point b) {
        clear();
        int minX = Math.max(0, Math.min(a.x, b.x));
        int maxX = Math.min(cols - 1, Math.max(a.x, b.x));
        int minY = Math.max(0, Math.min(a.y, b.y));
        int maxY = Math.min(rows - 1, Math.max(a.y, b.y));

        double cx = (minX + maxX + 1) / 2.0;
        double cy = (minY + maxY + 1) / 2.0;
        double rx = (maxX - minX + 1) / 2.0;
        double ry = (maxY - minY + 1) / 2.0;

        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                double dx = (i + 0.5 - cx) / rx;
                double dy = (j + 0.5 - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) {
                    mask[i][j] = true;
                }
            }
        }
        rectangular = false;
    }

    public void addCell(int x, int y) {
        if (x >= 0 && x < cols && y >= 0 && y < rows) {
            mask[x][y] = true;
            rectangular = false;
        }
    }

    /** Adds all cells of a Bresenham line between the two cells. */
    public void addLine(Point a, Point b) {
        for (Point p : bresenham(a, b)) {
            addCell(p.x, p.y);
        }
    }

    /**
     * Closes the lasso outline with a line between the last and first drawn
     * cells and selects the enclosed interior.
     */
    public void closeAndFill(Point first, Point last) {
        if (first != null && last != null) {
            addLine(last, first);
        }
        fillInterior();
        rectangular = false;
    }

    private void fillInterior() {
        //Flood from outside a 1 cell padded border; everything the flood
        //cannot reach is enclosed by the outline and becomes selected.
        boolean[][] outside = new boolean[cols + 2][rows + 2];
        ArrayDeque<Point> stack = new ArrayDeque<>();
        stack.push(new Point(0, 0));
        outside[0][0] = true;
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : deltas) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (nx < 0 || nx >= cols + 2 || ny < 0 || ny >= rows + 2 || outside[nx][ny]) {
                    continue;
                }
                int mx = nx - 1;
                int my = ny - 1;
                boolean blocked = mx >= 0 && mx < cols && my >= 0 && my < rows && mask[mx][my];
                if (!blocked) {
                    outside[nx][ny] = true;
                    stack.push(new Point(nx, ny));
                }
            }
        }
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (!mask[i][j] && !outside[i + 1][j + 1]) {
                    mask[i][j] = true;
                }
            }
        }
    }

    /**
     * Magic wand region: cells that share the tile index found at (x, y) in
     * the given tile layer. Contiguous limits the region to the connected
     * area around the clicked cell; otherwise all matching cells of the map
     * are returned.
     */
    public static boolean[][] computeWandRegion(int[][] tileLayer, int x, int y, boolean contiguous) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            return new boolean[cols][rows];
        }
        return computeWandRegion(tileLayer, x, y, contiguous,
                java.util.Collections.singleton(tileLayer[x][y]));
    }

    /**
     * Magic wand region matching any tile ID in a Smart Drawing template.
     * The clicked cell must itself belong to the supplied tile set.
     */
    public static boolean[][] computeWandRegion(int[][] tileLayer, int x, int y,
                                                 boolean contiguous, Set<Integer> targetTiles) {
        boolean[][] region = new boolean[cols][rows];
        if (x < 0 || x >= cols || y < 0 || y >= rows
                || targetTiles == null || !targetTiles.contains(tileLayer[x][y])) {
            return region;
        }
        if (!contiguous) {
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    region[i][j] = targetTiles.contains(tileLayer[i][j]);
                }
            }
            return region;
        }
        ArrayDeque<Point> stack = new ArrayDeque<>();
        stack.push(new Point(x, y));
        region[x][y] = true;
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : deltas) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows
                        && !region[nx][ny] && targetTiles.contains(tileLayer[nx][ny])) {
                    region[nx][ny] = true;
                    stack.push(new Point(nx, ny));
                }
            }
        }
        return region;
    }

    /** Unions the region into the mask, or subtracts it when remove is true. */
    public void applyRegion(boolean[][] region, boolean remove) {
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (region[i][j]) {
                    mask[i][j] = !remove;
                }
            }
        }
        rectangular = false;
    }

    /** Bounding box of the selected cells, or null when nothing is selected. */
    public Rectangle getBounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = -1, maxY = -1;
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (mask[i][j]) {
                    minX = Math.min(minX, i);
                    minY = Math.min(minY, j);
                    maxX = Math.max(maxX, i);
                    maxY = Math.max(maxY, j);
                }
            }
        }
        if (maxX < 0) {
            return null;
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public static java.util.List<Point> bresenham(Point a, Point b) {
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        int x0 = a.x, y0 = a.y, x1 = b.x, y1 = b.y;
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            cells.add(new Point(x0, y0));
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
        return cells;
    }

    /**
     * A four-connected version of a Bresenham line. Smart Drawing paths need
     * every consecutive cell to share an edge, so diagonal steps receive one
     * bridging cell along the line's dominant axis.
     */
    public static java.util.List<Point> orthogonalLine(Point a, Point b) {
        java.util.List<Point> diagonalLine = bresenham(a, b);
        java.util.ArrayList<Point> cells = new java.util.ArrayList<>();
        if (diagonalLine.isEmpty()) {
            return cells;
        }
        boolean horizontalFirst = Math.abs(b.x - a.x) >= Math.abs(b.y - a.y);
        cells.add(new Point(diagonalLine.get(0)));
        for (int i = 1; i < diagonalLine.size(); i++) {
            Point previous = diagonalLine.get(i - 1);
            Point next = diagonalLine.get(i);
            if (previous.x != next.x && previous.y != next.y) {
                Point bridge = horizontalFirst
                        ? new Point(next.x, previous.y)
                        : new Point(previous.x, next.y);
                if (!cells.get(cells.size() - 1).equals(bridge)) {
                    cells.add(bridge);
                }
            }
            if (!cells.get(cells.size() - 1).equals(next)) {
                cells.add(new Point(next));
            }
        }
        return cells;
    }
}
