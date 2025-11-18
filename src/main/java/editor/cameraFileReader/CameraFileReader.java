package editor.cameraFileReader;

import editor.MainFrame;
import editor.game.Game;
import net.miginfocom.swing.MigLayout;
import utils.BinaryReader;
import utils.FileChooserUtils;

import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * @author Trifindo, JackHack96, Kuha
 */
public class CameraFileReader extends JDialog {

    private static long distance;
    private static short verticalRotation;
    private static short horizontalRotation;
    private static short zRotation;
    private static long ukn1;
    private static byte[] ortho;
    private static byte[] ukn2;
    private static long fov;
    private static long nearclip;
    private static long farclip;
    private static long xoffset;
    private static long yoffset;
    private static long zoffset;

    private String[] HGSScolums = new String[]{
            "Type", "Distance", "Vertical Rotation", "Horizontal Rotation", "Z Rotation", "Orthographic", "FOV", "Near Clip", "Far Clip", "X Offset", "Y Offset", "Z Offset"
    };

    private String[] colums = new String[]{
            "Type", "Distance", "Vertical Rotation", "Horizontal Rotation", "Z Rotation", "Orthographic", "FOV", "Near Clip", "Far Clip"
    };

    private String[] selectedColums = colums;

    private String browsePathText = "";
    public CameraFileReader(Window owner) {
        super(owner);
        initComponents();
        DefaultTableModel tableModel = new DefaultTableModel();
        if(MainFrame.handler.getGame().gameSelected == Game.HEART_GOLD || MainFrame.handler.getGame().gameSelected == Game.SOUL_SILVER) {
            selectedColums = HGSScolums;
        }
        for(String columnName : selectedColums){
            tableModel.addColumn(columnName);
        }


        tableModel.addRow(new Object[]{"Initial",0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}); // Adding empty row for camera values
        tableModel.addRow(new Object[]{"Converted",0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}); // Adding empty row for converted camera values
        cameraValues.setModel(tableModel);

    }


