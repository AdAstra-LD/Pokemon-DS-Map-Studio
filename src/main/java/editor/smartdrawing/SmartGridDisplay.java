package editor.smartdrawing;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;

import editor.handler.MapEditorHandler;
import editor.grid.MapGrid;
import editor.mapdisplay.MapDisplay;
import editor.mapdisplay.ViewMode;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import utils.Utils;
import tileset.PaletteFolder;

/**
 * @author Trifindo, JackHack96
 */
public class SmartGridDisplay extends JPanel {

    private static BufferedImage gridImage = Utils.loadTexImageAsResource("/imgs/smartGrid.png");

    private MapEditorHandler handler;
    private boolean editable = true;
    private static final int FOLDER_HEADER_HEIGHT = 16;

    public SmartGridDisplay() {
        initComponents();

        //gridImage = Utils.loadImageAsResource("/imgs/smartGrid.png");
        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                SmartGrid.height * MapGrid.tileSize));
    }

    private void formMouseMoved(MouseEvent evt) {
        if (editable) {
            if (handler.getTileset().size() > 0) {
                int x = evt.getX() / MapGrid.tileSize;
                int gridIndex = getGridIndexAt(evt.getY());
                int y = gridIndex < 0 ? -1
                        : (evt.getY() - getGridY(gridIndex)) / MapGrid.tileSize;
                ;
                System.out.println(x + "  " + y);
                if (gridIndex < handler.getSmartGridArray().size() && gridIndex >= 0) {
                    if (!((y == 2) && (x == 4 || x == 3))) {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        setToolTipText(null);
                    } else {
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        setToolTipText("Select Smart Drawing");
                    }
                } else {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    setToolTipText("Right click for adding or removing Smart Drawing");
                }
            }
        }
    }

    private void formMousePressed(MouseEvent evt) {
        int folderHeaderIndex = getFolderHeaderIndexAt(evt.getY());
        if (folderHeaderIndex >= 0) {
            String folderPath = handler.getSmartGrid(folderHeaderIndex).getPaletteFolder();
            if (!folderPath.isEmpty()) {
                if (SwingUtilities.isLeftMouseButton(evt)) {
                    toggleSmartFolder(folderPath);
                } else if (SwingUtilities.isRightMouseButton(evt)) {
                    showSmartFolderMenu(folderHeaderIndex, evt);
                }
            }
            return;
        }
        if (editable) {
            if (handler.getTileset().size() > 0) {
                int x = evt.getX() / MapGrid.tileSize;
                int gridIndex = getGridIndexAt(evt.getY());
                int y = gridIndex < 0 ? -1
                        : (evt.getY() - getGridY(gridIndex)) / MapGrid.tileSize;
                //System.out.println(x + "  " + y);
                if (gridIndex < handler.getSmartGridArray().size() && gridIndex >= 0) {
                    if (!((y == 2) && (x == 4 || x == 3))) {
                        int[][] grid = handler.getSmartGrid(gridIndex).sgrid;
                        if (new Rectangle(SmartGrid.width, SmartGrid.height).contains(x, y)) {
                            if (SwingUtilities.isLeftMouseButton(evt)) {
                                if (handler.getTileSelected().isSizeOne()) {
                                    grid[x][y] = handler.getTileIndexSelected();
                                }
                            }
                            /*else if (SwingUtilities.isRightMouseButton(evt)) {
                        grid[x][y] = -1;
                    }*/
                            repaint();
                        }
                    } else {
                        handler.setSmartGridIndexSelected(gridIndex);
                        repaint();
                    }
                }

                if (SwingUtilities.isRightMouseButton(evt) && handler != null) {
                    if (gridIndex >= 0 && gridIndex < handler.getSmartGridArray().size()) {
                        handler.setSmartGridIndexSelected(gridIndex);
                    }

                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem item1 = new JMenuItem("Add Smart Painter");
                    JMenuItem item2 = new JMenuItem("Remove Smart Painter");
                    item1.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            handler.getSmartGridArray().add(new SmartGrid());
                            updateSize();
                            repaint();
                        }
                    });
                    item2.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (handler.getSmartGridArray().size() > 1) {
                                if (gridIndex >= 0 && gridIndex < handler.getSmartGridArray().size()) {
                                    handler.getSmartGridArray().remove(gridIndex);
                                    handler.setSmartGridIndexSelected(Math.max(0, gridIndex - 1));
                                    updateSize();
                                    repaint();
                                }
                            } else {
                                System.out.println("No se puede");
                                JOptionPane.showMessageDialog(menu,
                                        "There must me at least one Smart Painter",
                                        "Can't delete Smart Painter",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                    menu.add(item1);
                    menu.add(item2);
                    if (gridIndex >= 0) {
                        menu.add(buildFolderMenu(gridIndex));
                    }

                    menu.show(this, evt.getX(), evt.getY());
                }
            }
        } else {
            if (handler.getTileset().size() > 0) {
                int gridIndex = getGridIndexAt(evt.getY());
                if (gridIndex < handler.getSmartGridArray().size() && gridIndex >= 0) {
                    handler.setSmartGridIndexSelected(gridIndex);
                    repaint();

                    MapDisplay mapDisplay = handler.getMainFrame().getMapDisplay();
                    if (mapDisplay.getViewMode().getViewID() == ViewMode.ViewID.VIEW_ORTHO
                            && !mapDisplay.isSmartToolsEnabled()) {
                        if (mapDisplay.getEditMode() != MapDisplay.EditMode.MODE_INV_SMART_PAINT) {
                            mapDisplay.setEditMode(MapDisplay.EditMode.MODE_SMART_PAINT);
                            handler.getMainFrame().getJtbModeSmartPaint().setSelected(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gridImage != null && handler != null) {
            for (int k = 0; k < handler.getSmartGridArray().size(); k++) {
                paintFolderHeader(g, k);
                if (isSmartFolderCollapsed(handler.getSmartGrid(k).getPaletteFolder())) {
                    continue;
                }
                g.drawImage(gridImage, 0,
                        getGridY(k), null);
            }

        }

        if (handler != null) {
            for (int k = 0; k < handler.getSmartGridArray().size(); k++) {
                SmartGrid sg = handler.getSmartGrid(k);
                if (isSmartFolderCollapsed(sg.getPaletteFolder())) {
                    continue;
                }
                int[][] grid = sg.sgrid;
                for (int i = 0; i < SmartGrid.width; i++) {
                    for (int j = 0; j < SmartGrid.height; j++) {
                        int indexTile = grid[i][j];
                        if (indexTile != -1) {
                            try {
                                BufferedImage img = handler.getTileset().get(indexTile).getThumbnail();
                                g.drawImage(
                                        img,
                                        i * MapGrid.tileSize,
                                        getGridY(k) + j * MapGrid.tileSize,
                                        null);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            int index = handler.getSmartGridIndexSelected();
            if (index >= 0 && index < handler.getSmartGridArray().size()
                    && !isSmartFolderCollapsed(handler.getSmartGrid(index).getPaletteFolder())) {
                g.setColor(Color.red);
                g.drawRect(0, getGridY(index),
                        SmartGrid.width * MapGrid.tileSize - 1,
                        SmartGrid.height * MapGrid.tileSize - 1);
                g.setColor(new Color(255, 100, 100, 50));
                g.fillRect(0, getGridY(index),
                        SmartGrid.width * MapGrid.tileSize - 1,
                        SmartGrid.height * MapGrid.tileSize - 1);
            }
        }

    }

    public void updateSize() {
        int numSmartGrids = handler.getSmartGridArray().size();
        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
        this.setSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
        revalidate();
    }

    public void init(MapEditorHandler handler, boolean editable) {
        this.handler = handler;
        this.editable = editable;

        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
    }

    private int getGridY(int index) {
        int y = 0;
        String previous = null;
        for (int i = 0; i <= index && i < handler.getSmartGridArray().size(); i++) {
            String folder = handler.getSmartGrid(i).getPaletteFolder();
            if (previous == null || !previous.equals(folder)) {
                y += FOLDER_HEADER_HEIGHT;
            }
            if (i == index) {
                return y;
            }
            if (!isSmartFolderCollapsed(folder)) {
                y += SmartGrid.height * MapGrid.tileSize;
            }
            previous = folder;
        }
        return y;
    }

    private int getDisplayHeight() {
        if (handler == null) {
            return SmartGrid.height * MapGrid.tileSize;
        }
        int height = 0;
        String previous = null;
        for (SmartGrid grid : handler.getSmartGridArray()) {
            String folder = grid.getPaletteFolder();
            if (previous == null || !previous.equals(folder)) {
                height += FOLDER_HEADER_HEIGHT;
            }
            if (!isSmartFolderCollapsed(folder)) {
                height += SmartGrid.height * MapGrid.tileSize;
            }
            previous = folder;
        }
        return Math.max(1, height);
    }

    private int getGridIndexAt(int y) {
        for (int i = 0; i < handler.getSmartGridArray().size(); i++) {
            if (isSmartFolderCollapsed(handler.getSmartGrid(i).getPaletteFolder())) {
                continue;
            }
            int top = getGridY(i);
            if (y >= top && y < top + SmartGrid.height * MapGrid.tileSize) {
                return i;
            }
        }
        return -1;
    }

    private void paintFolderHeader(Graphics g, int index) {
        String folder = handler.getSmartGrid(index).getPaletteFolder();
        if (index > 0 && folder.equals(handler.getSmartGrid(index - 1).getPaletteFolder())) {
            return;
        }
        int y = getGridY(index) - FOLDER_HEADER_HEIGHT;
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        g.setColor(bg == null ? Color.darkGray : bg.darker());
        g.fillRect(0, y, getWidth(), FOLDER_HEADER_HEIGHT);
        g.setColor(fg == null ? Color.white : fg);
        String text = folder.isEmpty() ? "All Smart" : folder.substring(folder.lastIndexOf('/') + 1);
        if (!folder.isEmpty()) {
            int cx = 5;
            int cy = y + FOLDER_HEADER_HEIGHT / 2;
            if (isSmartFolderCollapsed(folder)) {
                g.fillPolygon(new int[]{cx - 2, cx + 3, cx - 2},
                        new int[]{cy - 4, cy, cy + 4}, 3);
            } else {
                g.fillPolygon(new int[]{cx - 3, cx + 4, cx},
                        new int[]{cy - 2, cy - 2, cy + 3}, 3);
            }
        }
        g.drawString(text, folder.isEmpty() ? 3 : 12, y + 12);
    }

    private boolean isSmartFolderCollapsed(String folderPath) {
        PaletteFolder folder = handler.getTileset().getPaletteFolder(folderPath);
        return folder != null && !folderPath.isEmpty() && folder.areSmartDrawingsCollapsed();
    }

    private int getFolderHeaderIndexAt(int y) {
        for (int i = 0; i < handler.getSmartGridArray().size(); i++) {
            String folder = handler.getSmartGrid(i).getPaletteFolder();
            if (i > 0 && folder.equals(handler.getSmartGrid(i - 1).getPaletteFolder())) {
                continue;
            }
            int top = getGridY(i) - FOLDER_HEADER_HEIGHT;
            if (y >= top && y < top + FOLDER_HEADER_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private void toggleSmartFolder(String folderPath) {
        PaletteFolder folder = handler.getTileset().getPaletteFolder(folderPath);
        if (folder != null) {
            folder.setSmartDrawingsCollapsed(!folder.areSmartDrawingsCollapsed());
            updateSize();
            repaint();
        }
    }

    private void showSmartFolderMenu(int headerIndex, MouseEvent evt) {
        String folderPath = handler.getSmartGrid(headerIndex).getPaletteFolder();
        PaletteFolder folder = handler.getTileset().getPaletteFolder(folderPath);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem collapse = new JMenuItem(folder != null && folder.areSmartDrawingsCollapsed()
                ? "Expand" : "Collapse");
        collapse.addActionListener(e -> toggleSmartFolder(folderPath));
        menu.add(collapse);
        int start = getFolderGroupStart(headerIndex);
        int end = getFolderGroupEnd(headerIndex);
        JMenuItem up = new JMenuItem("Move Up");
        up.setEnabled(start > 0);
        up.addActionListener(e -> moveSmartFolderGroup(headerIndex, -1));
        menu.add(up);
        JMenuItem down = new JMenuItem("Move Down");
        down.setEnabled(end < handler.getSmartGridArray().size());
        down.addActionListener(e -> moveSmartFolderGroup(headerIndex, 1));
        menu.add(down);
        menu.show(this, evt.getX(), evt.getY());
    }

    private int getFolderGroupStart(int index) {
        String folder = handler.getSmartGrid(index).getPaletteFolder();
        while (index > 0 && folder.equals(handler.getSmartGrid(index - 1).getPaletteFolder())) {
            index--;
        }
        return index;
    }

    private int getFolderGroupEnd(int index) {
        String folder = handler.getSmartGrid(index).getPaletteFolder();
        index++;
        while (index < handler.getSmartGridArray().size()
                && folder.equals(handler.getSmartGrid(index).getPaletteFolder())) {
            index++;
        }
        return index;
    }

    private void moveSmartFolderGroup(int index, int direction) {
        ArrayList<SmartGrid> grids = handler.getSmartGridArray();
        SmartGrid selected = handler.getSmartGridIndexSelected() >= 0
                && handler.getSmartGridIndexSelected() < grids.size()
                ? grids.get(handler.getSmartGridIndexSelected()) : null;
        int start = getFolderGroupStart(index);
        int end = getFolderGroupEnd(index);
        int groupSize = end - start;
        if (direction < 0 && start > 0) {
            int previousStart = getFolderGroupStart(start - 1);
            Collections.rotate(grids.subList(previousStart, end), groupSize);
        } else if (direction > 0 && end < grids.size()) {
            int nextEnd = getFolderGroupEnd(end);
            Collections.rotate(grids.subList(start, nextEnd), -groupSize);
        }
        if (selected != null) {
            handler.setSmartGridIndexSelected(grids.indexOf(selected));
        }
        updateSize();
        repaint();
    }

    private JMenu buildFolderMenu(int gridIndex) {
        JMenu menu = new JMenu("Move to Folder");
        JMenuItem none = new JMenuItem("All Smart Drawings");
        none.addActionListener(e -> moveGridToFolder(gridIndex, ""));
        menu.add(none);
        for (PaletteFolder folder : handler.getTileset().getPaletteFolders()) {
            if (folder.getPath().isEmpty()) {
                continue;
            }
            JMenuItem item = new JMenuItem(folder.getPath());
            item.addActionListener(e -> moveGridToFolder(gridIndex, folder.getPath()));
            menu.add(item);
        }
        return menu;
    }

    private void moveGridToFolder(int gridIndex, String folderPath) {
        SmartGrid grid = handler.getSmartGridArray().remove(gridIndex);
        grid.setPaletteFolder(folderPath);
        int insertIndex = handler.getSmartGridArray().size();
        for (int i = 0; i < handler.getSmartGridArray().size(); i++) {
            if (folderPath.equals(handler.getSmartGrid(i).getPaletteFolder())) {
                insertIndex = i + 1;
            }
        }
        handler.getSmartGridArray().add(insertIndex, grid);
        handler.setSmartGridIndexSelected(insertIndex);
        updateSize();
        repaint();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        addMouseMotionListener(new MouseMotionAdapter() {
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
