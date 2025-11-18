package editor.exportpath;

import editor.MainFrame;
import editor.handler.MapEditorHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;
import javafx.stage.DirectoryChooser;
import javafx.application.Platform;
import utils.FileChooserUtils;

import static editor.mapmatrix.MapMatrix.ExportPath;


/**
 * @author Trifindo, JackHack96, Kuha
 */
public class ExportPathDialog extends JDialog {

    MapEditorHandler handler;

    private String browsePathText = "";
    public ExportPathDialog(Window owner) {
        super(owner);
        initComponents();
        jcmbTheme.setSelectedItem(MainFrame.prefs.get("Theme", "Native"));
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        dispose();
    }

    private void okButtonActionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "Do not forget to save your map in order to save this path!");
        dispose();
    }

    private void BrowseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jbNsbBrowseActionPerformed
        File folder = new File(ExportPath);

        FileChooserUtils.selectDirectory(
                "Select the default export folder that you want",
                folder,
                selectedDirectory -> {
                    if (selectedDirectory != null && selectedDirectory.isDirectory()) {
                        browsePathText = selectedDirectory.getPath();
                        browsePath.setText(browsePathText);
                        ExportPath = selectedDirectory.getPath();
                    }
                }
        );
    }//GEN-LAST:event_jbNsbBrowseActionPerformed
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        label2 = new JLabel();
        jcmbTheme = new JComboBox<>();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        browsePath = new JTextField();
        browsePathButton = new JButton();

        //======== this ========
        setTitle("Export Default Path Selection");
        setModal(true);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setLayout(new MigLayout(
                    "insets 0,hidemode 3,gap 0 0",
                    // columns
                    "[grow,fill]",
                    // rows
                    "[grow,fill]" +
                            "[fill]"));

            //======== contentPanel ========
            {
                contentPanel.setLayout(new MigLayout(
                        "insets dialog,hidemode 3",
                        // columns
                        "[fill]" +
                                "[grow,fill]",
                        // rows
                        "[]"));

                //======== export path =======

                label2.setText("Export Path:");
                browsePath.setText(ExportPath);
                contentPanel.add(label2, "cell 0 1");
                browsePathButton.setText("Browse...");
                browsePathButton.addActionListener(e -> BrowseActionPerformed(e));
                contentPanel.add(browsePath, "cell 1 1");
                contentPanel.add(browsePathButton, "cell 2 1");
            }
            dialogPane.add(contentPanel, "cell 0 0");

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                        "insets dialog,alignx right",
                        // columns
                        "[button,fill]" +
                                "[button,fill]",
                        // rows
                        "[fill]"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> okButtonActionPerformed(e));
                buttonBar.add(okButton, "cell 0 0");

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));
                buttonBar.add(cancelButton, "cell 1 0");
            }
            dialogPane.add(buttonBar, "cell 0 1");
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        setSize(350, 160);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JLabel label2;
    private JComboBox<String> jcmbTheme;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField browsePath;
    private JButton browsePathButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
