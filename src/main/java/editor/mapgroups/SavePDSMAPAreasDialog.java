package editor.mapgroups;

import java.awt.Frame;

/**
 * @author Trifindo
 */
public class SavePDSMAPAreasDialog extends AreaExportDialog {

    public SavePDSMAPAreasDialog(Frame parent, boolean modal) {
        super(parent, modal);
    }

    @Override
    protected String getFormatName() {
        return "PDSMAP";
    }

    public String getAreaFolderPath() {
        return getFolderPath();
    }
}
