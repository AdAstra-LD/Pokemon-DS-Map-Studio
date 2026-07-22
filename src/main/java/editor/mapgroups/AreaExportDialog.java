package editor.mapgroups;

import editor.handler.MapEditorHandler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import utils.Utils;
import utils.swing.FolderPickerPanel;

/**
 * Base dialog for exports that pick a set of areas with the visual
 * {@link AreaSelectionPanel} and a destination folder. Subclasses only name
 * the exported format; the hooks are called during construction, so they must
 * return constants.
 *
 * @author AdAstra
 */
public abstract class AreaExportDialog extends JDialog {

    public static final int APPROVE_OPTION = 1, CANCEL_OPTION = 0;
    private int returnValue = CANCEL_OPTION;

    private ArrayList<Integer> selectedAreaIndices = new ArrayList<>();

    protected MapEditorHandler handler;

    private final AreaSelectionPanel areaSelectionPanel = new AreaSelectionPanel();
    private final FolderPickerPanel folderPicker = new FolderPickerPanel(
            getFormatName() + " destination folder path",
            "Select the folder for exporting the " + getFormatName() + " files");
    private final JButton acceptButton = new JButton("OK");

    protected AreaExportDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        getRootPane().setDefaultButton(acceptButton);
        acceptButton.requestFocus();
    }

    /** Name of the exported format, e.g. "NSBTX" or "PDSMAP". */
    protected abstract String getFormatName();

    protected String getDialogTitle() {
        return "Export Areas as " + getFormatName();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(getDialogTitle());
        setModal(true);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel selectionPanel = new JPanel(new BorderLayout(0, 6));
        selectionPanel.add(new JLabel("Select the areas that will be exported as "
                + getFormatName() + ":"), BorderLayout.NORTH);
        selectionPanel.add(areaSelectionPanel, BorderLayout.CENTER);
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
        bottomPanel.add(folderPicker);
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(buttonPanel);
        add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(940, 620));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void accept() {
        if (!folderPicker.isFolderValid()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid output folder for exporting the "
                            + getFormatName() + " files.",
                    "Invalid " + getFormatName() + " folder",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedAreaIndices = areaSelectionPanel.getSelectedAreaIndices();
        if (selectedAreaIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "There are no Areas selected for exporting as " + getFormatName() + ".\n"
                            + "Select at least one Area from the list.", "No Areas selected",
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

    public void init(MapEditorHandler handler) {
        this.handler = handler;

        areaSelectionPanel.init(handler);

        File defaultFolder = new File(
                Utils.removeExtensionFromPath(handler.getMapMatrix().filePath)).getParentFile();
        folderPicker.setDefaultDirectory(defaultFolder);
        if (defaultFolder != null && defaultFolder.isDirectory()) {
            folderPicker.setFolderPath(defaultFolder.getPath());
        }
    }

    public int getReturnValue() {
        return returnValue;
    }

    public String getFolderPath() {
        return folderPicker.getFolderPath();
    }

    public ArrayList<Integer> getSelectedAreaIndices() {
        return selectedAreaIndices;
    }
}