    private void readCamerBinFile(String path) {
        System.out.println("Reading camera file: " + path);
        try {
            File file = new File(path);

            if (file.exists()) {
                byte[] data = Files.readAllBytes(Paths.get(path));
                if(data.length == 36 || data.length == 24) {
                    System.out.println("File length is correct");
                    distance = BinaryReader.readUInt32(data, 0);
                    verticalRotation = BinaryReader.readInt16(data, 4);
                    System.out.println("Vertical Rotation: " + verticalRotation);
                    horizontalRotation = BinaryReader.readInt16(data, 6);
                    zRotation = BinaryReader.readInt16(data, 8);
                    ukn1 = BinaryReader.readInt16(data, 10);
                    ortho = BinaryReader.readBytes(data, 12, 1);
                    ukn2 = BinaryReader.readBytes(data, 13, 1);
                    fov = BinaryReader.readInt16(data, 14);
                    nearclip = BinaryReader.readUInt32(data, 16);
                    farclip = BinaryReader.readUInt32(data, 20);
                    if (MainFrame.handler.getGame().gameSelected == Game.HEART_GOLD || MainFrame.handler.getGame().gameSelected == Game.SOUL_SILVER) {
                        xoffset = BinaryReader.readInt32(data, 24);
                        yoffset = BinaryReader.readInt32(data, 28);
                        zoffset = BinaryReader.readInt32(data, 32);
                    }
                }else{
                    JOptionPane.showMessageDialog(this, "File length is incorrect", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error reading the camera file.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public float convertRotationToDegrees(long rotation) {
        //System.out.print("Rotation: ");
        //System.out.println(rotation);
        // rotation is the int16 value read from the file
        return (float) 180*rotation/32768;
    }

    public float convertFov(long fov) {
        return (float) 180*fov/65535;
    }

    public boolean isOrtho(byte value) {
        return value == 1;
    }

    public float convertDistance(long distance) {
        return  (float) distance /Integer.MAX_VALUE*100;
    }

    public void populateTable() {
        cameraValues.setValueAt(distance, 0, 1);
        cameraValues.setValueAt(verticalRotation, 0, 2);
        cameraValues.setValueAt(horizontalRotation, 0, 3);
        cameraValues.setValueAt(zRotation, 0, 4);
        cameraValues.setValueAt(ortho[0], 0, 5);
        cameraValues.setValueAt(fov, 0, 6);
        cameraValues.setValueAt(nearclip, 0, 7);
        cameraValues.setValueAt(farclip, 0, 8);
        if (MainFrame.handler.getGame().gameSelected == Game.HEART_GOLD || MainFrame.handler.getGame().gameSelected == Game.SOUL_SILVER) {
            cameraValues.setValueAt(xoffset, 0, 9);
            cameraValues.setValueAt(yoffset, 0, 10);
            cameraValues.setValueAt(zoffset, 0, 11);
        }

        cameraValues.setValueAt(convertDistance(distance), 1, 1);
        cameraValues.setValueAt(convertRotationToDegrees(verticalRotation)+"deg", 1, 2);
        cameraValues.setValueAt(convertRotationToDegrees(horizontalRotation)+"deg", 1, 3);
        cameraValues.setValueAt(convertRotationToDegrees(zRotation)+"deg", 1, 4);
        cameraValues.setValueAt(isOrtho(ortho[0]), 1, 5);
        cameraValues.setValueAt(convertFov(fov)+"deg", 1, 6);
        cameraValues.setValueAt(nearclip, 1, 7);
        cameraValues.setValueAt(farclip, 1, 8);
        if (MainFrame.handler.getGame().gameSelected == Game.HEART_GOLD || MainFrame.handler.getGame().gameSelected == Game.SOUL_SILVER) {
            cameraValues.setValueAt(xoffset, 1, 9);
            cameraValues.setValueAt(yoffset, 1, 10);
            cameraValues.setValueAt(zoffset, 1, 11);
        }
    }

    private void browseButton(ActionEvent evt) {//GEN-FIRST:event_jbNsbBrowseActionPerformed

        File folder = new File(browsePathText);

        FileChooserUtils.selectFile(
                "Select the camera file",
                folder,
                selectedFile -> {
                    if (selectedFile != null && selectedFile.isFile()) {
                        browsePathText = selectedFile.getPath();
                        browsePath.setText(browsePathText);
                        readCamerBinFile(browsePathText);
                        populateTable();
                    }
                }
        );
    }//GEN-LAST:event_jbNsbBrowseActionPerformed

    private void ok(ActionEvent e) {
        dispose();
    }

    private void cancel(ActionEvent e) {
        dispose();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Educational license - Corentin Macé
        dialogPane = new JPanel();
        buttonBar = new JPanel();
        browsePath = new JTextArea();
        browseButton = new JButton();
        okButton = new JButton();
        cancelButton = new JButton();
        scrollPane1 = new JScrollPane();
        cameraValues = new JTable();

        //======== this ========
        setTitle("Camera File Reader");
        setPreferredSize(new Dimension(1100, 200));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setLayout(new BorderLayout());

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                    "fillx,insets dialog",
                    // columns
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[button,fill]" +
                    "[button,fill]",
                    // rows
                    "[]" +
                    "[]"));
                buttonBar.add(browsePath, "cell 0 1 8 1");

                //---- browseButton ----
                browseButton.setText("Browse..");
                browseButton.addActionListener(e -> browseButton(e));
                buttonBar.add(browseButton, "cell 8 1");

                //---- okButton ----
                okButton.setText("OK");
                okButton.setBackground(UIManager.getColor("Actions.Blue"));
                okButton.addActionListener(e -> ok(e));
                buttonBar.add(okButton, "tag ok,cell 21 1");

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setBackground(UIManager.getColor("Button.disabledBackground"));
                cancelButton.addActionListener(e -> cancel(e));
                buttonBar.add(cancelButton, "tag cancel,cell 22 1");
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);

            //======== scrollPane1 ========
            {

                //---- cameraValues ----
                cameraValues.setBackground(UIManager.getColor("Button.disabledBackground"));
                cameraValues.setEnabled(false);
                scrollPane1.setViewportView(cameraValues);
            }
            dialogPane.add(scrollPane1, BorderLayout.NORTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Educational license - Corentin Macé
    private JPanel dialogPane;
    private JPanel buttonBar;
    private JTextArea browsePath;
    private JButton browseButton;
    private JButton okButton;
    private JButton cancelButton;
    private JScrollPane scrollPane1;
    private JTable cameraValues;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
