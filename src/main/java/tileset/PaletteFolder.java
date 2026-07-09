
package tileset;

/**
 * A folder of the tile palette selector. Folders only organize how tiles are
 * displayed; tiles reference their folder by path and the tileset keeps an
 * ordered list of folders. Stored in the tileset metadata sidecar file.
 */
public class PaletteFolder {

    public static final String UNSORTED = "";
    public static final String UNSORTED_DISPLAY_NAME = "All Tiles";
    public static final int DEFAULT_COLUMNS = 8;
    public static final int DEFAULT_ROWS = 4;

    private String path;
    private int columns = 0;   //Layout template grid width in tile units; 0 = flow layout
    private int rows = DEFAULT_ROWS;
    private boolean collapsed = false;
    private boolean pinned = false;

    public PaletteFolder(String path) {
        this.path = path;
        if (path != null && !path.isEmpty()) {
            this.columns = DEFAULT_COLUMNS;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = Math.max(0, columns);
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, rows);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
