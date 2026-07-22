package editor.converter;

import editor.mapgroups.AreaExportDialog;

import java.awt.Frame;

/**
 * @author Trifindo
 */
public class ExportNsbtxDialog extends AreaExportDialog {

    public ExportNsbtxDialog(Frame parent, boolean modal) {
        super(parent, modal);
    }

    @Override
    protected String getFormatName() {
        return "NSBTX";
    }

    @Override
    protected String getDialogTitle() {
        return "Export Areas as NSBTX (Experimental)";
    }

    public String getNsbtxFolderPath() {
        return getFolderPath();
    }
}
