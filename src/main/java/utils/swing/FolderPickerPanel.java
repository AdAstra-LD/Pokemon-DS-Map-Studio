package utils.swing;

import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.io.File;
import java.util.function.Consumer;

/**
 * Titled folder-path row (read-only text field plus a Browse button) shared by
 * the export dialogs.
 *
 * @author AdAstra
 */
public class FolderPickerPanel extends JPanel {

    private final JTextField pathField = new JTextField();
    private final String chooserTitle;
    private String folderPath = "";
    private File defaultDirectory;
    private Consumer<String> folderSelectedListener;

    public FolderPickerPanel(String borderTitle, String chooserTitle) {
        super(new BorderLayout(6, 0));
        this.chooserTitle = chooserTitle;
        setBorder(BorderFactory.createTitledBorder(borderTitle));

        pathField.setEditable(false);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browse());
        add(pathField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);
    }

    private void browse() {
        final SystemFileChooser fc = new SystemFileChooser();
        if (defaultDirectory != null) {
            fc.setCurrentDirectory(defaultDirectory);
        }
        fc.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        fc.setApproveButtonText("Select folder");
        fc.setDialogTitle(chooserTitle);

        final int returnVal = fc.showOpenDialog(this);
        if (returnVal == SystemFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists() && file.isDirectory()) {
                if (folderSelectedListener != null) {
                    folderSelectedListener.accept(file.getPath());
                } else {
                    setFolderPath(file.getPath());
                }
            }
        }
    }

    /**
     * Replaces the default browse behavior; the listener is responsible for
     * calling {@link #setFolderPath(String)} once the folder is accepted.
     */
    public void setFolderSelectedListener(Consumer<String> listener) {
        this.folderSelectedListener = listener;
    }

    public void setDefaultDirectory(File directory) {
        this.defaultDirectory = directory;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
        pathField.setText(folderPath);
    }

    public String getFolderPath() {
        return folderPath;
    }

    public boolean isFolderValid() {
        try {
            return new File(folderPath).isDirectory();
        } catch (Exception ex) {
            return false;
        }
    }
}
