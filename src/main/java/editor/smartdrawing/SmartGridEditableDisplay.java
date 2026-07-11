package editor.smartdrawing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.GroupLayout;

import editor.grid.MapGrid;
import editor.handler.MapEditorHandler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import tileset.Tile;
import tileset.PaletteFolder;
import utils.Utils;

/**
 * @author Trifindo, JackHack96
 */
public class SmartGridEditableDisplay extends JPanel {

    private static BufferedImage gridImage = Utils.loadTexImageAsResource("/imgs/smartGrid.png");

    private MapEditorHandler handler;

    private ArrayList<SmartGridEditable> smartGridArray;
    private static final int FOLDER_HEADER_HEIGHT = 16;

    public SmartGridEditableDisplay() {
        initComponents();

        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                SmartGrid.height * MapGrid.tileSize));
    }

    private void formMouseMoved(MouseEvent evt) {
        if (handler.getTileset().size() > 0) {
            float scale = getScale();
            int x = (int)(evt.getX() / (MapGrid.tileSize * scale));
            int displayY = (int) (evt.getY() / scale);
            int gridIndex = getGridIndexAt(displayY);
            int y = gridIndex < 0 ? -1
                    : (displayY - getGridY(gridIndex)) / MapGrid.tileSize;
            System.out.println(x + "  " + y);
            if (gridIndex < smartGridArray.size() && gridIndex >= 0) {
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

    private void formMousePressed(MouseEvent evt) {
        if (handler.getTileset().size() > 0) {
            float scale = getScale();
            int x = (int)(evt.getX() / (MapGrid.tileSize * scale));
            int displayY = (int) (evt.getY() / scale);
            int gridIndex = getGridIndexAt(displayY);
            int y = gridIndex < 0 ? -1
                    : (displayY - getGridY(gridIndex)) / MapGrid.tileSize;
            //System.out.println(x + "  " + y);
            if (gridIndex < smartGridArray.size() && gridIndex >= 0) {
                if (!((y == 2) && (x == 4 || x == 3))) {
                    Tile[][] grid = smartGridArray.get(gridIndex).sgrid;
                    if (new Rectangle(SmartGrid.width, SmartGrid.height).contains(x, y)) {
                        if (SwingUtilities.isLeftMouseButton(evt)) {
                            if (handler.getTileSelected().isSizeOne()) {
                                grid[x][y] = handler.getTileSelected();
                            }
                        }
                        repaint();
                    }
                } else {
                    handler.setSmartGridIndexSelected(gridIndex);
                    repaint();
                }
            }

            if (SwingUtilities.isRightMouseButton(evt) && handler != null) {
                if (gridIndex >= 0 && gridIndex < smartGridArray.size()) {
                    handler.setSmartGridIndexSelected(gridIndex);
                }

                JPopupMenu menu = new JPopupMenu();
                JMenuItem item1 = new JMenuItem("Add Smart Painter");
                JMenuItem item2 = new JMenuItem("Remove Smart Painter");
                item1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        smartGridArray.add(new SmartGridEditable());
                        updateSize();
                        repaint();
                    }
                });
                item2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (smartGridArray.size() > 1) {
                            if (gridIndex >= 0 && gridIndex < smartGridArray.size()) {
                                smartGridArray.remove(gridIndex);
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
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        float scale = getScale();
        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(scale, scale);

        if (gridImage != null && handler != null) {
            for (int k = 0; k < smartGridArray.size(); k++) {
                paintFolderHeader(g, k);
                g.drawImage(gridImage, 0,
                        getGridY(k), null);
            }

        }

        if (handler != null) {
            for (int k = 0; k < smartGridArray.size(); k++) {
                SmartGridEditable sg = smartGridArray.get(k);
                Tile[][] grid = sg.sgrid;
                for (int i = 0; i < SmartGrid.width; i++) {
                    for (int j = 0; j < SmartGrid.height; j++) {
                        Tile tile = grid[i][j];
                        if (tile != null) {
                            try {
                                BufferedImage img = tile.getThumbnail();
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
            g.setColor(Color.red);
            g.drawRect(
                    0,
                    getGridY(index),
                    SmartGrid.width * MapGrid.tileSize - 1,
                    SmartGrid.height * MapGrid.tileSize - 1);
            g.setColor(new Color(255, 100, 100, 50));
            g.fillRect(0,
                    getGridY(index),
                    SmartGrid.width * MapGrid.tileSize - 1,
                    SmartGrid.height * MapGrid.tileSize - 1);
        }

    }

    public void updateSize() {
        //int numSmartGrids = handler.getSmartGridArray().size();
        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
        this.setSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
        revalidate();
    }

    public void updateTiles() {
        for (SmartGridEditable sgrid : smartGridArray) {
            Tile[][] data = sgrid.sgrid;
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    if (!handler.getTileset().getTiles().contains(data[i][j])) {
                        data[i][j] = null;
                    }
                }
            }
        }
    }

    public void init(MapEditorHandler handler) {
        this.handler = handler;

        smartGridArray = new ArrayList<>(handler.getSmartGridArray().size());
        for (SmartGrid sgrid : handler.getSmartGridArray()) {
            SmartGridEditable editable = new SmartGridEditable(sgrid.sgrid, handler.getTileset());
            editable.setPaletteFolder(sgrid.getPaletteFolder());
            smartGridArray.add(editable);
        }

        this.setPreferredSize(new Dimension(
                SmartGrid.width * MapGrid.tileSize,
                getDisplayHeight()));
    }

    public ArrayList<SmartGridEditable> getSmartGridArray() {
        return smartGridArray;
    }

    public void moveSelectedSmartGridUp() {
        int index = handler.getSmartGridIndexSelected();
        if (index > 0) {
            Collections.swap(smartGridArray, index, index - 1);
            handler.setSmartGridIndexSelected(index - 1);
        }
    }

    public void moveSelectedSmartGridDown() {
        int index = handler.getSmartGridIndexSelected();
        if (index < smartGridArray.size() - 1) {
            Collections.swap(smartGridArray, index, index + 1);
            handler.setSmartGridIndexSelected(index + 1);
        }
    }

    private float getScale(){
        return getWidth() / (float)(SmartGrid.width * MapGrid.tileSize);
    }

    private int getGridY(int index) {
        int y = 0;
        String previous = null;
        for (int i = 0; i <= index && i < smartGridArray.size(); i++) {
            String folder = smartGridArray.get(i).getPaletteFolder();
            if (previous == null || !previous.equals(folder)) {
                y += FOLDER_HEADER_HEIGHT;
            }
            if (i == index) {
                return y;
            }
            y += SmartGrid.height * MapGrid.tileSize;
            previous = folder;
        }
        return y;
    }

    private int getDisplayHeight() {
        if (smartGridArray == null) {
            return SmartGrid.height * MapGrid.tileSize;
        }
        int height = 0;
        String previous = null;
        for (SmartGridEditable grid : smartGridArray) {
            String folder = grid.getPaletteFolder();
            if (previous == null || !previous.equals(folder)) {
                height += FOLDER_HEADER_HEIGHT;
            }
            height += SmartGrid.height * MapGrid.tileSize;
            previous = folder;
        }
        return Math.max(1, height);
    }

    private int getGridIndexAt(int y) {
        for (int i = 0; i < smartGridArray.size(); i++) {
            int top = getGridY(i);
            if (y >= top && y < top + SmartGrid.height * MapGrid.tileSize) {
                return i;
            }
        }
        return -1;
    }

    private void paintFolderHeader(Graphics g, int index) {
        String folder = smartGridArray.get(index).getPaletteFolder();
        if (index > 0 && folder.equals(smartGridArray.get(index - 1).getPaletteFolder())) {
            return;
        }
        int y = getGridY(index) - FOLDER_HEADER_HEIGHT;
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        g.setColor(bg == null ? Color.darkGray : bg.darker());
        g.fillRect(0, y, SmartGrid.width * MapGrid.tileSize, FOLDER_HEADER_HEIGHT);
        g.setColor(fg == null ? Color.white : fg);
        String text = folder.isEmpty() ? "All Smart" : folder.substring(folder.lastIndexOf('/') + 1);
        g.drawString(text, 3, y + 12);
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
        SmartGridEditable grid = smartGridArray.remove(gridIndex);
        grid.setPaletteFolder(folderPath);
        int insertIndex = smartGridArray.size();
        for (int i = 0; i < smartGridArray.size(); i++) {
            if (folderPath.equals(smartGridArray.get(i).getPaletteFolder())) {
                insertIndex = i + 1;
            }
        }
        smartGridArray.add(insertIndex, grid);
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
