package editor;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

final class RecentMapsMenu {
    private final MainFrame frame;
    private final Preferences prefs;
    private final JMenu openRecentMapMenu;
    private final JMenuItem clearHistoryMenuItem;

    RecentMapsMenu(MainFrameContext context) {
        this.frame = context.frame;
        this.prefs = context.prefs;
        this.openRecentMapMenu = context.openRecentMapMenu;
        this.clearHistoryMenuItem = context.clearHistoryMenuItem;
    }

    void addAndPersist(String path) {
        RecentMapsStore.add(path);
        RecentMapsStore.save(prefs);
        updateMenu();
    }

    void updateMenu() {
        openRecentMapMenu.removeAll();
        for (String item : RecentMapsStore.items()) {
            int index = RecentMapsStore.items().indexOf(item);
            JMenuItem menuItem = new JMenuItem();
            menuItem.setText(item);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + index, InputEvent.CTRL_MASK));

            menuItem.addActionListener(e -> {
                if (frame.isMapOpened()) {
                    final int returnVal = JOptionPane.showConfirmDialog(frame,
                            "Do you want to close current map?", "Open recent", JOptionPane.YES_NO_OPTION);
                    if (returnVal == JOptionPane.YES_OPTION) {
                        frame.openMap(item);
                    }
                } else {
                    frame.openMap(item);
                }
            });
            openRecentMapMenu.add(menuItem);
        }
        openRecentMapMenu.addSeparator();
        openRecentMapMenu.add(clearHistoryMenuItem);
    }

    void clear() {
        openRecentMapMenu.removeAll();
        RecentMapsStore.clear(prefs);
        updateMenu();
    }
}
