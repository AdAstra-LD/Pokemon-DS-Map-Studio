package editor.settings;

import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.multi.MultiLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.plaf.synth.SynthLookAndFeel;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.sun.java.swing.plaf.gtk.GTKLookAndFeel;
import com.sun.java.swing.plaf.motif.MotifLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;


import editor.MainFrame;
import net.miginfocom.swing.*;

import static editor.MainFrame.prefs;

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

        String theme = MainFrame.prefs.get("Theme", "Native");
        try {
            switch (theme) {
                case "Native":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "FlatLaf":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                case "FlatLaf Intellij":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                case "FlatLaf Darcula":
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                case "FlatMac Light":
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                    break;
                case "FlatMac Dark":
                    UIManager.setLookAndFeel(new FlatMacDarkLaf());
                    break;
                case "IntelliJ":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                case "Motif":
                    UIManager.setLookAndFeel(new MotifLookAndFeel());
                    break;
                case "Windows Classic":
                    UIManager.setLookAndFeel(new WindowsClassicLookAndFeel());
                    break;
                case "Windows":
                    UIManager.setLookAndFeel(new WindowsLookAndFeel());
                    break;
                case "Nimbus":
                    UIManager.setLookAndFeel(new NimbusLookAndFeel());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(new MetalLookAndFeel());
                    break;
                case "Multi":
                    UIManager.setLookAndFeel(new MultiLookAndFeel());
                    break;
                case "Arc":
                    UIManager.setLookAndFeel(new FlatArcIJTheme());
                    break;
                case "Arc - Orange":
                    UIManager.setLookAndFeel(new FlatArcOrangeIJTheme());
                    break;
                case "Arc Dark":
                    UIManager.setLookAndFeel(new FlatArcDarkIJTheme());
                    break;
                case "Arc Dark - Orange":
                    UIManager.setLookAndFeel(new FlatArcDarkOrangeIJTheme());
                    break;
                case "Carbon":
                    UIManager.setLookAndFeel(new FlatCarbonIJTheme());
                    break;
                case "Cobalt 2":
                    UIManager.setLookAndFeel(new FlatCobalt2IJTheme());
                    break;
                case "Cyan light":
                    UIManager.setLookAndFeel(new FlatCyanLightIJTheme());
                    break;
                case "Dark Flat":
                    UIManager.setLookAndFeel(new FlatDarkFlatIJTheme());
                    break;
                case "Dark purple":
                    UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
                    break;
                case "Dracula":
                    UIManager.setLookAndFeel(new FlatDraculaIJTheme());
                    break;
                case "Gradianto Dark Fuchsia":
                    UIManager.setLookAndFeel(new FlatGradiantoDarkFuchsiaIJTheme());
                    break;
                case "Gradianto Deep Ocean":
                    UIManager.setLookAndFeel(new FlatGradiantoDeepOceanIJTheme());
                    break;
                case "Gradianto Midnight Blue":
                    UIManager.setLookAndFeel(new FlatGradiantoMidnightBlueIJTheme());
                    break;
                case "Gradianto Nature Green":
                    UIManager.setLookAndFeel(new FlatGradiantoNatureGreenIJTheme());
                    break;
                case "Gray":
                    UIManager.setLookAndFeel(new FlatGrayIJTheme());
                    break;
                case "Gruvbox Dark Hard":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkHardIJTheme());
                    break;
                case "Gruvbox Dark Medium":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkMediumIJTheme());
                    break;
                case "Gruvbox Dark Soft":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkSoftIJTheme());
                    break;
                case "Hiberbee Dark":
                    UIManager.setLookAndFeel(new FlatHiberbeeDarkIJTheme());
                    break;
                case "High contrast":
                    UIManager.setLookAndFeel(new FlatHighContrastIJTheme());
                    break;
                case "Light Flat":
                    UIManager.setLookAndFeel(new FlatLightFlatIJTheme());
                    break;
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }

        //JOptionPane.showMessageDialog(this, "Please restart PDSMS!");
        dispose();
    }

    private void jcmbTheme(ActionEvent e) {
        String theme = Objects.requireNonNull(jcmbTheme.getSelectedItem()).toString();
        try {
            switch (theme) {
                case "Native":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "FlatLaf":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                case "FlatLaf Intellij":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                case "FlatLaf Darcula":
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                case "FlatMac Light":
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                    break;
                case "FlatMac Dark":
                    UIManager.setLookAndFeel(new FlatMacDarkLaf());
                    break;
                case "IntelliJ":
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                case "Motif":
                    UIManager.setLookAndFeel(new MotifLookAndFeel());
                    break;
                case "Windows Classic":
                    UIManager.setLookAndFeel(new WindowsClassicLookAndFeel());
                    break;
                case "Windows":
                    UIManager.setLookAndFeel(new WindowsLookAndFeel());
                    break;
                case "Nimbus":
                    UIManager.setLookAndFeel(new NimbusLookAndFeel());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(new MetalLookAndFeel());
                    break;
                case "Multi":
                    UIManager.setLookAndFeel(new MultiLookAndFeel());
                    break;
                case "Arc":
                    UIManager.setLookAndFeel(new FlatArcIJTheme());
                    break;
                case "Arc - Orange":
                    UIManager.setLookAndFeel(new FlatArcOrangeIJTheme());
                    break;
                case "Arc Dark":
                    UIManager.setLookAndFeel(new FlatArcDarkIJTheme());
                    break;
                case "Arc Dark - Orange":
                    UIManager.setLookAndFeel(new FlatArcDarkOrangeIJTheme());
                    break;
                case "Carbon":
                    UIManager.setLookAndFeel(new FlatCarbonIJTheme());
                    break;
                case "Cobalt 2":
                    UIManager.setLookAndFeel(new FlatCobalt2IJTheme());
                    break;
                case "Cyan light":
                    UIManager.setLookAndFeel(new FlatCyanLightIJTheme());
                    break;
                case "Dark Flat":
                    UIManager.setLookAndFeel(new FlatDarkFlatIJTheme());
                    break;
                case "Dark purple":
                    UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
                    break;
                case "Dracula":
                    UIManager.setLookAndFeel(new FlatDraculaIJTheme());
                    break;
                case "Gradianto Dark Fuchsia":
                    UIManager.setLookAndFeel(new FlatGradiantoDarkFuchsiaIJTheme());
                    break;
                case "Gradianto Deep Ocean":
                    UIManager.setLookAndFeel(new FlatGradiantoDeepOceanIJTheme());
                    break;
                case "Gradianto Midnight Blue":
                    UIManager.setLookAndFeel(new FlatGradiantoMidnightBlueIJTheme());
                    break;
                case "Gradianto Nature Green":
                    UIManager.setLookAndFeel(new FlatGradiantoNatureGreenIJTheme());
                    break;
                case "Gray":
                    UIManager.setLookAndFeel(new FlatGrayIJTheme());
                    break;
                case "Gruvbox Dark Hard":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkHardIJTheme());
                    break;
                case "Gruvbox Dark Medium":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkMediumIJTheme());
                    break;
                case "Gruvbox Dark Soft":
                    UIManager.setLookAndFeel(new FlatGruvboxDarkSoftIJTheme());
                    break;
                case "Hiberbee Dark":
                    UIManager.setLookAndFeel(new FlatHiberbeeDarkIJTheme());
                    break;
                case "High contrast":
                    UIManager.setLookAndFeel(new FlatHighContrastIJTheme());
                    break;
                case "Light Flat":
                    UIManager.setLookAndFeel(new FlatLightFlatIJTheme());
                    break;
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Educational license - Corentin Macé
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
                    "FlatLaf Intellij",
                    "FlatLaf Darcula",
                    "FlatMac Light",
                    "FlatMac Dark",
                    "IntelliJ",
                    "Motif",
                    "Windows Classic",
                    "Windows",
                    "Metal",
                    "Arc",
                    "Arc - Orange",
                    "Arc Dark",
                    "Arc Dark - Orange",
                    "Carbon",
                    "Cobalt 2",
                    "Cyan light",
                    "Dark Flat",
                    "Dark purple",
                    "Dracula",
                    "Gradianto Dark Fuchsia",
                    "Gradianto Deep Ocean",
                    "Gradianto Midnight Blue",
                    "Gradianto Nature Green",
                    "Gray",
                    "Gruvbox Dark Hard",
                    "Gruvbox Dark Medium",
                    "Gruvbox Dark Soft",
                    "Hiberbee Dark",
                    "High contrast",
                    "Light Flat"
                }));
                jcmbTheme.addActionListener(e -> jcmbTheme(e));
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
        setSize(300, 150);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Educational license - Corentin Macé
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
