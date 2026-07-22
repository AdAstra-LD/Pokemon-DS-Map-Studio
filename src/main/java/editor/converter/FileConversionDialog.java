package editor.converter;

import editor.handler.MapEditorHandler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import utils.Utils;
import utils.swing.FolderPickerPanel;
import utils.swing.JScrollCheckboxList;

/**
 * Base dialog for conversions that pick files of one format from a source
 * folder and a destination folder for the converted files. Subclasses only
 * name the two formats; the hooks are called during construction, so they
 * must return constants.
 *
 * @author AdAstra
 */
public abstract class FileConversionDialog extends JDialog {

    public static final int APPROVE_OPTION = 1, CANCEL_OPTION = 0;
    private int returnValue = CANCEL_OPTION;

    private ArrayList<String> selectedFileNames = new ArrayList<>();

    protected MapEditorHandler handler;

    private final JScrollCheckboxList fileScrollCheckboxList = new JScrollCheckboxList();
    private final FolderPickerPanel sourceFolderPicker = new FolderPickerPanel(
            getSourceFormatName() + " folder path",
            "Select the folder that contains the " + getSourceFormatName() + " files");
    private final FolderPickerPanel destinationFolderPicker = new FolderPickerPanel(
            getDestinationFormatName() + " destination folder path",
            "Select the folder for exporting the " + getDestinationFormatName() + " files");
    private final JButton acceptButton = new JButton("OK");

    protected FileConversionDialog(Window owner) {
        super(owner);
        initComponents();

        getRootPane().setDefaultButton(acceptButton);
        acceptButton.requestFocus();
    }

    /** Name of the source format, e.g. "OBJ" or "IMD". */
    protected abstract String getSourceFormatName();

    /** Name of the destination format, e.g. "IMD" or "NSBMD". */
    protected abstract String getDestinationFormatName();

    /** Whether a listed file starts checked; defaults to all checked. */
    protected boolean isFileInitiallySelected(String fileName) {
        return true;
    }

    /** Extra settings component shown above the buttons; null for none. */
    protected JComponent getExtraOptionsComponent() {
        return null;
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Export maps as " + getDestinationFormatName() + " settings");
        setModal(true);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        sourceFolderPicker.setFolderSelectedListener(this::loadFilesFromFolder);
        add(sourceFolderPicker, BorderLayout.NORTH);

        fileScrollCheckboxList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fileScrollCheckboxList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        fileScrollCheckboxList.setPreferredSize(new Dimension(440, 180));

        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> setAllSelected(true));
        JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> setAllSelected(false));
        JPanel selectButtonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selectButtonsRow.add(selectAllButton);
        selectButtonsRow.add(deselectAllButton);

        JPanel selectionPanel = new JPanel(new BorderLayout(0, 6));
        selectionPanel.setBorder(BorderFactory.createTitledBorder(
                getSourceFormatName() + " settings"));
        selectionPanel.add(new JLabel("Select the " + getSourceFormatName()
                + " files that will be converted into " + getDestinationFormatName() + ":"),
                BorderLayout.NORTH);
        selectionPanel.add(fileScrollCheckboxList, BorderLayout.CENTER);
        selectionPanel.add(selectButtonsRow, BorderLayout.SOUTH);
        add(selectionPanel, BorderLayout.CENTER);

        acceptButton.setPreferredSize(new Dimension(75, acceptButton.getPreferredSize().height));
        acceptButton.addActionListener(e -> accept());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancel());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(acceptButton);
        buttonPanel.add(cancelButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(destinationFolderPicker);
        JComponent extraOptions = getExtraOptionsComponent();
        if (extraOptions != null) {
            JPanel extraOptionsRow = new JPanel(new BorderLayout());
            extraOptionsRow.add(extraOptions, BorderLayout.CENTER);
            bottomPanel.add(extraOptionsRow);
        }
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(buttonPanel);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void accept() {
        if (!sourceFolderPicker.isFolderValid()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid folder that contains the "
                            + getSourceFormatName() + " files.",
                    "Invalid " + getSourceFormatName() + " folder",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!destinationFolderPicker.isFolderValid()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid output folder for exporting the "
                            + getDestinationFormatName() + " files.",
                    "Invalid " + getDestinationFormatName() + " folder",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        DefaultListModel<JCheckBox> model =
                (DefaultListModel<JCheckBox>) fileScrollCheckboxList.getCheckboxList().getModel();
        selectedFileNames = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            if (model.get(i).isSelected()) {
                selectedFileNames.add(model.get(i).getText());
            }
        }
        if (selectedFileNames.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no " + getSourceFormatName() + " selected for converting into "
                            + getDestinationFormatName() + ".\n"
                            + "Select at least one " + getSourceFormatName() + " model from the list.",
                    "No " + getSourceFormatName() + " models selected",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        returnValue = APPROVE_OPTION;
        dispose();
    }

    private void cancel() {
        returnValue = CANCEL_OPTION;
        dispose();
    }

    private void setAllSelected(boolean selected) {
        DefaultListModel<JCheckBox> model =
                (DefaultListModel<JCheckBox>) fileScrollCheckboxList.getCheckboxList().getModel();
        for (int i = 0; i < model.getSize(); i++) {
            model.get(i).setSelected(selected);
        }
        fileScrollCheckboxList.repaint();
    }

    private void loadFilesFromFolder(String folderPath) {
        String extension = "." + getSourceFormatName().toLowerCase();
        File[] files = new File(folderPath).listFiles((dir, name) -> name.endsWith(extension));
        if (files == null) {
            return;
        }
        DefaultListModel<JCheckBox> model = new DefaultListModel<>();
        for (File file : files) {
            JCheckBox checkBox = new JCheckBox(file.getName());
            checkBox.setSelected(isFileInitiallySelected(file.getName()));
            model.addElement(checkBox);
        }
        fileScrollCheckboxList.getCheckboxList().setModel(model);
        sourceFolderPicker.setFolderPath(folderPath);
    }

    public void init(MapEditorHandler handler) {
        this.handler = handler;

        File defaultFolder = new File(
                Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)).getParentFile();
        sourceFolderPicker.setDefaultDirectory(defaultFolder);
        destinationFolderPicker.setDefaultDirectory(defaultFolder);
        if (defaultFolder != null && defaultFolder.isDirectory()) {
            loadFilesFromFolder(defaultFolder.getPath());
            destinationFolderPicker.setFolderPath(defaultFolder.getPath());
        }
    }

    protected static boolean hasMatrixCoordsInName(String fileName) {
        String name = Utils.removeExtensionFromPath(fileName);
        try {
            String[] splitName = name.split("_");
            return isInteger(splitName[splitName.length - 2])
                    && isInteger(splitName[splitName.length - 1]);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isInteger(String name) {
        try {
            Integer.parseInt(name);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public int getReturnValue() {
        return returnValue;
    }

    public String getSourceFolderPath() {
        return sourceFolderPicker.getFolderPath();
    }

    public String getDestinationFolderPath() {
        return destinationFolderPicker.getFolderPath();
    }

    public ArrayList<String> getSelectedFileNames() {
        return selectedFileNames;
    }
}
