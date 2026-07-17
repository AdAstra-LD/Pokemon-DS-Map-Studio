
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
    private int columns = 0;   //All Tiles uses flow layout; named folders always use a grid
    private int rows = DEFAULT_ROWS;
    private boolean collapsed = false;
    private boolean pinned = false;
    private boolean gridLinesVisible = true;
    private boolean smartDrawingsCollapsed = false;
    private transient int pinnedScrollY = 0;
    private int pinnedViewportHeight = 0; //0 = automatic

    public PaletteFolder(String path) {
        this.path = path;
        if (path != null && !path.isEmpty()) {
            this.columns = DEFAULT_COLUMNS;
        } else {
            this.pinned = true;
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
        this.columns = path == null || path.isEmpty()
                ? Math.max(0, columns) : Math.max(1, columns);
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

    public boolean isGridLinesVisible() {
        return gridLinesVisible;
    }

    public void setGridLinesVisible(boolean gridLinesVisible) {
        this.gridLinesVisible = gridLinesVisible;
    }

    public boolean areSmartDrawingsCollapsed() {
        return smartDrawingsCollapsed;
    }

    public void setSmartDrawingsCollapsed(boolean smartDrawingsCollapsed) {
        this.smartDrawingsCollapsed = smartDrawingsCollapsed;
    }

    public int getPinnedScrollY() {
        return pinnedScrollY;
    }

    public void setPinnedScrollY(int pinnedScrollY) {
        this.pinnedScrollY = Math.max(0, pinnedScrollY);
    }

    public int getPinnedViewportHeight() {
        return pinnedViewportHeight;
    }

    public void setPinnedViewportHeight(int pinnedViewportHeight) {
        this.pinnedViewportHeight = Math.max(0, pinnedViewportHeight);
    }
}
