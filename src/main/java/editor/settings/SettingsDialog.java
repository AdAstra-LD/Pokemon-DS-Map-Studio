package editor.settings;

import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import javax.swing.*;

import editor.MainFrame;
import net.miginfocom.swing.*;

/**
 * @author Trifindo, JackHack96
 */
public class SettingsDialog extends JDialog {

    public static boolean HideMap = false;
    public SettingsDialog(Window owner) {
        super(owner);
        initComponents();
        jcmbTheme.setSelectedItem(MainFrame.prefs.get("Theme", "Native"));
        jcbHideMap.setSelected(MainFrame.prefs.getBoolean("HideMap", false));
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        dispose();
    }

    private void okButtonActionPerformed(ActionEvent e) {
        MainFrame.prefs.put("Theme", Objects.requireNonNull(jcmbTheme.getSelectedItem()).toString());
        MainFrame.prefs.putBoolean("HideMap", jcbHideMap.isSelected());

        JOptionPane.showMessageDialog(this, "Please restart PDSMS!");
        dispose();
    }

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
        jcbHideMap = new JCheckBox();


        //======== this ========
        setTitle("Settings");
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

                //---- label1 ----
                label1.setText("Theme:");
                contentPanel.add(label1, "cell 0 0");

                //---- jcmbTheme ----
                jcmbTheme.setModel(new DefaultComboBoxModel<>(new String[] {
                    "Native",
                    "FlatLaf",
                    "FlatLaf Dark"
                }));
                contentPanel.add(jcmbTheme, "cell 1 0");

                //---- label1 ----
                label2.setText("Hide map on Discord:");
                contentPanel.add(label2, "cell 1 1");

                //---- hideMap ----
                jcbHideMap.setSelected(HideMap);
                contentPanel.add(jcbHideMap, "cell 2 1");
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
        setSize(250, 150);
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

    private JCheckBox jcbHideMap;

    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
