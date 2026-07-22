package editor.converter;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.Frame;
import java.util.ArrayList;

/**
 * @author Trifindo
 */
public class ExportNsbmdDialog extends FileConversionDialog {

    private JCheckBox includeNsbtxCheckBox;

    public ExportNsbmdDialog(Frame parent, boolean modal) {
        super(parent);
    }

    @Override
    protected String getSourceFormatName() {
        return "IMD";
    }

    @Override
    protected String getDestinationFormatName() {
        return "NSBMD";
    }

    @Override
    protected boolean isFileInitiallySelected(String fileName) {
        return hasMatrixCoordsInName(fileName);
    }

    //Called during base construction, before field initializers run
    @Override
    protected JComponent getExtraOptionsComponent() {
        if (includeNsbtxCheckBox == null) {
            includeNsbtxCheckBox = new JCheckBox("Include NSBTX in NSBMD");
            includeNsbtxCheckBox.setSelected(false);
        }
        return includeNsbtxCheckBox;
    }

    public String getImdFolderPath() {
        return getSourceFolderPath();
    }

    public String getNsbFolderPath() {
        return getDestinationFolderPath();
    }

    public ArrayList<String> getSelectedImdNames() {
        return getSelectedFileNames();
    }

    public boolean includeNsbtxInNsbmd() {
        return includeNsbtxCheckBox != null && includeNsbtxCheckBox.isSelected();
    }
}
