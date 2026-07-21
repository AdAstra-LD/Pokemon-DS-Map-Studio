package editor.tileselector;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;

import editor.handler.MapEditorHandler;
import editor.mapdisplay.MapDisplay;
import editor.mapdisplay.ViewMode;
import editor.tileseteditor.TilesetEditorDialog;
import formats.collisions.CollisionTypes;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import tileset.PaletteFolder;
import tileset.PaletteFolderBundleIO;
import tileset.Tile;
import tileset.Tileset;

/**
 * @author Trifindo, JackHack96
 */
public class TileSelector extends JPanel {

    private MapEditorHandler handler;
    private TilesetEditorDialog dialog;
    private final int tilePixelSize = 16;
    private final int maxCols = 8;
    private final int headerHeight = 16;
    private static final int PINNED_DIVIDER_HEIGHT = 5;
    private static final int MIN_PINNED_BODY_HEIGHT = 32;
    private int rows;
    private ArrayList<Rectangle> boundingBoxes = new ArrayList<>();
    private BufferedImage display;

    private boolean multiSelectionEnabled;
    private boolean multiselecting = false;
    private boolean dragging = false;
    private boolean canDrag = false;
    private int indexTileHovering = -1;
    private int indexSecondTileSelected = -1;
    private int mouseX, mouseY;
    private BufferedImage multiSelectImg;
    private ArrayList<Integer> dragSelectionIndices = null;
    private int dragSelectionAnchorIndex = -1;
    private ArrayList<Integer> rangeSelectedIndices = new ArrayList<>();
    private Section rangeSelectionSection = null;
    private boolean rangeSelectionInitialized = false;
    private boolean rangeAnchorFromRightClick = false;

    //Folder view (main window selector only)
    private static final Rectangle HIDDEN_BOUNDS = new Rectangle(-1, -1, 0, 0);
    private boolean folderViewActive = false;
    private ArrayList<Section> sections = new ArrayList<>();
    private ArrayList<Placement> placements = new ArrayList<>();
    private int widthUnits = maxCols;

    //Ctrl+click multi selection for bulk folder moves (main window selector only)
    private final LinkedHashSet<Integer> multiSelected = new LinkedHashSet<>();

    //Drag a tile into a folder / layout grid cell (main window selector only)
    private boolean tileDragArmed = false;
    private boolean tileDragging = false;
    private int tileDragIndex = -1;
    private Point tileDragStart = null;
    //Dragging from a multi selected tile moves the whole group
    private boolean tileDragGroup = false;
    //Shift + drag rubber band selection (screen coords; main window selector only)
    private Point bandStart = null;
    private Rectangle bandRect = null;
    private PaletteFolder resizingPinnedFolder;
    private int pinnedResizeStartY;
    private int pinnedResizeStartHeight;

    /** One displayed folder block: header bar + tile area. */
    private static class Section {
        PaletteFolder folder;   //null for "All Tiles" until its state folder exists
        boolean allTiles;       //the master section that always shows every tile
        int depth;              //nesting depth (0 = root folder)
        Rectangle headerBounds;
        Rectangle bodyBounds;
        int columns;            //layout template grid columns; 0 = flow
        int rows;               //layout template grid rows (empty cells stay visible)
        int gridRows;           //rows of the explicit slot grid (columns > 0)
        Rectangle addRowBounds;
        final ArrayList<Integer> tileIndices = new ArrayList<>();

        String getDisplayName() {
            if (allTiles) {
                return "All Tiles";
            }
            String path = folder.getPath();
            int idx = path.lastIndexOf('/');
            return idx < 0 ? path : path.substring(idx + 1);
        }

        boolean isCollapsed() {
            return folder != null && folder.isCollapsed();
        }
    }

    /** One drawn tile occurrence; a tile shows in All Tiles AND its folder. */
    private static class Placement {
        final int tileIndex;
        final Rectangle bounds;
        final Section section;

        Placement(int tileIndex, Rectangle bounds, Section section) {
            this.tileIndex = tileIndex;
            this.bounds = bounds;
            this.section = section;
        }
    }

    private static class PinnedLayout {
        final Section section;
        final Rectangle bounds;
        final int bodyViewportHeight;
        final Rectangle dividerBounds;

        PinnedLayout(Section section, Rectangle bounds, int bodyViewportHeight,
                Rectangle dividerBounds) {
            this.section = section;
            this.bounds = bounds;
            this.bodyViewportHeight = bodyViewportHeight;
            this.dividerBounds = dividerBounds;
        }
    }

    public TileSelector() {
        initComponents();
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseWheelListener(this::scrollPinnedFolder);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (getParent() instanceof JViewport) {
            ((JViewport) getParent()).setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
    }

    private void formMouseDragged(MouseEvent evt) {
        if (resizingPinnedFolder != null) {
            int height = Math.max(MIN_PINNED_BODY_HEIGHT,
                    pinnedResizeStartHeight + evt.getY() - pinnedResizeStartY);
            resizingPinnedFolder.setPinnedViewportHeight(height);
            repaint();
            return;
        }
        if (multiSelectionEnabled) {
            if (SwingUtilities.isLeftMouseButton(evt) && canDrag) {
                dragging = true;
                mouseX = evt.getX();
                mouseY = evt.getY();
                indexTileHovering = getIndexSelected(evt);
                repaint();
            }
        } else if (bandStart != null && SwingUtilities.isLeftMouseButton(evt)) {
            bandRect = new Rectangle(Math.min(bandStart.x, evt.getX()),
                    Math.min(bandStart.y, evt.getY()),
                    Math.abs(evt.getX() - bandStart.x),
                    Math.abs(evt.getY() - bandStart.y));
            selectTilesInBand();
            repaint();
        } else if (tileDragArmed && SwingUtilities.isLeftMouseButton(evt)) {
            if (!tileDragging && tileDragStart.distance(evt.getPoint()) > 5) {
                tileDragging = true;
            }
            if (tileDragging) {
                mouseX = evt.getX();
                mouseY = evt.getY();
                repaint();
            }
        }
    }

    private void selectTilesInBand() {
        if (bandRect == null) {
            return;
        }
        Point p1 = getSourcePoint(bandRect.x, bandRect.y);
        Point p2 = getSourcePoint(bandRect.x + bandRect.width, bandRect.y + bandRect.height);
        Rectangle band = new Rectangle(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y),
                Math.abs(p2.x - p1.x) + 1, Math.abs(p2.y - p1.y) + 1);
        multiSelected.clear();
        if (!placements.isEmpty()) {
            for (Placement placement : placements) {
                if (placement.bounds.intersects(band)) {
                    multiSelected.add(placement.tileIndex);
                }
            }
        } else {
            for (int i = 0; i < boundingBoxes.size(); i++) {
                if (boundingBoxes.get(i).intersects(band)) {
                    multiSelected.add(i);
                }
            }
        }
    }

