package formats.imd;

import editor.converter.FileConversionDialog;

import java.awt.Window;
import java.util.ArrayList;

/**
 * @author Trifindo, JackHack96
 */
public class ExportImdDialog extends FileConversionDialog {

    public ExportImdDialog(Window owner) {
        super(owner);
    }

    @Override
    protected String getSourceFormatName() {
        return "OBJ";
    }

    @Override
    protected String getDestinationFormatName() {
        return "IMD";
    }

    public String getObjFolderPath() {
        return getSourceFolderPath();
    }

    public String getImdFolderPath() {
        return getDestinationFolderPath();
    }

    public ArrayList<String> getSelectedObjNames() {
        return getSelectedFileNames();
    }
}