    private void formMousePressed(MouseEvent evt) {
        PinnedLayout resizeLayout = getPinnedDividerAt(evt.getPoint());
        if (resizeLayout != null && SwingUtilities.isLeftMouseButton(evt)) {
            resizingPinnedFolder = resizeLayout.section.folder;
            pinnedResizeStartY = evt.getY();
            pinnedResizeStartHeight = resizeLayout.bodyViewportHeight;
            return;
        }
        if (!multiSelectionEnabled) {
            singleSelectMousePressed(evt);
            return;
        }
        Section headerSection = getHeaderSectionAt(evt.getX(), evt.getY());
        if (folderViewActive && headerSection != null) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                toggleSectionCollapsed(headerSection);
            } else if (SwingUtilities.isRightMouseButton(evt)) {
                showFolderMenu(headerSection, evt);
            }
            return;
        }
        Section addRowSection = getAddRowSectionAt(evt.getX(), evt.getY());
        if (folderViewActive && addRowSection != null && SwingUtilities.isLeftMouseButton(evt)) {
            addLayoutRow(addRowSection);
            return;
        }
        int index = getIndexSelected(evt);
        if (index != -1) {
            Section clickedSection = getSectionAt(evt.getX(), evt.getY());
            if (SwingUtilities.isLeftMouseButton(evt)) {
                if (rangeAnchorFromRightClick && clickedSection == rangeSelectionSection) {
                    rangeSelectedIndices = getVisualRange(rangeSelectionSection,
                            handler.getTileIndexSelected(), index);
                    indexSecondTileSelected = index;
                    multiselecting = rangeSelectedIndices.size() > 1;
                    rangeAnchorFromRightClick = false;
                } else if (multiselecting && rangeSelectedIndices.contains(index)) {
                    canDrag = true;
                    dragSelectionIndices = getIndicesSelected();
                    dragSelectionAnchorIndex = index;
                    multiSelectImg = handler.getTileset().get(index).getThumbnail();
                    multiselecting = false;
                } else if (rangeSelectionInitialized && clickedSection == rangeSelectionSection
                        && index == handler.getTileIndexSelected()) {
                    canDrag = true;
                    indexSecondTileSelected = handler.getTileIndexSelected();
                    dragSelectionIndices = getIndicesSelected();
                    dragSelectionAnchorIndex = index;
                    multiSelectImg = handler.getTileSelected().getThumbnail();
                    multiselecting = false;
                } else {
                    handler.setIndexTileSelected(index);
                    rangeSelectionSection = clickedSection;
                    rangeSelectionInitialized = true;
                    rangeSelectedIndices.clear();
                    rangeSelectedIndices.add(index);
                    dragSelectionIndices = null;
                    multiselecting = false;
                    rangeAnchorFromRightClick = false;
                }
            } else if (SwingUtilities.isRightMouseButton(evt) && multiSelectionEnabled) {
                indexSecondTileSelected = index;
                if (!rangeSelectionInitialized || clickedSection != rangeSelectionSection
                        || (rangeSelectionSection != null
                        && !rangeSelectionSection.tileIndices.contains(handler.getTileIndexSelected()))) {
                    handler.setIndexTileSelected(index);
                    rangeSelectionSection = clickedSection;
                    rangeSelectionInitialized = true;
                    rangeSelectedIndices.clear();
                    rangeSelectedIndices.add(index);
                    multiselecting = false;
                    rangeAnchorFromRightClick = true;
                } else {
                    rangeSelectedIndices = getVisualRange(rangeSelectionSection,
                            handler.getTileIndexSelected(), index);
                    multiselecting = rangeSelectedIndices.size() > 1;
                    rangeAnchorFromRightClick = false;
                }
            }
        } else {
            multiselecting = false;
            rangeSelectedIndices.clear();
            rangeSelectionSection = null;
            rangeSelectionInitialized = false;
            rangeAnchorFromRightClick = false;
        }
        handler.getMainFrame().updateTileSelectedID();
        repaint();
    }

    private void singleSelectMousePressed(MouseEvent evt) {
        Section headerSection = getHeaderSectionAt(evt.getX(), evt.getY());
        if (headerSection != null) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                toggleSectionCollapsed(headerSection);
            } else if (SwingUtilities.isRightMouseButton(evt)) {
                showFolderMenu(headerSection, evt);
            }
            return;
        }

        Section addRowSection = getAddRowSectionAt(evt.getX(), evt.getY());
        if (addRowSection != null && SwingUtilities.isLeftMouseButton(evt)) {
            addLayoutRow(addRowSection);
            return;
        }

        if (SwingUtilities.isLeftMouseButton(evt) && evt.isShiftDown()) {
            //Shift + drag selects a rectangle of tiles; a plain Shift + click
            //selects the range from the current tile to the clicked one
            bandStart = evt.getPoint();
            bandRect = null;
            return;
        }
        int index = getIndexSelected(evt);
        if (index != -1) {
            if (SwingUtilities.isLeftMouseButton(evt) && evt.isControlDown()) {
                //Ctrl+click builds a multi selection for bulk folder moves
                if (!multiSelected.remove(index)) {
                    multiSelected.add(index);
                }
                repaint();
                return;
            }
            handler.setIndexTileSelected(index);
            if (SwingUtilities.isLeftMouseButton(evt)) {
                //Pressing a multi selected tile arms a group drag and keeps
                //the selection; a plain click clears it on release instead
                tileDragGroup = multiSelected.contains(index);
                if (!tileDragGroup) {
                    multiSelected.clear();
                }
                if (handler.getMainFrame().getMapDisplay().getViewMode().getViewID() == ViewMode.ViewID.VIEW_ORTHO) {
                    handler.getMainFrame().getMapDisplay().setEditMode(MapDisplay.EditMode.MODE_EDIT);
                    handler.getMainFrame().getJtbModeEdit().setSelected(true);
                }
                //Arm the organize drag (folder / layout slot placement)
                tileDragArmed = folderViewActive;
                tileDragIndex = index;
                tileDragStart = evt.getPoint();
            } else if (SwingUtilities.isRightMouseButton(evt)) {
                if (multiSelected.size() > 1 && multiSelected.contains(index)) {
                    showMultiSelectionMenu(evt);
                } else {
                    multiSelected.clear();
                    showTileMenu(index, evt);
                }
            }
        } else if (SwingUtilities.isRightMouseButton(evt)) {
            showBackgroundMenu(evt);
        } else if (SwingUtilities.isLeftMouseButton(evt)) {
            multiSelected.clear();
        }
        handler.getMainFrame().updateTileSelectedID();
        repaint();
    }

    private void toggleSectionCollapsed(Section section) {
        PaletteFolder folder = section.folder;
        if (folder == null && section.allTiles) {
            //Collapse state of All Tiles lives in a folder with an empty path.
            folder = handler.getTileset().getOrCreatePaletteFolder(PaletteFolder.UNSORTED);
        }
        if (folder != null) {
            folder.setCollapsed(!folder.isCollapsed());
            updateLayout();
            repaint();
        }
    }

    private void addLayoutRow(Section section) {
        if (section.folder == null) {
            return;
        }
        section.folder.setRows(section.folder.getRows() + 1);
        updateLayout();
        repaint();
    }

    private void formMouseReleased(MouseEvent evt) {
        if (resizingPinnedFolder != null) {
            resizingPinnedFolder = null;
            setCursor(Cursor.getDefaultCursor());
            repaint();
            return;
        }
        if (!multiSelectionEnabled) {
            if (bandStart != null) {
                if (bandRect == null && SwingUtilities.isLeftMouseButton(evt)) {
                    //Shift + click: select the range from the current tile to the click
                    int index = getIndexSelected(evt);
                    int from = handler.getTileIndexSelected();
                    if (index != -1 && from >= 0) {
                        multiSelected.clear();
                        for (int i = Math.min(from, index); i <= Math.max(from, index); i++) {
                            multiSelected.add(i);
                        }
                    }
                }
                bandStart = null;
                bandRect = null;
                repaint();
                return;
            }
            if (tileDragging) {
                if (tileDragGroup && multiSelected.size() > 1) {
                    dropSelectedTilesGroup(evt);
                } else {
                    dropTile(evt);
                }
            } else if (tileDragGroup) {
                //A plain click (no drag) on a selected tile clears the multi selection
                multiSelected.clear();
            }
            tileDragArmed = false;
            tileDragging = false;
            tileDragIndex = -1;
            tileDragGroup = false;
            repaint();
            return;
        }
        canDrag = false;
        if (dragging) {
            dragging = false;

            Section sourceSection = rangeSelectionSection;
            if (folderViewActive && dropSelectedTilesToFolder(evt)) {
                repaint();
                dragSelectionIndices = null;
                dragSelectionAnchorIndex = -1;
                return;
            }

            //A selection arranged in a palette folder never changes Tile ID order.
            if (sourceSection != null && !sourceSection.allTiles) {
                repaint();
                dragSelectionIndices = null;
                dragSelectionAnchorIndex = -1;
                return;
            }

            int index = getIndexSelected(evt);
            ArrayList<Integer> selectionIndices = dragSelectionIndices == null
                    ? getIndicesSelected() : new ArrayList<>(dragSelectionIndices);
            if (index != -1 && !selectionIndices.contains(index)) {
                ArrayList<Integer> indices = new ArrayList<>(handler.getTileset().size());
                for (int i = 0; i < handler.getTileset().size(); i++) {
                    indices.add(i);
                }
                indices.removeAll(selectionIndices);
                int insertionIndex = Math.max(0, indices.indexOf(index));
                indices.addAll(insertionIndex, selectionIndices);
                handler.getTileset().moveTiles(indices);
                handler.setIndexTileSelected(insertionIndex);
                rangeSelectedIndices.clear();
                rangeSelectedIndices.add(insertionIndex);
                multiselecting = false;
                updateLayout();
                dialog.updateViewTileIndex();
            }
            repaint();
        }
        dragSelectionIndices = null;
        dragSelectionAnchorIndex = -1;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean drawPinnedSections = false;
        if (display != null) {
            g.drawImage(display, 0, 0, null);
            if (handler != null && folderViewActive) {
                drawPinnedSections = true;
            }
            if (handler != null && handler.getTileset().size() > 0) {
                g.setColor(Color.red);
                drawTileBounds(g, handler.getTileIndexSelected());
            }
        }

        if (handler != null) {
            if (handler.getTileset().size() > 0) {
                if (multiSelectionEnabled && !rangeSelectedIndices.isEmpty()
                        && boundingBoxes.size() > 0) {
                    for (int i : rangeSelectedIndices) {
                        if (i == handler.getTileIndexSelected()) {
                            continue;
                        }
                        g.setColor(Color.red);
                        drawTileBounds(g, i);
                    }
                }
                if (!multiSelectionEnabled && !multiSelected.isEmpty()) {
                    g.setColor(new Color(80, 180, 255));
                    for (int index : multiSelected) {
                        if (index < handler.getTileset().size()) {
                            drawTileBounds(g, index);
                        }
                    }
                }
                if (dragging) {
                    if (indexTileHovering != -1 && boundingBoxes.size() > 0) {
                        g.setColor(Color.blue);
                        drawTileBounds(g, indexTileHovering);
                    }

                    if (drawPinnedSections) {
                        drawPinnedFolderSections((Graphics2D) g);
                        drawPinnedSections = false;
                    }

                    g.drawImage(multiSelectImg,
                            mouseX, mouseY, null);
                }
                if (tileDragging && tileDragIndex >= 0) {
                    if (drawPinnedSections) {
                        drawPinnedFolderSections((Graphics2D) g);
                        drawPinnedSections = false;
                    }
                    drawTileDrag(g);
                }
            }
        }
        if (drawPinnedSections) {
            drawPinnedFolderSections((Graphics2D) g);
        }
        if (bandRect != null) {
            g.setColor(new Color(80, 180, 255, 50));
            g.fillRect(bandRect.x, bandRect.y, bandRect.width, bandRect.height);
            g.setColor(new Color(80, 180, 255));
            g.drawRect(bandRect.x, bandRect.y, bandRect.width, bandRect.height);
        }
    }

    public void init(MapEditorHandler handler, TilesetEditorDialog dialog) {
        this.handler = handler;
        this.multiSelectionEnabled = true;
        this.dialog = dialog;

        updateLayout();
    }

    public void init(MapEditorHandler handler) {
        this.handler = handler;
        this.multiSelectionEnabled = false;

        updateLayout();
    }

    public void updateLayout() {
        boundingBoxes = new ArrayList<>();
        sections = new ArrayList<>();
        placements = new ArrayList<>();
        folderViewActive = false;
        widthUnits = maxCols;

        if (handler == null) {
            return;
        }
        multiSelected.removeIf(index -> index >= handler.getTileset().size());

        int height;
        if (multiSelectionEnabled && !hasNamedFolders()) {
            height = Math.max(1, computeFlatLayout());
        } else {
            folderViewActive = true;
            height = Math.max(1, computeFolderLayout());
        }
        rows = height / tilePixelSize;

        display = new BufferedImage(widthUnits * tilePixelSize, height, BufferedImage.TYPE_4BYTE_ABGR);
        paintDisplay();

        updateSize();
    }

    private boolean hasNamedFolders() {
        for (PaletteFolder folder : handler.getTileset().getPaletteFolders()) {
            if (!folder.getPath().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void updateSize() {
        this.setPreferredSize(new Dimension(
                display.getWidth(),
                display.getHeight()));
        this.setSize(new Dimension(
                display.getWidth(),
                display.getHeight()));
        revalidate();
    }

    /* -------------------- Layout -------------------- */

    /** Classic layout: every tile in index order, flowing in 8 unit columns. */
    private int computeFlatLayout() {
        for (int i = 0; i < handler.getTileset().size(); i++) {
            boundingBoxes.add(HIDDEN_BOUNDS);
        }
        ArrayList<Integer> all = new ArrayList<>();
        for (int i = 0; i < handler.getTileset().size(); i++) {
            all.add(i);
        }
        return flowTiles(null, all, 0, maxCols);
    }

    /**
     * Folder view: named folders show their assigned tiles; the "All Tiles"
     * section below always shows every tile in tile id order (the order that
     * matters for animations and exports is never changed by folders).
     */
    private int computeFolderLayout() {
        Tileset tset = handler.getTileset();
        for (int i = 0; i < tset.size(); i++) {
            boundingBoxes.add(HIDDEN_BOUNDS);
        }

        //Folder tree: roots in list order, each followed by its subfolders.
        //Subtrees of collapsed folders are skipped entirely.
        ArrayList<Section> newSections = new ArrayList<>();
        for (PaletteFolder folder : tset.getPaletteFolders()) {
            if (folder.getPath().isEmpty()) {
                continue; //Reserved entry holding the All Tiles collapse state
            }
            String parent = Tileset.getParentFolderPath(folder.getPath());
            if (parent == null || tset.getPaletteFolder(parent) == null) {
                addFolderSectionTree(newSections, tset, folder, 0);
            }
        }
        Section allTiles = new Section();
        allTiles.allTiles = true;
        allTiles.folder = tset.getOrCreatePaletteFolder(PaletteFolder.UNSORTED);
        newSections.add(allTiles);

        for (int i = 0; i < tset.size(); i++) {
            allTiles.tileIndices.add(i);
            for (String path : tset.get(i).getPaletteFolderSlots().keySet()) {
                for (Section section : newSections) {
                    if (!section.allTiles && section.folder.getPath().equals(path)) {
                        section.tileIndices.add(i);
                        break;
                    }
                }
            }
        }

        for (Section section : newSections) {
            if (!section.allTiles && section.columns > 0) {
                section.columns = Math.min(section.columns, Math.max(1, tset.size()));
                widthUnits = Math.max(widthUnits, section.columns);
            }
        }

        int y = 0;
        int width = widthUnits * tilePixelSize;
        for (Section section : newSections) {
            if (!section.allTiles && section.depth == 0) {
                y = layoutFolderSection(section, newSections, y, width);
            }
        }
        allTiles.headerBounds = new Rectangle(0, y, width, headerHeight);
        y += headerHeight + 1;
        if (allTiles.isCollapsed()) {
            allTiles.bodyBounds = new Rectangle(0, y, width, 0);
        } else {
            int bodyStart = y;
            y = flowTiles(allTiles, allTiles.tileIndices, y, maxCols);
            allTiles.bodyBounds = new Rectangle(0, bodyStart, width, y - bodyStart);
            y += 3;
        }
        sections = newSections;
        return y;
    }

    /** Parent header, child folder trees, then the parent's own tile body. */
    private int layoutFolderSection(Section section, ArrayList<Section> allSections,
            int y, int width) {
        section.headerBounds = new Rectangle(0, y, width, headerHeight);
        y += headerHeight + 1;
        if (section.isCollapsed()) {
            section.bodyBounds = new Rectangle(0, y, width, 0);
            return y;
        }

        String sectionPath = section.folder.getPath();
        for (Section child : allSections) {
            if (child.allTiles || child.depth != section.depth + 1 || child.folder == null) {
                continue;
            }
            String parentPath = Tileset.getParentFolderPath(child.folder.getPath());
            if (sectionPath.equals(parentPath)) {
                y = layoutFolderSection(child, allSections, y, width);
            }
        }

        int bodyStart = y;
        if (section.columns > 0) {
            y = layoutSlotGrid(section, y);
        } else {
            y = flowTiles(section, section.tileIndices, y, maxCols);
        }
        section.bodyBounds = new Rectangle(0, bodyStart, width, y - bodyStart);
        return y + 3;
    }

    /** Adds the folder's section followed by its subfolder subtrees. */
    private void addFolderSectionTree(ArrayList<Section> out, Tileset tset,
                                      PaletteFolder folder, int depth) {
        Section section = new Section();
        section.folder = folder;
        section.depth = depth;
        section.columns = folder.getColumns();
        section.rows = folder.getRows();
        out.add(section);
        if (folder.isCollapsed()) {
            return; //Collapsing a folder hides its whole subtree
        }
        String prefix = folder.getPath() + "/";
        for (PaletteFolder child : tset.getPaletteFolders()) {
            if (child.getPath().startsWith(prefix)
                    && child.getPath().indexOf('/', prefix.length()) < 0) {
                addFolderSectionTree(out, tset, child, depth + 1);
            }
        }
    }

    /**
     * Layout template grid: tiles with a slot go to their fixed cell (empty
     * cells stay visibly empty); tiles without a slot flow below the grid.
     */
    private int layoutSlotGrid(Section section, int startY) {
        Tileset tset = handler.getTileset();
        ArrayList<Integer> flowing = new ArrayList<>();
        int gridRows = 0;
        for (int index : section.tileIndices) {
            Tile tile = tset.get(index);
            int slot = tile.getPaletteSlot(section.folder.getPath());
            //Slots beyond a sane grid size (e.g. a hand edited sidecar) flow instead
            if (slot < 0 || slot >= section.columns * 512) {
                flowing.add(index);
                continue;
            }
            int row = slot / section.columns;
            int col = slot % section.columns;
            addPlacement(index, new Rectangle(
                    col * tilePixelSize,
                    startY + row * tilePixelSize,
                    getDisplayWidth(tile, section) * tilePixelSize,
                    getDisplayHeight(tile, section) * tilePixelSize), section);
            gridRows = Math.max(gridRows, row + getDisplayHeight(tile, section));
        }
        gridRows = Math.max(gridRows, section.rows);
        section.gridRows = gridRows;
        section.addRowBounds = new Rectangle(0, startY + gridRows * tilePixelSize,
                tilePixelSize, tilePixelSize);
        return flowTiles(section, flowing, startY + (gridRows + 1) * tilePixelSize,
                Math.max(section.columns, 1));
    }

    /** Flow layout shared by the flat view and folder bodies. Returns the end y. */
    private int flowTiles(Section section, ArrayList<Integer> indices, int startY, int cols) {
        int rowIndex = 0;
        int colIndex = 0;
        int rowWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < indices.size(); i++) {
            Tile tile = handler.getTileset().get(indices.get(i));
            int displayWidth = getDisplayWidth(tile, section);
            int displayHeight = getDisplayHeight(tile, section);
            int tileWidth = Math.min(displayWidth, cols);
            rowWidth += tileWidth;

            if (rowWidth > cols) {
                rowWidth = 0;
                colIndex = 0;
                rowIndex += maxHeight;
                maxHeight = 0;
                i--;
            } else {
                addPlacement(indices.get(i), new Rectangle(
                        colIndex * tilePixelSize,
                        startY + rowIndex * tilePixelSize,
                        displayWidth * tilePixelSize,
                        displayHeight * tilePixelSize), section);
                colIndex += tileWidth;
                if (displayHeight > maxHeight) {
                    maxHeight = displayHeight;
                }
                if (rowWidth == cols) {
                    rowWidth = 0;
                    colIndex = 0;
                    rowIndex += maxHeight;
                    maxHeight = 0;
                }
            }
        }
        return startY + (rowIndex + maxHeight) * tilePixelSize;
    }

    private void addPlacement(int tileIndex, Rectangle bounds, Section section) {
        placements.add(new Placement(tileIndex, bounds, section));
        //The canonical box of a tile is its first visible occurrence
        if (boundingBoxes.get(tileIndex) == HIDDEN_BOUNDS) {
            boundingBoxes.set(tileIndex, bounds);
        }
    }

    private int getDisplayWidth(Tile tile, Section section) {
        return section != null && !section.allTiles ? tile.getPaletteDisplayWidth() : tile.getWidth();
    }

    private int getDisplayHeight(Tile tile, Section section) {
        return section != null && !section.allTiles ? tile.getPaletteDisplayHeight() : tile.getHeight();
    }

    private BufferedImage getDisplayThumbnail(Tile tile, Section section) {
        return section != null && !section.allTiles ? tile.getPaletteThumbnail() : tile.getThumbnail();
    }

    /* -------------------- Display painting -------------------- */

    private void paintDisplay() {
        Graphics2D g = (Graphics2D) display.getGraphics();

        if (folderViewActive) {
            for (Section section : sections) {
                paintSectionHeader(g, section);
                if (!section.isCollapsed() && !section.allTiles && section.columns > 0) {
                    paintEmptySlots(g, section);
                    paintAddRowCell(g, section);
                }
            }
        }

        for (Placement placement : placements) {
            g.drawImage(getDisplayThumbnail(handler.getTileset().get(placement.tileIndex), placement.section),
                    placement.bounds.x, placement.bounds.y, null);
        }
        g.dispose();
    }

    private void paintSectionHeader(Graphics2D g, Section section) {
        paintSectionHeader(g, section, section.headerBounds);
    }

    private void paintSectionHeader(Graphics2D g, Section section, Rectangle r) {
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) {
            bg = Color.gray;
        }
        if (fg == null) {
            fg = Color.black;
        }
        g.setColor(new Color(
                Math.max(0, bg.getRed() - 25),
                Math.max(0, bg.getGreen() - 25),
                Math.max(0, bg.getBlue() - 25)));
        g.fillRect(r.x, r.y, r.width, r.height);

        //Expand / collapse triangle, indented by nesting depth
        g.setColor(fg);
        int indent = section.depth * 10;
        int cx = r.x + 5 + indent;
        int cy = r.y + r.height / 2;
        Polygon triangle;
        if (section.isCollapsed()) {
            triangle = new Polygon(new int[]{cx - 2, cx + 3, cx - 2}, new int[]{cy - 4, cy, cy + 4}, 3);
        } else {
            triangle = new Polygon(new int[]{cx - 3, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
        }
        g.fillPolygon(triangle);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        String text = section.getDisplayName() + "  (" + section.tileIndices.size() + ")";
        while (text.length() > 3 && g.getFontMetrics().stringWidth(text) > r.width - 16 - indent) {
            text = text.substring(0, text.length() - 4) + "...";
        }
        g.drawString(text, r.x + 12 + indent, r.y + r.height - 4);
    }

    private void drawPinnedFolderSections(Graphics2D g) {
        for (PinnedLayout layout : getPinnedLayouts()) {
            paintPinnedSection(g, layout);
        }
    }

    private void paintPinnedSection(Graphics2D g, PinnedLayout layout) {
        Section section = layout.section;
        Rectangle sticky = layout.bounds;
        Graphics2D copy = (Graphics2D) g.create();
        copy.setClip(sticky);
        copy.setColor(getBackground());
        copy.fillRect(sticky.x, sticky.y, sticky.width, sticky.height);

        copy.translate(sticky.x - section.headerBounds.x, sticky.y - section.headerBounds.y);
        paintSectionHeader(copy, section);
        copy.dispose();

        if (layout.bodyViewportHeight > 0) {
            int bodyTop = sticky.y + headerHeight + 1;
            Graphics2D body = (Graphics2D) g.create();
            body.setClip(sticky.x, bodyTop, sticky.width, layout.bodyViewportHeight);
            body.setColor(getBackground());
            body.fillRect(sticky.x, bodyTop, sticky.width, layout.bodyViewportHeight);
            int dx = sticky.x - section.bodyBounds.x;
            int dy = bodyTop - section.bodyBounds.y - section.folder.getPinnedScrollY();
            body.translate(dx, dy);
            if (!section.allTiles && section.columns > 0) {
                paintEmptySlots(body, section);
                paintAddRowCell(body, section);
            }
            for (Placement placement : placements) {
                if (placement.section == section) {
                    body.drawImage(getDisplayThumbnail(handler.getTileset().get(placement.tileIndex), placement.section),
                            placement.bounds.x, placement.bounds.y, null);
                }
            }
            paintPinnedSelectionOverlays(body, section);
            body.dispose();
            paintPinnedScrollIndicator(g, layout, bodyTop);
        }
        if (layout.dividerBounds != null) {
            Color divider = UIManager.getColor("Separator.foreground");
            g.setColor(divider == null ? new Color(125, 125, 125) : divider);
            int y = layout.dividerBounds.y + layout.dividerBounds.height / 2;
            g.drawLine(layout.dividerBounds.x, y,
                    layout.dividerBounds.x + layout.dividerBounds.width - 1, y);
            int center = layout.dividerBounds.x + layout.dividerBounds.width / 2;
            g.drawLine(center - 7, y - 1, center + 7, y - 1);
            g.drawLine(center - 7, y + 1, center + 7, y + 1);
        }
    }

    private void paintPinnedScrollIndicator(Graphics2D g, PinnedLayout layout, int bodyTop) {
        int contentHeight = layout.section.bodyBounds.height;
        if (contentHeight <= layout.bodyViewportHeight) {
            return;
        }
        int trackX = layout.bounds.x + layout.bounds.width - 4;
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(trackX, bodyTop, 4, layout.bodyViewportHeight);
        int thumbHeight = Math.max(12,
                layout.bodyViewportHeight * layout.bodyViewportHeight / contentHeight);
        int maxThumbY = Math.max(0, layout.bodyViewportHeight - thumbHeight);
        int maxScroll = contentHeight - layout.bodyViewportHeight;
        int thumbY = bodyTop + (maxScroll == 0 ? 0
                : layout.section.folder.getPinnedScrollY() * maxThumbY / maxScroll);
        Color thumb = UIManager.getColor("ScrollBar.thumb");
        g.setColor(thumb == null ? Color.gray : thumb);
        g.fillRect(trackX, thumbY, 4, thumbHeight);
    }

    private void paintPinnedSelectionOverlays(Graphics2D g, Section section) {
        if (handler == null || handler.getTileset().size() == 0) {
            return;
        }
        g.setColor(Color.red);
        drawTileBoundsInSection(g, handler.getTileIndexSelected(), section);
        if (multiSelectionEnabled && !rangeSelectedIndices.isEmpty()) {
            for (int i : rangeSelectedIndices) {
                if (i == handler.getTileIndexSelected()) {
                    continue;
                }
                drawTileBoundsInSection(g, i, section);
            }
        } else if (!multiSelectionEnabled && !multiSelected.isEmpty()) {
            g.setColor(new Color(80, 180, 255));
            for (int index : multiSelected) {
                drawTileBoundsInSection(g, index, section);
            }
        }
    }

    private void paintEmptySlots(Graphics2D g, Section section) {
        if (section.folder != null && !section.folder.isGridLinesVisible()) {
            return;
        }
        Color line = UIManager.getColor("Label.disabledForeground");
        if (line == null) {
            line = Color.lightGray;
        }
        g.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), 90));
        BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{2.0f, 2.0f}, 0.0f);
        g.setStroke(dotted);
        for (int row = 0; row < section.gridRows; row++) {
            for (int col = 0; col < section.columns; col++) {
                g.drawRect(col * tilePixelSize + 1,
                        section.bodyBounds.y + row * tilePixelSize + 1,
                        tilePixelSize - 3, tilePixelSize - 3);
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    private void paintAddRowCell(Graphics2D g, Section section) {
        if (section.addRowBounds == null) {
            return;
        }
        Rectangle r = section.addRowBounds;
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) {
            bg = Color.darkGray;
        }
        if (fg == null) {
            fg = Color.white;
        }
        g.setColor(new Color(Math.min(255, bg.getRed() + 20),
                Math.min(255, bg.getGreen() + 20),
                Math.min(255, bg.getBlue() + 20)));
        int buttonSize = Math.min(r.width, r.height) - 4;
        int buttonX = r.x + (r.width - buttonSize) / 2;
        int buttonY = r.y + (r.height - buttonSize) / 2;
        g.fillRect(buttonX, buttonY, buttonSize, buttonSize);
        g.setColor(fg);
        g.drawRect(buttonX, buttonY, buttonSize - 1, buttonSize - 1);
        int cx = buttonX + buttonSize / 2;
        int cy = buttonY + buttonSize / 2;
        int arm = Math.max(3, buttonSize / 3);
        g.drawLine(cx - arm, cy, cx + arm, cy);
        g.drawLine(cx, cy - arm, cx, cy + arm);
    }

    public void updateTile(int index) {
        Graphics g = display.getGraphics();
        Tile tile = handler.getTileset().get(index);
        for (Placement placement : placements) {
            if (placement.tileIndex == index) {
                g.drawImage(getDisplayThumbnail(tile, placement.section),
                        placement.bounds.x, placement.bounds.y, null);
            }
        }
    }

    public void updateTiles(ArrayList<Integer> indices) {
        for (int i = 0; i < indices.size(); i++) {
            updateTile(indices.get(i));
        }
    }

    /* -------------------- Organize drag (folders / layout slots) -------------------- */

    private void drawTileDrag(Graphics g) {
        Section target = getSectionAt(mouseX, mouseY);
        Point sourcePoint = getSourcePoint(mouseX, mouseY);
        int dx = mouseX - sourcePoint.x;
        int dy = mouseY - sourcePoint.y;
        if (target != null) {
            g.setColor(Color.orange);
            if (target.headerBounds != null && target.headerBounds.contains(sourcePoint.x, sourcePoint.y)) {
                Rectangle r = offsetRect(target.headerBounds, dx, dy);
                g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            } else if (!target.allTiles && target.columns > 0) {
                if (target.addRowBounds != null && target.addRowBounds.contains(sourcePoint.x, sourcePoint.y)) {
                    Rectangle r = offsetRect(target.addRowBounds, dx, dy);
                    g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
                } else {
                    Point cell = getSlotCellAt(target, sourcePoint.x, sourcePoint.y);
                    g.drawRect(cell.x * tilePixelSize + dx,
                            target.bodyBounds.y + cell.y * tilePixelSize + dy,
                            tilePixelSize - 1, tilePixelSize - 1);
                }
            } else {
                Rectangle r = offsetRect(target.bodyBounds, dx, dy);
                g.drawRect(r.x, r.y, r.width - 1, Math.max(r.height - 1, headerHeight));
            }
        }
        Graphics2D g2d = (Graphics2D) g;
        java.awt.Composite oldComposite = g2d.getComposite();
        g2d.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.7f));
        g.drawImage(handler.getTileset().get(tileDragIndex).getThumbnail(), mouseX + 4, mouseY + 4, null);
        g2d.setComposite(oldComposite);
        if (tileDragGroup && multiSelected.size() > 1) {
            String label = "x" + multiSelected.size();
            g.setColor(new Color(80, 180, 255));
            g.fillRoundRect(mouseX + 2, mouseY - 12, g.getFontMetrics().stringWidth(label) + 8, 14, 6, 6);
            g.setColor(Color.white);
            g.drawString(label, mouseX + 6, mouseY - 1);
        }
    }

    private void dropTile(MouseEvent evt) {
        Section target = getSectionAt(evt.getX(), evt.getY());
        if (target == null || tileDragIndex < 0) {
            return;
        }
        Point sourcePoint = getSourcePoint(evt.getX(), evt.getY());
        Tile tile = handler.getTileset().get(tileDragIndex);

        if (target.allTiles) {
            //Dropping on All Tiles takes the tile out of its folder
            if (tile.getPaletteFolder().isEmpty()) {
                return;
            }
            tile.setPaletteFolder(PaletteFolder.UNSORTED);
            tile.setPaletteSlot(-1);
        } else {
            String targetPath = target.folder.getPath();
            boolean overHeader = target.headerBounds != null && target.headerBounds.contains(sourcePoint.x, sourcePoint.y);
            if (target.addRowBounds != null && target.addRowBounds.contains(sourcePoint.x, sourcePoint.y)) {
                addLayoutRow(target);
                return;
            }
            if (!overHeader && target.columns > 0 && !target.isCollapsed()) {
                //Place into a layout template cell; swap with the occupant if any
                Point cell = getSlotCellAt(target, sourcePoint.x, sourcePoint.y);
                int slot = cell.y * target.columns + cell.x;
                Integer occupant = getSlotOccupant(target, cell.x, cell.y);
                if (occupant != null && occupant != tileDragIndex) {
                    Tile other = handler.getTileset().get(occupant);
                    other.setPaletteSlot(targetPath,
                            tile.getPaletteSlot(targetPath));
                }
                tile.setPaletteFolder(targetPath);
                tile.setPaletteSlot(slot);
            } else {
                if (tile.getPaletteFolder().equals(targetPath)) {
                    return; //Already there; ordering follows the tileset order
                }
                tile.setPaletteFolder(targetPath);
                tile.setPaletteSlot(-1);
            }
        }
        updateLayout();
        repaint();
    }

    /**
     * Drops the Ctrl / Shift multi selection as a group, in tileset index order.
     */
    private void dropSelectedTilesGroup(MouseEvent evt) {
        Section target = getSectionAt(evt.getX(), evt.getY());
        if (target == null) {
            return;
        }
        ArrayList<Integer> selected = new ArrayList<>(multiSelected);
        java.util.Collections.sort(selected);
        if (target.allTiles) {
            //Dropping on All Tiles takes the group out of its folder
            for (int index : selected) {
                Tile tile = handler.getTileset().get(index);
                tile.setPaletteFolder(PaletteFolder.UNSORTED);
                tile.setPaletteSlot(-1);
            }
            updateLayout();
            repaint();
            return;
        }
        dragSelectionIndices = selected;
        dropSelectedTilesToFolder(evt);
        dragSelectionIndices = null;
    }

    private boolean dropSelectedTilesToFolder(MouseEvent evt) {
        Section target = getSectionAt(evt.getX(), evt.getY());
        if (target == null || target.allTiles) {
            return false;
        }
        Point sourcePoint = getSourcePoint(evt.getX(), evt.getY());
        ArrayList<Integer> selected = dragSelectionIndices != null
                ? new ArrayList<>(dragSelectionIndices) : getIndicesSelected();
        if (selected.isEmpty()) {
            return false;
        }
        String sourcePath = rangeSelectionSection != null
                && !rangeSelectionSection.allTiles && rangeSelectionSection.folder != null
                ? rangeSelectionSection.folder.getPath() : null;
        if (target.addRowBounds != null && target.addRowBounds.contains(sourcePoint.x, sourcePoint.y)) {
            int firstNewRowSlot = target.folder.getRows() * target.columns;
            target.folder.setRows(target.folder.getRows() + 1);
            target.folder.setCollapsed(false);
            target.folder.setPinned(true);
            placeSelectedTilesInFolder(selected, sourcePath, target, firstNewRowSlot, true);
        } else if (target.columns > 0 && !target.isCollapsed()
                && target.bodyBounds != null && target.bodyBounds.contains(sourcePoint.x, sourcePoint.y)) {
            Point cell = getSlotCellAt(target, sourcePoint.x, sourcePoint.y);
            placeSelectedTilesInFolder(selected, sourcePath, target,
                    cell.y * target.columns + cell.x, false);
        } else {
            moveTilesToFolder(selected, sourcePath, target.folder.getPath());
            target.folder.setCollapsed(false);
        }
        updateLayout();
        if (dialog != null) {
            dialog.updateViewTileIndex();
        }
        handler.getMainFrame().updateTileSelectorLayout();
        return true;
    }

    private void placeSelectedTilesInFolder(ArrayList<Integer> selected, String sourcePath,
            Section target, int firstSlot, boolean alignTopLeft) {
        if (target.columns <= 0) {
            moveTilesToFolder(selected, sourcePath, target.folder.getPath());
            return;
        }
        if (placeSelectedTilesPreservingShape(selected, sourcePath,
                target, firstSlot, alignTopLeft)) {
            target.folder.setCollapsed(false);
            return;
        }
        int slot = Math.max(0, firstSlot);
        for (int index : selected) {
            Tile tile = handler.getTileset().get(index);
            while (slotOccupiedByOther(target, slot, selected)) {
                slot++;
            }
            moveTileOccurrence(tile, sourcePath, target.folder.getPath(), slot);
            int rowEnd = slot / target.columns + Math.max(1, getDisplayHeight(tile, target));
            if (rowEnd > target.folder.getRows()) {
                target.folder.setRows(rowEnd);
            }
            slot += Math.max(1, getDisplayWidth(tile, target));
        }
        target.folder.setCollapsed(false);
    }

    private boolean placeSelectedTilesPreservingShape(ArrayList<Integer> selected,
            String sourcePath, Section target, int firstSlot, boolean alignTopLeft) {
        Section source = rangeSelectionSection;
        if (source == null || source.allTiles || source.columns <= 0 || selected.isEmpty()) {
            return false;
        }
        if (sourcePath == null || !source.folder.getPath().equals(sourcePath)) {
            return false;
        }
        for (int index : selected) {
            if (handler.getTileset().get(index).getPaletteSlot(sourcePath) < 0) {
                return false;
            }
        }

        int anchorIndex = selected.contains(dragSelectionAnchorIndex)
                ? dragSelectionAnchorIndex : selected.get(0);
        int anchorSlot = handler.getTileset().get(anchorIndex).getPaletteSlot(sourcePath);
        int anchorCol = anchorSlot % source.columns;
        int anchorRow = anchorSlot / source.columns;
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        for (int index : selected) {
            Tile tile = handler.getTileset().get(index);
            int slot = tile.getPaletteSlot(sourcePath);
            int col = slot % source.columns;
            int row = slot / source.columns;
            minCol = Math.min(minCol, col);
            minRow = Math.min(minRow, row);
            maxCol = Math.max(maxCol, col);
        }
        if (maxCol - minCol + 1 > target.columns) {
            return false;
        }

        int targetCol = firstSlot % target.columns;
        int targetRow = firstSlot / target.columns;
        int deltaCol = alignTopLeft ? targetCol - minCol : targetCol - anchorCol;
        int deltaRow = alignTopLeft ? targetRow - minRow : targetRow - anchorRow;
        deltaCol = Math.max(-minCol,
                Math.min(target.columns - 1 - maxCol, deltaCol));
        deltaRow = Math.max(-minRow, deltaRow);

        String targetPath = target.folder.getPath();
        for (int index : selected) {
            Tile tile = handler.getTileset().get(index);
            int sourceSlot = tile.getPaletteSlot(sourcePath);
            int col = sourceSlot % source.columns + deltaCol;
            int row = sourceSlot / source.columns + deltaRow;
            int newSlot = row * target.columns + col;
            tile.setPaletteSlot(targetPath, newSlot);
            int rowEnd = row + Math.max(1, getDisplayHeight(tile, target));
            target.folder.setRows(Math.max(target.folder.getRows(), rowEnd));
        }
        if (!sourcePath.equals(targetPath)) {
            for (int index : selected) {
                handler.getTileset().get(index).removePaletteFolder(sourcePath);
            }
        }
        return true;
    }

    private void moveTileOccurrence(Tile tile, String sourcePath,
            String targetPath, int targetSlot) {
        if (sourcePath == null) {
            tile.setPaletteFolder(targetPath);
            tile.setPaletteSlot(targetSlot);
        } else if (sourcePath.equals(targetPath)) {
            tile.setPaletteSlot(targetPath, targetSlot);
        } else {
            tile.addPaletteFolder(targetPath, targetSlot);
            tile.removePaletteFolder(sourcePath);
        }
    }

    private boolean slotOccupiedByOther(Section section, int slot, ArrayList<Integer> movingIndices) {
        int col = slot % section.columns;
        int row = slot / section.columns;
        for (int index : section.tileIndices) {
            if (movingIndices.contains(index)) {
                continue;
            }
            Tile tile = handler.getTileset().get(index);
            int otherSlot = tile.getPaletteSlot(section.folder.getPath());
            if (otherSlot < 0) {
                continue;
            }
            int displayWidth = getDisplayWidth(tile, section);
            int displayHeight = getDisplayHeight(tile, section);
            int otherCol = otherSlot % section.columns;
            int otherRow = otherSlot / section.columns;
            if (col >= otherCol && col < otherCol + displayWidth
                    && row >= otherRow && row < otherRow + displayHeight) {
                return true;
            }
        }
        return false;
    }

    private Point getSlotCellAt(Section section, int x, int y) {
        int col = Math.max(0, Math.min(section.columns - 1, x / tilePixelSize));
        int row = Math.max(0, Math.min(section.gridRows - 1, (y - section.bodyBounds.y) / tilePixelSize));
        return new Point(col, row);
    }

    /** The tile of the section whose explicit slot area covers the given cell. */
    private Integer getSlotOccupant(Section section, int col, int row) {
        for (int index : section.tileIndices) {
            Tile tile = handler.getTileset().get(index);
            int slot = tile.getPaletteSlot(section.folder.getPath());
            if (slot < 0) {
                continue;
            }
            int tileRow = slot / section.columns;
            int tileCol = slot % section.columns;
            int displayWidth = getDisplayWidth(tile, section);
            int displayHeight = getDisplayHeight(tile, section);
            if (col >= tileCol && col < tileCol + displayWidth
                    && row >= tileRow && row < tileRow + displayHeight) {
                return index;
            }
        }
        return null;
    }

    /* -------------------- Hit testing -------------------- */

    private Section getHeaderSectionAt(int x, int y) {
        Point sourcePoint = getSourcePoint(x, y);
        x = sourcePoint.x;
        y = sourcePoint.y;
        for (Section section : sections) {
            if (section.headerBounds != null && section.headerBounds.contains(x, y)) {
                return section;
            }
        }
        return null;
    }

    private Section getSectionAt(int x, int y) {
        Point sourcePoint = getSourcePoint(x, y);
        x = sourcePoint.x;
        y = sourcePoint.y;
        for (Section section : sections) {
            if (section.headerBounds != null && section.headerBounds.contains(x, y)) {
                return section;
            }
            if (section.bodyBounds != null && section.bodyBounds.contains(x, y)) {
                return section;
            }
        }
        return null;
    }

    private Section getAddRowSectionAt(int x, int y) {
        Point sourcePoint = getSourcePoint(x, y);
        x = sourcePoint.x;
        y = sourcePoint.y;
        for (Section section : sections) {
            if (section.addRowBounds != null && section.addRowBounds.contains(x, y)) {
                return section;
            }
        }
        return null;
    }

    private int getIndexSelected(java.awt.event.MouseEvent evt) {
        Point sourcePoint = getSourcePoint(evt.getX(), evt.getY());
        if (!placements.isEmpty()) {
            for (Placement placement : placements) {
                if (placement.bounds.contains(sourcePoint.x, sourcePoint.y)) {
                    return placement.tileIndex;
                }
            }
            return -1;
        }
        for (int i = 0; i < boundingBoxes.size(); i++) {
            if (boundingBoxes.get(i).contains(sourcePoint.x, sourcePoint.y)) {
                return i;
            }
        }
        return -1;
    }

    private Point getSourcePoint(int x, int y) {
        for (PinnedLayout layout : getPinnedLayouts()) {
            if (layout.bounds.contains(x, y)) {
                if (y < layout.bounds.y + headerHeight + 1) {
                    return new Point(x + layout.section.headerBounds.x - layout.bounds.x,
                            y + layout.section.headerBounds.y - layout.bounds.y);
                }
                return new Point(x + layout.section.bodyBounds.x - layout.bounds.x,
                        y - (layout.bounds.y + headerHeight + 1)
                                + layout.section.bodyBounds.y
                                + layout.section.folder.getPinnedScrollY());
            }
        }
        return new Point(x, y);
    }

    private ArrayList<PinnedLayout> getPinnedLayouts() {
        Rectangle visible = getVisibleRect();
        ArrayList<Section> active = new ArrayList<>();
        int maxStack = getMaxPinnedStackHeight(visible);
        for (Section section : sections) {
            if (section.folder != null && section.folder.isPinned()
                    && section.headerBounds != null
                    && shouldActivatePinnedSection(section, visible, maxStack)) {
                active.add(section);
            }
        }
        if (active.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<PinnedLayout> result;
        boolean added;
        do {
            result = buildPinnedLayouts(active, visible);
            int coveredBottom = result.get(result.size() - 1).bounds.y
                    + result.get(result.size() - 1).bounds.height;
            added = false;
            for (Section section : sections) {
                if (!active.contains(section) && section.folder != null
                        && section.folder.isPinned() && section.headerBounds != null
                        && (usesPinnedBody(section)
                        ? shouldActivatePinnedSection(section, visible, maxStack)
                        : section.headerBounds.y < coveredBottom)) {
                    active.add(section);
                    added = true;
                }
            }
            if (added) {
                active.sort((a, b) -> Integer.compare(sections.indexOf(a), sections.indexOf(b)));
            }
        } while (added);
        return result;
    }

    private boolean shouldActivatePinnedSection(Section section, Rectangle visible,
            int maxStack) {
        if (usesPinnedBody(section)) {
            int index = sections.indexOf(section);
            if (index >= 0 && index + 1 < sections.size()) {
                Section next = sections.get(index + 1);
                int reservedHeaders = getPinnedAncestorHeaderHeight(section, visible);
                int available = Math.max(headerHeight, maxStack - reservedHeaders);
                int sectionHeight = headerHeight + 1 + section.bodyBounds.height;
                int activationBottom = visible.y + reservedHeaders
                        + Math.min(sectionHeight, available);
                return next.headerBounds != null && next.headerBounds.y <= activationBottom;
            }
        }
        return section.headerBounds.y < visible.y;
    }

    private boolean usesPinnedBody(Section section) {
        if (section.allTiles || section.isCollapsed() || section.bodyBounds == null
                || section.bodyBounds.height == 0) {
            return false;
        }
        int index = sections.indexOf(section);
        if (index >= 0 && index + 1 < sections.size()) {
            Section next = sections.get(index + 1);
            if (!next.allTiles && next.folder != null && section.folder != null
                    && next.folder.getPath().startsWith(section.folder.getPath() + "/")) {
                return false;
            }
        }
        return true;
    }

    private int getPinnedAncestorHeaderHeight(Section section, Rectangle visible) {
        if (section.folder == null) {
            return 0;
        }
        int height = 0;
        String path = section.folder.getPath();
        for (Section candidate : sections) {
            if (candidate == section || candidate.folder == null || candidate.allTiles
                    || !candidate.folder.isPinned() || candidate.headerBounds == null) {
                continue;
            }
            if (path.startsWith(candidate.folder.getPath() + "/")
                    && candidate.headerBounds.y < visible.y) {
                height += headerHeight;
            }
        }
        return height;
    }

    private ArrayList<PinnedLayout> buildPinnedLayouts(ArrayList<Section> active,
            Rectangle visible) {
        int expanded = 0;
        for (Section section : active) {
            if (usesPinnedBody(section)) {
                expanded++;
            }
        }
        ArrayList<PinnedLayout> result = new ArrayList<>();
        int maxStack = getMaxPinnedStackHeight(visible);
        int headerBudget = active.size() * headerHeight;
        int bodyBudget = Math.max(0, maxStack - headerBudget
                - active.size() - expanded * PINNED_DIVIDER_HEIGHT);
        int remainingExpanded = expanded;
        int y = visible.y;
        for (Section section : active) {
            int bodyHeight = 0;
            if (usesPinnedBody(section)) {
                int desired = section.folder.getPinnedViewportHeight();
                if (desired <= 0) {
                    desired = Math.max(MIN_PINNED_BODY_HEIGHT, visible.height / 10);
                }
                int reserve = Math.max(0, remainingExpanded - 1)
                        * Math.min(MIN_PINNED_BODY_HEIGHT,
                        remainingExpanded == 0 ? 0 : bodyBudget / remainingExpanded);
                int available = Math.max(0, bodyBudget - reserve);
                if (available < MIN_PINNED_BODY_HEIGHT && remainingExpanded > 0) {
                    available = bodyBudget / remainingExpanded;
                }
                bodyHeight = Math.min(section.bodyBounds.height,
                        Math.min(desired, available));
                bodyBudget -= bodyHeight;
                remainingExpanded--;
            }
            int maxScroll = Math.max(0, section.bodyBounds == null
                    ? 0 : section.bodyBounds.height - bodyHeight);
            section.folder.setPinnedScrollY(Math.min(section.folder.getPinnedScrollY(), maxScroll));
            int dividerHeight = bodyHeight > 0 ? PINNED_DIVIDER_HEIGHT : 0;
            int height = headerHeight + (bodyHeight > 0
                    ? 1 + bodyHeight + dividerHeight : 0);
            Rectangle bounds = new Rectangle(visible.x, y,
                    Math.min(section.headerBounds.width, visible.width), height);
            Rectangle dividerBounds = bodyHeight > 0
                    ? new Rectangle(bounds.x, bounds.y + bounds.height - dividerHeight,
                    bounds.width, dividerHeight) : null;
            result.add(new PinnedLayout(section, bounds, bodyHeight, dividerBounds));
            y += height;
        }
        return result;
    }

    private int getMaxPinnedStackHeight(Rectangle visible) {
        return Math.max(headerHeight, (int) (visible.height * 0.45));
    }

    private PinnedLayout getPinnedDividerAt(Point point) {
        for (PinnedLayout layout : getPinnedLayouts()) {
            if (layout.dividerBounds != null && layout.dividerBounds.contains(point)) {
                return layout;
            }
        }
        return null;
    }

    private void formMouseMoved(MouseEvent evt) {
        setCursor(getPinnedDividerAt(evt.getPoint()) == null
                ? Cursor.getDefaultCursor()
                : Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
    }

    private void scrollPinnedFolder(MouseWheelEvent evt) {
        for (PinnedLayout layout : getPinnedLayouts()) {
            if (layout.bodyViewportHeight <= 0 || !layout.bounds.contains(evt.getPoint())
                    || evt.getY() < layout.bounds.y + headerHeight + 1) {
                continue;
            }
            int maxScroll = Math.max(0,
                    layout.section.bodyBounds.height - layout.bodyViewportHeight);
            if (maxScroll == 0) {
                return;
            }
            int old = layout.section.folder.getPinnedScrollY();
            int next = Math.max(0, Math.min(maxScroll,
                    old + evt.getWheelRotation() * tilePixelSize * 3));
            if (next != old) {
                layout.section.folder.setPinnedScrollY(next);
                evt.consume();
                repaint();
                return;
            }
            break; //At an edge: continue scrolling the main Tile List.
        }
        scrollMainTileList(evt);
    }

    private void scrollMainTileList(MouseWheelEvent evt) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class, this);
        if (scrollPane == null) {
            return;
        }
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        int direction = evt.getWheelRotation() < 0 ? -1 : 1;
        int delta;
        if (evt.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            delta = direction * bar.getBlockIncrement(direction);
        } else {
            delta = evt.getUnitsToScroll() * bar.getUnitIncrement(direction);
        }
        bar.setValue(bar.getValue() + delta);
        evt.consume();
    }

    private Rectangle offsetRect(Rectangle r, int dx, int dy) {
        return new Rectangle(r.x + dx, r.y + dy, r.width, r.height);
    }

    /* -------------------- Context menus -------------------- */

    private void showTileMenu(int index, MouseEvent evt) {
        Tile tile = handler.getTileset().get(index);
        Section occurrence = getSectionAt(evt.getX(), evt.getY());
        String occurrenceFolder = occurrence != null && !occurrence.allTiles
                ? occurrence.folder.getPath() : PaletteFolder.UNSORTED;
        JPopupMenu menu = new JPopupMenu();

        String name = tile.getPaletteName();
        JMenuItem miTitle = new JMenuItem("Tile " + index + (name.isEmpty() ? "" : " - " + name));
        miTitle.setEnabled(false);
        menu.add(miTitle);
        menu.addSeparator();

        JMenuItem miRename = new JMenuItem("Rename Tile...");
        miRename.addActionListener(e -> renameTile(index));
        menu.add(miRename);

        ArrayList<Integer> single = new ArrayList<>();
        single.add(index);
        menu.add(buildFolderMoveMenu(single, occurrenceFolder));

        JMenuItem miRemoveFolder = new JMenuItem("Remove from Folder");
        miRemoveFolder.setEnabled(!occurrenceFolder.isEmpty());
        miRemoveFolder.addActionListener(e -> {
            tile.removePaletteFolder(occurrenceFolder);
            updateLayout();
            repaint();
        });
        menu.add(miRemoveFolder);

        JMenuItem miDisplaySize = new JMenuItem("Folder Display Size...");
        miDisplaySize.setEnabled(!occurrenceFolder.isEmpty());
        miDisplaySize.addActionListener(e -> showFolderDisplaySizeDialog(index));
        menu.add(miDisplaySize);

        JMenu collisionMenu = new JMenu("Collision Defaults");
        JMenuItem miCollisionFootprint = new JMenuItem("Set Footprint...");
        miCollisionFootprint.addActionListener(e -> showCollisionFootprintDialog(index));
        collisionMenu.add(miCollisionFootprint);
        JMenuItem miCollision = new JMenuItem("Edit Defaults...");
        miCollision.addActionListener(e -> showCollisionDefaultsDialog(index));
        collisionMenu.add(miCollision);
        collisionMenu.addSeparator();
        JMenuItem miCopyCollision = new JMenuItem("Copy Defaults");
        miCopyCollision.addActionListener(e -> copyCollisionDefaults(tile));
        collisionMenu.add(miCopyCollision);
        JMenuItem miPasteCollision = new JMenuItem("Paste Defaults");
        miPasteCollision.setEnabled(CollisionDefaultsClipboard.hasAllLayers());
        miPasteCollision.addActionListener(e -> pasteCollisionDefaults(tile));
        collisionMenu.add(miPasteCollision);
        menu.add(collisionMenu);

        JMenuItem miClearSlot = new JMenuItem("Clear Layout Slot");
        miClearSlot.setEnabled(!occurrenceFolder.isEmpty()
                && tile.getPaletteSlot(occurrenceFolder) >= 0);
        miClearSlot.addActionListener(e -> {
            tile.setPaletteSlot(occurrenceFolder, -1);
            updateLayout();
            repaint();
        });
        menu.add(miClearSlot);

        menu.show(this, evt.getX(), evt.getY());
    }

    private void showMultiSelectionMenu(MouseEvent evt) {
        JPopupMenu menu = new JPopupMenu();
        int count = multiSelected.size();

        JMenuItem miTitle = new JMenuItem(count + " tiles selected (Ctrl+click)");
        miTitle.setEnabled(false);
        menu.add(miTitle);
        menu.addSeparator();

        String currentFolder = rangeSelectionSection != null
                && !rangeSelectionSection.allTiles && rangeSelectionSection.folder != null
                ? rangeSelectionSection.folder.getPath() : null;
        menu.add(buildFolderMoveMenu(new ArrayList<>(multiSelected), currentFolder));

        JMenuItem miClear = new JMenuItem("Clear Selection");
        miClear.addActionListener(e -> {
            multiSelected.clear();
            repaint();
        });
        menu.add(miClear);

        menu.show(this, evt.getX(), evt.getY());
    }

    /** "Move to Folder" submenu working on one or many tiles. */
    private JMenu buildFolderMoveMenu(ArrayList<Integer> indices, String currentFolder) {
        JMenu mFolders = new JMenu(indices.size() > 1
                ? "Move " + indices.size() + " Tiles to Folder" : "Move to Folder");
        JCheckBoxMenuItem miNone = new JCheckBoxMenuItem("All Tiles only",
                currentFolder != null && currentFolder.isEmpty());
        miNone.addActionListener(e -> moveTilesToFolder(indices, currentFolder,
                PaletteFolder.UNSORTED));
        mFolders.add(miNone);
        mFolders.addSeparator();
        for (PaletteFolder folder : handler.getTileset().getPaletteFolders()) {
            if (folder.getPath().isEmpty()) {
                continue;
            }
            String parent = Tileset.getParentFolderPath(folder.getPath());
            if (parent == null || handler.getTileset().getPaletteFolder(parent) == null) {
                mFolders.add(buildFolderDestinationMenu(folder, indices, currentFolder));
            }
        }
        mFolders.addSeparator();
        JMenuItem miNewFolder = new JMenuItem("New Folder...");
        miNewFolder.addActionListener(e -> {
            String path = promptNewFolder();
            if (path != null) {
                moveTilesToFolder(indices, currentFolder, path);
            }
        });
        mFolders.add(miNewFolder);
        return mFolders;
    }

    private JMenu buildFolderDestinationMenu(PaletteFolder folder,
            ArrayList<Integer> indices, String currentFolder) {
        JMenu menu = new JMenu(getFolderLeafName(folder.getPath()));
        JMenuItem moveHere = new JMenuItem(folder.getPath().equals(currentFolder)
                ? "Current Folder" : "Move Here");
        moveHere.setEnabled(!folder.getPath().equals(currentFolder));
        moveHere.addActionListener(e -> moveTilesToFolder(indices, currentFolder,
                folder.getPath()));
        menu.add(moveHere);

        String parentPath = folder.getPath();
        for (PaletteFolder child : handler.getTileset().getPaletteFolders()) {
            if (parentPath.equals(Tileset.getParentFolderPath(child.getPath()))) {
                menu.add(buildFolderDestinationMenu(child, indices, currentFolder));
            }
        }
        return menu;
    }

    private String getFolderLeafName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private void showFolderMenu(Section section, MouseEvent evt) {
        JPopupMenu menu = new JPopupMenu();
        PaletteFolder folder = section.allTiles ? null : section.folder;

        if (section.allTiles) {
            PaletteFolder allTilesFolder = section.folder != null ? section.folder
                    : handler.getTileset().getOrCreatePaletteFolder(PaletteFolder.UNSORTED);
            JCheckBoxMenuItem miPinned = new JCheckBoxMenuItem(
                    "Pin All Tiles While Scrolling", allTilesFolder.isPinned());
            miPinned.addActionListener(e -> {
                allTilesFolder.setPinned(miPinned.isSelected());
                repaint();
            });
            menu.add(miPinned);
            menu.addSeparator();
        }

        if (folder != null) {
            JMenuItem miNewSub = new JMenuItem("New Subfolder...");
            miNewSub.addActionListener(e -> {
                String name = sanitizeFolderName(JOptionPane.showInputDialog(getDialogParent(),
                        "Subfolder name (inside \"" + folder.getPath() + "\"):",
                        "New Subfolder...", JOptionPane.PLAIN_MESSAGE));
                if (name != null) {
                    handler.getTileset().getOrCreatePaletteFolderWithParents(
                            folder.getPath() + "/" + name);
                    folder.setCollapsed(false);
                    updateLayout();
                    repaint();
                }
            });
            menu.add(miNewSub);

            JMenuItem miRename = new JMenuItem("Rename / Move Folder...");
            miRename.addActionListener(e -> {
                String newPath = sanitizeFolderName((String) JOptionPane.showInputDialog(getDialogParent(),
                        "Folder path (\"/\" nests folders, e.g. \"Terrain/Grass\";\n"
                                + "subfolders and tiles follow along):",
                        "Rename Folder", JOptionPane.PLAIN_MESSAGE,
                        null, null, folder.getPath()));
                if (newPath != null && !newPath.equals(folder.getPath())
                        && handler.getTileset().getPaletteFolder(newPath) == null
                        && !newPath.startsWith(folder.getPath() + "/")) {
                    String parent = Tileset.getParentFolderPath(newPath);
                    if (parent != null) {
                        handler.getTileset().getOrCreatePaletteFolderWithParents(parent);
                    }
                    handler.getTileset().renamePaletteFolder(folder, newPath);
                    updateLayout();
                    repaint();
                }
            });
            menu.add(miRename);

            JMenuItem miExportFolder = new JMenuItem("Export This Folder...");
            miExportFolder.addActionListener(e -> exportFolder(folder));
            menu.add(miExportFolder);

            JCheckBoxMenuItem miPinned = new JCheckBoxMenuItem("Pin Folder While Scrolling",
                    folder.isPinned());
            miPinned.addActionListener(e -> {
                folder.setPinned(miPinned.isSelected());
                repaint();
            });
            menu.add(miPinned);

            JCheckBoxMenuItem miUseGrid = new JCheckBoxMenuItem("Show Grid Lines",
                    folder.isGridLinesVisible());
            miUseGrid.addActionListener(e -> {
                folder.setGridLinesVisible(miUseGrid.isSelected());
                updateLayout();
                repaint();
            });
            menu.add(miUseGrid);

            JMenuItem miColumns = new JMenuItem("Set Grid Size...");
            miColumns.addActionListener(e -> {
                int maxColumns = Math.max(1, handler.getTileset().size());
                SpinnerNumberModel columnsModel = new SpinnerNumberModel(
                        Math.max(1, folder.getColumns()), 1, maxColumns, 1);
                SpinnerNumberModel rowsModel = new SpinnerNumberModel(
                        folder.getRows(), 1, Math.max(folder.getRows(), 1024), 1);
                JSpinner columnsSpinner = new JSpinner(columnsModel);
                JSpinner rowsSpinner = new JSpinner(rowsModel);
                JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 6, 4));
                fields.add(new JLabel("Columns:"));
                fields.add(columnsSpinner);
                fields.add(new JLabel("Rows:"));
                fields.add(rowsSpinner);
                JButton fitRows = new JButton("Fit Rows to Tiles");
                fitRows.addActionListener(fitEvent -> rowsSpinner.setValue(
                        getRequiredFolderRows(folder, (Integer) columnsSpinner.getValue())));
                JPanel panel = new JPanel(new BorderLayout(0, 6));
                panel.add(fields, BorderLayout.CENTER);
                panel.add(fitRows, BorderLayout.SOUTH);
                int result = JOptionPane.showConfirmDialog(getDialogParent(), panel,
                        "Layout Template", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    folder.setColumns((Integer) columnsSpinner.getValue());
                    folder.setRows((Integer) rowsSpinner.getValue());
                    updateLayout();
                    repaint();
                }
            });
            menu.add(miColumns);

            //Reordering swaps the folder with its previous / next sibling
            //(folders sharing the same parent)
            ArrayList<PaletteFolder> siblings = getSiblingFolders(folder);
            int siblingPos = siblings.indexOf(folder);

            JMenuItem miUp = new JMenuItem("Move Up");
            miUp.setEnabled(siblingPos > 0);
            miUp.addActionListener(e -> {
                swapFolders(folder, siblings.get(siblingPos - 1));
                updateLayout();
                repaint();
            });
            menu.add(miUp);

            JMenuItem miDown = new JMenuItem("Move Down");
            miDown.setEnabled(siblingPos >= 0 && siblingPos < siblings.size() - 1);
            miDown.addActionListener(e -> {
                swapFolders(folder, siblings.get(siblingPos + 1));
                updateLayout();
                repaint();
            });
            menu.add(miDown);

            JMenuItem miDelete = new JMenuItem("Delete Folder (subfolders too; tiles stay in All Tiles)");
            miDelete.addActionListener(e -> {
                handler.getTileset().removePaletteFolder(folder);
                updateLayout();
                repaint();
            });
            menu.add(miDelete);
            menu.addSeparator();
        }

        addCommonFolderItems(menu);
        menu.show(this, evt.getX(), evt.getY());
    }

    private void showBackgroundMenu(MouseEvent evt) {
        JPopupMenu menu = new JPopupMenu();
        addCommonFolderItems(menu);
        menu.show(this, evt.getX(), evt.getY());
    }

    private void addCommonFolderItems(JPopupMenu menu) {
        JMenuItem miNew = new JMenuItem("New Folder...");
        miNew.addActionListener(e -> promptNewFolder());
        menu.add(miNew);

        JMenuItem miImport = new JMenuItem("Import Folder...");
        miImport.addActionListener(e -> importFolder());
        menu.add(miImport);

        if (hasNamedFolders()) {
            JMenuItem miExpand = new JMenuItem("Expand All");
            miExpand.addActionListener(e -> setAllCollapsed(false));
            menu.add(miExpand);

            JMenuItem miCollapse = new JMenuItem("Collapse All");
            miCollapse.addActionListener(e -> setAllCollapsed(true));
            menu.add(miCollapse);
        }
    }

    private void exportFolder(PaletteFolder folder) {
        JFileChooser chooser = createFolderBundleChooser("Export Palette Folder");
        chooser.setSelectedFile(new File(folder.getPath().replace('/', '-')
                + "." + PaletteFolderBundleIO.EXTENSION));
        if (chooser.showSaveDialog(getDialogParent()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        File output = selected.getName().toLowerCase().endsWith(
                "." + PaletteFolderBundleIO.EXTENSION)
                ? selected : new File(selected.getPath() + "." + PaletteFolderBundleIO.EXTENSION);
        try {
            PaletteFolderBundleIO.write(output, handler.getTileset(), folder);
            JOptionPane.showMessageDialog(getDialogParent(),
                    "Folder exported to:\n" + output.getPath(), "Export Palette Folder",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getDialogParent(),
                    "Can't export folder:\n" + ex.getMessage(), "Export Palette Folder",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importFolder() {
        JFileChooser chooser = createFolderBundleChooser("Import Palette Folder");
        if (chooser.showOpenDialog(getDialogParent()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            int added = PaletteFolderBundleIO.read(chooser.getSelectedFile(), handler.getTileset());
            handler.getMainFrame().renderTilesetThumbnails();
            updateLayout();
            repaint();
            handler.getMainFrame().updateTileSelectorLayout();
            if (dialog != null) {
                dialog.updateViewTileIndex();
            }
            JOptionPane.showMessageDialog(getDialogParent(),
                    "Folder imported. " + added + " new tile(s) were added; matching tiles were reused.",
                    "Import Palette Folder", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getDialogParent(),
                    "Can't import folder:\n" + ex.getMessage(), "Import Palette Folder",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser createFolderBundleChooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "PDSMS palette folder (*." + PaletteFolderBundleIO.EXTENSION + ")",
                PaletteFolderBundleIO.EXTENSION));
        return chooser;
    }

    private java.awt.Component getDialogParent() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        return window == null ? this : window;
    }

    private void setAllCollapsed(boolean collapsed) {
        for (PaletteFolder folder : handler.getTileset().getPaletteFolders()) {
            if (!folder.getPath().isEmpty()) {
                folder.setCollapsed(collapsed);
            }
        }
        updateLayout();
        repaint();
    }

    /** Asks for a new folder name, creates it and returns its path (or null). */
    private String promptNewFolder() {
        String path = sanitizeFolderName(JOptionPane.showInputDialog(getDialogParent(),
                "Folder name (use \"/\" for a folder inside a folder,\n"
                        + "e.g. \"Terrain/HGSS Grass\"):", "New Folder...",
                JOptionPane.PLAIN_MESSAGE));
        if (path == null) {
            return null;
        }
        handler.getTileset().getOrCreatePaletteFolderWithParents(path);
        updateLayout();
        repaint();
        return path;
    }

    /** Folders sharing the folder's parent, in their display order. */
    private ArrayList<PaletteFolder> getSiblingFolders(PaletteFolder folder) {
        String parent = Tileset.getParentFolderPath(folder.getPath());
        ArrayList<PaletteFolder> siblings = new ArrayList<>();
        for (PaletteFolder f : handler.getTileset().getPaletteFolders()) {
            if (f.getPath().isEmpty()) {
                continue;
            }
            String fParent = Tileset.getParentFolderPath(f.getPath());
            if (parent == null ? fParent == null : parent.equals(fParent)) {
                siblings.add(f);
            }
        }
        return siblings;
    }

    private void swapFolders(PaletteFolder a, PaletteFolder b) {
        ArrayList<PaletteFolder> folders = handler.getTileset().getPaletteFolders();
        java.util.Collections.swap(folders, folders.indexOf(a), folders.indexOf(b));
    }

    /** Normalizes a folder path: no '|', trimmed segments, no empty segments. */
    private String sanitizeFolderName(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        for (String segment : input.replace("|", "").split("/")) {
            segment = segment.trim();
            if (segment.isEmpty()) {
                continue;
            }
            if (path.length() > 0) {
                path.append('/');
            }
            path.append(segment);
        }
        return path.length() == 0 ? null : path.toString();
    }

    private int getRequiredFolderRows(PaletteFolder folder, int columns) {
        int requiredRows = 1;
        for (Tile tile : handler.getTileset().getTiles()) {
            int slot = tile.getPaletteSlot(folder.getPath());
            if (slot >= 0) {
                requiredRows = Math.max(requiredRows, slot / Math.max(1, columns)
                        + Math.max(1, tile.getPaletteDisplayHeight()));
            }
        }
        return requiredRows;
    }

    private void moveTilesToFolder(ArrayList<Integer> indices, String folderPath) {
        moveTilesToFolder(indices, null, folderPath);
    }

    private void moveTilesToFolder(ArrayList<Integer> indices, String sourceFolder,
            String folderPath) {
        for (int index : indices) {
            Tile tile = handler.getTileset().get(index);
            if (sourceFolder == null || folderPath.isEmpty()) {
                tile.setPaletteFolder(folderPath);
                tile.setPaletteSlot(-1);
            } else if (!sourceFolder.equals(folderPath)) {
                tile.addPaletteFolder(folderPath, -1);
                tile.removePaletteFolder(sourceFolder);
            }
        }
        updateLayout();
        repaint();
    }

    private void renameTile(int index) {
        Tile tile = handler.getTileset().get(index);
        String name = (String) JOptionPane.showInputDialog(getDialogParent(),
                "Tile name (empty to clear):", "Rename Tile " + index,
                JOptionPane.PLAIN_MESSAGE, null, null, tile.getPaletteName());
        if (name != null) {
            tile.setPaletteName(name.replace("|", "").trim());
            repaint();
        }
    }

    private void showFolderDisplaySizeDialog(int index) {
        Tile tile = handler.getTileset().get(index);
        if (tile.getPaletteFolder().isEmpty()) {
            return;
        }
        SpinnerNumberModel widthModel = new SpinnerNumberModel(
                tile.getPaletteDisplayWidth(), 1, Tile.maxTileSize, 1);
        SpinnerNumberModel heightModel = new SpinnerNumberModel(
                tile.getPaletteDisplayHeight(), 1, Tile.maxTileSize, 1);
        JSpinner widthSpinner = new JSpinner(widthModel);
        JSpinner heightSpinner = new JSpinner(heightModel);
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 6, 4));
        panel.add(new JLabel("Folder width:"));
        panel.add(widthSpinner);
        panel.add(new JLabel("Folder height:"));
        panel.add(heightSpinner);
        panel.add(new JLabel("Real map size:"));
        panel.add(new JLabel(tile.getWidth() + " x " + tile.getHeight()));

        int result = JOptionPane.showConfirmDialog(getDialogParent(), panel,
                "Folder Display Size - Tile " + index,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            tile.setPaletteDisplayWidth((Integer) widthSpinner.getValue());
            tile.setPaletteDisplayHeight((Integer) heightSpinner.getValue());
            handler.getMainFrame().renderTileThumbnail(index);
            updateLayout();
            repaint();
            handler.getMainFrame().updateTileSelectorLayout();
        }
    }

    /* -------------------- Smart collision defaults (per cell) -------------------- */

    private void showCollisionFootprintDialog(int index) {
        Tile tile = handler.getTileset().get(index);
        SpinnerNumberModel widthModel = new SpinnerNumberModel(
                tile.getCollisionFootprintWidth(), 1, 32, 1);
        SpinnerNumberModel heightModel = new SpinnerNumberModel(
                tile.getCollisionFootprintHeight(), 1, 32, 1);
        SpinnerNumberModel anchorXModel = new SpinnerNumberModel(
                tile.getCollisionFootprintAnchorX(), 0,
                tile.getCollisionFootprintWidth() - 1, 1);
        SpinnerNumberModel anchorYModel = new SpinnerNumberModel(
                tile.getCollisionFootprintAnchorY(), 0,
                tile.getCollisionFootprintHeight() - 1, 1);
        JSpinner widthSpinner = new JSpinner(widthModel);
        JSpinner heightSpinner = new JSpinner(heightModel);
        JSpinner anchorXSpinner = new JSpinner(anchorXModel);
        JSpinner anchorYSpinner = new JSpinner(anchorYModel);
        JCheckBox followTileSize = new JCheckBox("Use Folder Display Size ("
                + tile.getPaletteDisplayWidth() + " x " + tile.getPaletteDisplayHeight() + ")",
                !tile.hasCustomCollisionFootprint());

        Runnable updateControls = () -> {
            boolean custom = !followTileSize.isSelected();
            if (!custom) {
                widthSpinner.setValue(tile.getPaletteDisplayWidth());
                heightSpinner.setValue(tile.getPaletteDisplayHeight());
                anchorXSpinner.setValue(0);
                anchorYSpinner.setValue(tile.getPaletteDisplayHeight() - 1);
            }
            widthSpinner.setEnabled(custom);
            heightSpinner.setEnabled(custom);
            anchorXSpinner.setEnabled(custom);
            anchorYSpinner.setEnabled(custom);
        };
        Runnable updateAnchorLimits = () -> {
            int maxX = (Integer) widthSpinner.getValue() - 1;
            int maxY = (Integer) heightSpinner.getValue() - 1;
            if ((Integer) anchorXSpinner.getValue() > maxX) {
                anchorXSpinner.setValue(maxX);
            }
            if ((Integer) anchorYSpinner.getValue() > maxY) {
                anchorYSpinner.setValue(maxY);
            }
            anchorXModel.setMaximum(maxX);
            anchorYModel.setMaximum(maxY);
        };
        followTileSize.addActionListener(e -> {
            updateControls.run();
            updateAnchorLimits.run();
        });
        widthSpinner.addChangeListener(e -> updateAnchorLimits.run());
        heightSpinner.addChangeListener(e -> updateAnchorLimits.run());
        updateControls.run();
        updateAnchorLimits.run();

        JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 6, 4));
        fields.add(new JLabel("Footprint width:"));
        fields.add(widthSpinner);
        fields.add(new JLabel("Footprint height:"));
        fields.add(heightSpinner);
        fields.add(new JLabel("Anchor column:"));
        fields.add(anchorXSpinner);
        fields.add(new JLabel("Anchor row (top = 0):"));
        fields.add(anchorYSpinner);

        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.add(new JLabel("The anchor cell aligns with the tile's map placement square."),
                BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(followTileSize, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(getDialogParent(), panel,
                "Collision Footprint - Tile " + index,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            if (followTileSize.isSelected()) {
                tile.resetCollisionFootprint();
            } else {
                tile.setCollisionFootprint((Integer) widthSpinner.getValue(),
                        (Integer) heightSpinner.getValue(),
                        (Integer) anchorXSpinner.getValue(),
                        (Integer) anchorYSpinner.getValue());
            }
            repaint();
        }
    }

    private void showCollisionDefaultsDialog(int index) {
        Tile tile = handler.getTileset().get(index);
        int gameIndex = handler.getGameIndex();
        CollisionTypes types = new CollisionTypes(gameIndex);
        int numLayers = CollisionTypes.numLayersPerGame[gameIndex];
        int w = tile.getCollisionFootprintWidth();
        int h = tile.getCollisionFootprintHeight();

        //Working copies of the per-cell grids
        int[][][] work = new int[numLayers][w][h];
        for (int layer = 0; layer < numLayers; layer++) {
            int[][] existing = tile.getCollisionDefaults().get(layer);
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    work[layer][i][j] = existing != null && i < existing.length && j < existing[0].length
                            ? existing[i][j] : -1;
                }
            }
        }

        int[] selectedCollisionValues = new int[numLayers];

        final int cellPx = Math.max(12, Math.min(48, 280 / Math.max(w, h)));
        JPanel[] gridPanels = new JPanel[numLayers];
        JPanel layersPanel = new JPanel(new java.awt.GridLayout(0,
                Math.min(2, numLayers), 8, 8));
        for (int layer = 0; layer < numLayers; layer++) {
            final int layerIndex = layer;
            JComboBox<String> valueCombo = new JComboBox<>();
            for (int value = 0; value < CollisionTypes.numCollisions; value++) {
                String collisionName = types.getCollisionName(layerIndex, value);
                valueCombo.addItem(String.format("%02X", value)
                        + (collisionName == null || collisionName.isEmpty()
                                ? "" : "  " + collisionName));
            }
            valueCombo.setSelectedIndex(0);
            valueCombo.addActionListener(e ->
                    selectedCollisionValues[layerIndex] = valueCombo.getSelectedIndex());
            JPanel gridPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int displayWidth = tile.getPaletteDisplayWidth();
                    int displayHeight = tile.getPaletteDisplayHeight();
                    int anchorX = tile.getCollisionFootprintAnchorX();
                    int anchorY = tile.getCollisionFootprintAnchorY();
                    int imageX = anchorX * cellPx;
                    int imageY = (anchorY - displayHeight + 1) * cellPx;
                    g.drawImage(tile.getPaletteThumbnail(), imageX, imageY,
                            displayWidth * cellPx, displayHeight * cellPx, null);
                    Graphics2D g2d = (Graphics2D) g;
                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                            int value = work[layerIndex][i][j];
                            if (value >= 0) {
                                Color fill = types.getFillColor(layerIndex, value);
                                g2d.setColor(new Color(fill.getRed(), fill.getGreen(),
                                        fill.getBlue(), 140));
                                g2d.fillRect(i * cellPx, j * cellPx, cellPx, cellPx);
                                g2d.setColor(CollisionTypes.getContrastColor(fill));
                                if (cellPx >= 22) {
                                    g2d.drawString(String.format("%02X", value),
                                            i * cellPx + cellPx / 2 - 7,
                                            j * cellPx + cellPx / 2 + 5);
                                }
                            }
                            g2d.setColor(new Color(0, 0, 0, 120));
                            g2d.drawRect(i * cellPx, j * cellPx,
                                    cellPx - 1, cellPx - 1);
                        }
                    }
                    g2d.setColor(Color.YELLOW);
                    g2d.drawRect(anchorX * cellPx + 1, anchorY * cellPx + 1,
                            cellPx - 3, cellPx - 3);
                    g2d.drawRect(anchorX * cellPx + 2, anchorY * cellPx + 2,
                            cellPx - 5, cellPx - 5);
                }
            };
            gridPanel.setPreferredSize(new Dimension(w * cellPx, h * cellPx));
            MouseAdapter painter = new MouseAdapter() {
                private void paintCell(MouseEvent e) {
                    int i = e.getX() / cellPx;
                    int j = e.getY() / cellPx;
                    if (i < 0 || i >= w || j < 0 || j >= h) {
                        return;
                    }
                    work[layerIndex][i][j] = SwingUtilities.isRightMouseButton(e)
                            ? -1 : selectedCollisionValues[layerIndex];
                    gridPanel.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    paintCell(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    paintCell(e);
                }
            };
            gridPanel.addMouseListener(painter);
            gridPanel.addMouseMotionListener(painter);
            gridPanels[layer] = gridPanel;

            JButton fillAll = new JButton("Fill Layer");
            fillAll.addActionListener(e -> {
                for (int[] column : work[layerIndex]) {
                    java.util.Arrays.fill(column, selectedCollisionValues[layerIndex]);
                }
                gridPanel.repaint();
            });
            JButton clearAll = new JButton("Clear Layer");
            clearAll.addActionListener(e -> {
                for (int[] column : work[layerIndex]) {
                    java.util.Arrays.fill(column, -1);
                }
                gridPanel.repaint();
            });
            JPanel layerButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            layerButtons.add(fillAll);
            layerButtons.add(clearAll);
            JPanel layerPanel = new JPanel(new BorderLayout(2, 4));
            layerPanel.setBorder(BorderFactory.createEtchedBorder());
            JPanel layerHeader = new JPanel(new BorderLayout(4, 2));
            layerHeader.add(new JLabel(CollisionTypes.layerName(gameIndex, layer),
                    SwingConstants.CENTER), BorderLayout.NORTH);
            layerHeader.add(valueCombo, BorderLayout.CENTER);
            layerPanel.add(layerHeader, BorderLayout.NORTH);
            JPanel centeredGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            centeredGrid.add(gridPanel);
            layerPanel.add(centeredGrid, BorderLayout.CENTER);
            layerPanel.add(layerButtons, BorderLayout.SOUTH);
            layersPanel.add(layerPanel);
        }

        JButton pasteDefaults = new JButton("Paste Defaults");
        pasteDefaults.setEnabled(CollisionDefaultsClipboard.hasAllLayers());
        JButton copyDefaults = new JButton("Copy Defaults");
        copyDefaults.addActionListener(e -> {
            CollisionDefaultsClipboard.copyAll(work, w, h,
                    tile.getCollisionFootprintAnchorX(),
                    tile.getCollisionFootprintAnchorY());
            pasteDefaults.setEnabled(true);
        });
        pasteDefaults.addActionListener(e -> {
            if (!CollisionDefaultsClipboard.hasAllLayers()) {
                return;
            }
            CollisionDefaultsClipboard.pasteAll(work, w, h,
                    tile.getCollisionFootprintAnchorX(),
                    tile.getCollisionFootprintAnchorY());
            for (JPanel gridPanel : gridPanels) {
                gridPanel.repaint();
            }
        });

        JPanel top = new JPanel(new java.awt.GridLayout(0, 1, 4, 2));
        top.add(new JLabel("Left click / drag a cell to stamp the selected collision,"));
        top.add(new JLabel("right click a cell to clear it (no default = untouched):"));
        top.add(new JLabel("The yellow cell is the tile's map placement anchor."));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(copyDefaults);
        buttons.add(pasteDefaults);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(top, BorderLayout.NORTH);
        if (numLayers > 2) {
            JScrollPane layerScroll = new JScrollPane(layersPanel);
            layerScroll.setPreferredSize(new Dimension(
                    Math.min(700, layersPanel.getPreferredSize().width + 24), 560));
            layerScroll.getVerticalScrollBar().setUnitIncrement(16);
            panel.add(layerScroll, BorderLayout.CENTER);
        } else {
            panel.add(layersPanel, BorderLayout.CENTER);
        }
        panel.add(buttons, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(getDialogParent(), panel,
                "Collision Defaults - Tile " + index + " (" + w + "x" + h
                        + ", anchor " + tile.getCollisionFootprintAnchorX()
                        + "," + tile.getCollisionFootprintAnchorY() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            for (int layer = 0; layer < numLayers; layer++) {
                tile.getCollisionDefaults().put(layer, work[layer]);
            }
            tile.pruneEmptyCollisionDefaults();
        }
    }

    private void copyCollisionDefaults(Tile tile) {
        CollisionDefaultsClipboard.copyAll(tile,
                CollisionTypes.numLayersPerGame[handler.getGameIndex()]);
    }

    private void pasteCollisionDefaults(Tile tile) {
        CollisionDefaultsClipboard.pasteAll(tile,
                CollisionTypes.numLayersPerGame[handler.getGameIndex()]);
        repaint();
    }

    /* -------------------- Tooltips -------------------- */

    @Override
    public String getToolTipText(MouseEvent evt) {
        if (handler == null || multiSelectionEnabled) {
            return null;
        }
        Section header = getHeaderSectionAt(evt.getX(), evt.getY());
        if (header != null) {
            return header.getDisplayName() + " - " + header.tileIndices.size() + " tiles"
                    + (!header.allTiles && header.columns > 0
                    ? ", " + header.columns + " column grid" : "");
        }
        int index = getIndexSelected(evt);
        if (index < 0) {
            return null;
        }
        Tile tile = handler.getTileset().get(index);
        StringBuilder sb = new StringBuilder("<html><b>Tile ").append(index).append("</b>");
        if (!tile.getPaletteName().isEmpty()) {
            sb.append(" - ").append(tile.getPaletteName());
        }
        sb.append("<br>").append(tile.getObjFilename());
        if (!tile.getPaletteFolder().isEmpty()) {
            sb.append("<br>Folder: ").append(tile.getPaletteFolder());
        }
        for (Map.Entry<Integer, int[][]> entry : tile.getCollisionDefaults().entrySet()) {
            int cells = 0;
            int uniform = -2; //-2 = unset, -3 = mixed values
            for (int[] column : entry.getValue()) {
                for (int value : column) {
                    if (value >= 0) {
                        cells++;
                        if (uniform == -2) {
                            uniform = value;
                        } else if (uniform != value) {
                            uniform = -3;
                        }
                    }
                }
            }
            if (cells > 0) {
                sb.append("<br>Collision L").append(entry.getKey()).append(": ");
                if (uniform >= 0) {
                    sb.append(String.format("%02X", uniform));
                    if (cells > 1) {
                        sb.append(" x").append(cells);
                    }
                } else {
                    sb.append(cells).append(" cells");
                }
            }
        }
        return sb.append("</html>").toString();
    }

    /* -------------------- Misc accessors -------------------- */

    /** Releases the Ctrl / Shift multi selection (e.g. when a new map is opened). */
    public void clearMultiSelection() {
        if (!multiSelected.isEmpty()) {
            multiSelected.clear();
            repaint();
        }
    }

    public ArrayList<Integer> getIndicesSelected() {
        if (!rangeSelectedIndices.isEmpty()) {
            return new ArrayList<>(rangeSelectedIndices);
        }
        ArrayList<Integer> indices = new ArrayList<>(1);
        indices.add(handler.getTileIndexSelected());
        return indices;
    }

    private ArrayList<Integer> getVisualRange(Section section, int anchorIndex, int endIndex) {
        ArrayList<Placement> orderedPlacements = new ArrayList<>();
        for (Placement placement : placements) {
            if (placement.section == section) {
                orderedPlacements.add(placement);
            }
        }
        orderedPlacements.sort((a, b) -> {
            int row = Integer.compare(a.bounds.y, b.bounds.y);
            return row != 0 ? row : Integer.compare(a.bounds.x, b.bounds.x);
        });
        ArrayList<Integer> ordered = new ArrayList<>();
        for (Placement placement : orderedPlacements) {
            if (!ordered.contains(placement.tileIndex)) {
                ordered.add(placement.tileIndex);
            }
        }
        int anchor = ordered.indexOf(anchorIndex);
        int end = ordered.indexOf(endIndex);
        if (anchor < 0 || end < 0) {
            ArrayList<Integer> single = new ArrayList<>();
            single.add(endIndex);
            return single;
        }
        int from = Math.min(anchor, end);
        int to = Math.max(anchor, end);
        return new ArrayList<>(ordered.subList(from, to + 1));
    }

    /** The named folder containing the most visibly selected tile occurrences. */
    public String getSelectedLayoutFolderPath() {
        if (rangeSelectionInitialized) {
            if (rangeSelectionSection == null) {
                return null;
            }
            return rangeSelectionSection.allTiles ? null : rangeSelectionSection.folder.getPath();
        }
        ArrayList<Integer> selected = getIndicesSelected();
        Section best = null;
        int bestCount = 0;
        for (Section section : sections) {
            if (section.allTiles || section.folder == null) {
                continue;
            }
            int count = 0;
            for (int index : selected) {
                if (section.tileIndices.contains(index)) {
                    count++;
                }
            }
            if (count > bestCount) {
                best = section;
                bestCount = count;
            }
        }
        return best == null ? null : best.folder.getPath();
    }

    /** Filters the numeric Tileset Editor range to occurrences visible in one folder. */
    public ArrayList<Integer> getSelectedIndicesInFolder(String folderPath) {
        ArrayList<Integer> selected = getIndicesSelected();
        selected.removeIf(index -> !handler.getTileset().get(index).isInPaletteFolder(folderPath));
        return selected;
    }

    private void drawTileBounds(Graphics g, int tileIndex) {
        if (tileIndex < 0 || tileIndex >= boundingBoxes.size()) {
            return;
        }
        if (!placements.isEmpty()) {
            for (Placement placement : placements) {
                if (placement.tileIndex == tileIndex) {
                    drawBoundsRect(g, placement.bounds);
                }
            }
            return;
        }
        Rectangle bounds = boundingBoxes.get(tileIndex);
        if (bounds.width <= 0) {
            return;
        }
        drawBoundsRect(g, bounds);
    }

    private void drawTileBoundsInSection(Graphics g, int tileIndex, Section section) {
        for (Placement placement : placements) {
            if (placement.tileIndex == tileIndex && placement.section == section) {
                drawBoundsRect(g, placement.bounds);
            }
        }
    }

    private void drawBoundsRect(Graphics g, Rectangle bounds) {
        Color borderColor = g.getColor();
        Color fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 100);
        g.setColor(fillColor);
        g.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        g.setColor(borderColor);
        g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }

    private BufferedImage getSubTilesetImage(int indexFrom, int indexTo) {
        int yMin = boundingBoxes.get(indexFrom).y;
        int yMax = yMin;
        for (int i = indexFrom; i <= indexTo; i++) {
            Rectangle bounds = boundingBoxes.get(i);
            int newY = bounds.y + bounds.height;
            if (newY > yMax) {
                yMax = newY;
            }
        }

        BufferedImage img = new BufferedImage(widthUnits * tilePixelSize, yMax - yMin, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        for (int i = indexFrom; i <= indexTo; i++) {
            BufferedImage tileImg = handler.getTileset().getTiles().get(i).getThumbnail();
            Rectangle bounds = boundingBoxes.get(i);
            g.drawImage(tileImg, bounds.x, bounds.y - yMin, null);
        }

        return img;
    }

    public BufferedImage getTilesetImage() {
        return display;
    }

    public int getTileSelectedY() {
        if (handler.getTileIndexSelected() < boundingBoxes.size()) {
            return Math.max(0, boundingBoxes.get(handler.getTileIndexSelected()).y);
        }
        return 0;
    }

    public void setIndexSecondTileSelected(int index) {
        this.indexSecondTileSelected = index;
        if (index < 0) {
            rangeSelectedIndices.clear();
            rangeSelectionSection = null;
            rangeSelectionInitialized = false;
            multiselecting = false;
            rangeAnchorFromRightClick = false;
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                formMouseDragged(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                formMouseMoved(e);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                formMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                formMouseReleased(e);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGap(0, 300, Short.MAX_VALUE)
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
