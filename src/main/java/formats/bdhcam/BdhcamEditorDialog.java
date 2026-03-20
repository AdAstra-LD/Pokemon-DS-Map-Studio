/*
 * Created by JFormDesigner on Mon Nov 02 17:36:12 CET 2020
 */

package formats.bdhcam;

import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.event.*;

import editor.MainFrame;
import editor.cameraFileReader.CameraFileReader;
import editor.game.Game;
import formats.bdhcam.camplate.*;
import editor.handler.MapEditorHandler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.miginfocom.swing.*;
import utils.BinaryReader;
import utils.FileChooserUtils;

import static editor.mapmatrix.MapMatrix.ExportPath;

import static editor.mapmatrix.MapMatrix.ExportPath;

/**
 * @author Trifindo
 */
public class BdhcamEditorDialog extends JDialog {

    private MapEditorHandler handler;

    private String browsePathText = "";
    private BdhcamHandler bdhcamHandler;

    private boolean jlPlatesEnabled = true;
    private boolean jlParamsEnabled = true;

    private boolean jcbPlateTypeEnabled = true;
    private boolean jcbHeightEnabled = true;
    private boolean jsHeightEnabled = true;
    private boolean jcbParamType2Enabled = true;

    private boolean jSFinalValueEnabled = true;
    private boolean jcbParamType1Enabled = true;
    private boolean jsDurationEnabled = true;

    private boolean jsFirstValueEnabled = true;
    private boolean jsSecondValueEnabled = true;

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
    protected ImageIcon[] plateIcons = {
            new ImageIcon(getClass().getResource("/icons/clockIcon.png")),
            new ImageIcon(getClass().getResource("/icons/posDepXIcon.png")),
            new ImageIcon(getClass().getResource("/icons/posDepYIcon.png")),
    };

    public BdhcamEditorDialog(Window owner) {
        super(owner);
        initComponents();

        jlPlates.setCellRenderer(new PlateListCellRenderer());
        jlParameters.setCellRenderer(new ParameterListCellRenderer());
        jcbPlateType.setRenderer(new PlateTypeCellRenderer());

        DefaultComboBoxModel plateTypeModel = new DefaultComboBoxModel();
        for(Camplate.Type type : Camplate.Type.values()){
            plateTypeModel.addElement(type);
        }
        jcbPlateType.setModel(plateTypeModel);

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (CamParameter.Type type : CamParameter.Type.values()) {
            model.addElement(type);
        }
        jcbParamType1.setModel(model);
        jcbParamType1.setRenderer(new ParameterTypeListCellRenderer());

        jcbParamType2.setModel(model);
        jcbParamType2.setRenderer(new ParameterTypeListCellRenderer());
    }

    private void panel1ComponentResized(ComponentEvent e) {
        int size = Math.min(displayContainer.getWidth(), displayContainer.getHeight());
        platesDisplay.setPreferredSize(new Dimension(size, size));
        displayContainer.revalidate();
    }

    private void jbPlayAnimPosIndepActionPerformed(ActionEvent e) {
        bdhcamHandler.startAnimation();
    }

    private void jlPlatesValueChanged(ListSelectionEvent e) {
        if(jlPlatesEnabled){
            int index = jlPlates.getSelectedIndex();
            if(index != -1){
                bdhcamHandler.setSelectedPlate(index);
                bdhcamHandler.stopAnimation();
                //bdhcamHandler.setPlayerInPlate(bdhcamHandler.getSelectedPlate());
                updateViewPlateData();
                platesDisplay.repaint();
            }
        }
    }

    private void jlParametersValueChanged(ListSelectionEvent e) {
        if (jlParamsEnabled) {
            int index = jlParameters.getSelectedIndex();
            if (index != -1) {
                bdhcamHandler.setIndexParamSelected(index);
            }
            updateViewParameterSettings();
        }
    }

    private void jsFinalValue1StateChanged(ChangeEvent e) {
        if (jSFinalValueEnabled) {
            float value = (Float) jsFinalValue1.getValue();
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameterPosIndep param = (CamParameterPosIndep) bdhcamHandler.getSelectedParameter();
                param.finalValue = value;
                updateViewParameterSettings();
            }
        }
    }

    private void jsDurationStateChanged(ChangeEvent e) {
        if (jsDurationEnabled) {
            int value = (Integer) jsDuration.getValue();
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameterPosIndep param = (CamParameterPosIndep) bdhcamHandler.getSelectedParameter();
                param.duration = value;
                updateViewParameterSettings();
            }
        }
    }

    private void jbAddParameterActionPerformed(ActionEvent e) {
        if (bdhcamHandler.getSelectedPlate() != null) {
            Camplate plate = bdhcamHandler.getSelectedPlate();
            plate.addParameter();
            updateViewParameterList(plate.parameters.size() - 1);
            //updateViewParameterSettings();
        }
    }

    private void jbRemoveParameterActionPerformed(ActionEvent e) {
        if (bdhcamHandler.getSelectedParameter() != null) {
            Camplate plate = bdhcamHandler.getSelectedPlate();
            plate.parameters.remove(bdhcamHandler.getIndexParamSelected());
            updateView();
        }
    }


    private void jcbParamType1ActionPerformed(ActionEvent e) {
        if (jcbParamType1Enabled) {
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameter param = bdhcamHandler.getSelectedParameter();
                param.type = CamParameter.Type.values()[jcbParamType1.getSelectedIndex()];
                updateViewParameterList();
            }
        }
    }

    private void jcbParamType2ActionPerformed(ActionEvent e) {
        if (jcbParamType2Enabled) {
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameter param = bdhcamHandler.getSelectedParameter();
                param.type = CamParameter.Type.values()[jcbParamType2.getSelectedIndex()];
                updateViewParameterList();
            }
        }
    }


    private void jsFirstValueStateChanged(ChangeEvent e) {
        if (jsFirstValueEnabled) {
            float value = (Float) jsFirstValue.getValue();
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameterPosDep param = (CamParameterPosDep) bdhcamHandler.getSelectedParameter();
                param.firstValue = value;
                updateViewParameterSettings();
            }
        }
    }

    private void jsSecondValueStateChanged(ChangeEvent e) {
        if (jsSecondValueEnabled) {
            float value = (Float) jsSecondValue.getValue();
            if (bdhcamHandler.getSelectedParameter() != null) {
                CamParameterPosDep param = (CamParameterPosDep) bdhcamHandler.getSelectedParameter();
                param.secondValue = value;
                updateViewParameterSettings();
            }
        }
    }

    private void jbAddPlateActionPerformed(ActionEvent e) {
        bdhcamHandler.getBdhcam().getPlates().add(new CamplatePosIndep());
        updateViewPlateList(bdhcamHandler.getBdhcam().getPlates().size() - 1);
        updateView();
        platesDisplay.repaint();
    }

    private void jbRemovePlateActionPerformed(ActionEvent e) {
        ArrayList<Camplate> plateList = this.bdhcamHandler.getBdhcam().getPlates();
        if (plateList.size() > 0) {
            plateList.remove(bdhcamHandler.getIndexSelected());
            updateViewPlateList(plateList.size() - 1);
            updateView();
            platesDisplay.repaint();
        }
    }
    
	private void jbDuplicatePlateActionPerformed(ActionEvent e) {
		ArrayList<Camplate> plateList = this.bdhcamHandler.getBdhcam().getPlates();
		if (plateList.size() > 0) {
			bdhcamHandler.getBdhcam().duplicatePlate(plateList.get(bdhcamHandler.getIndexSelected()));
			updateViewPlateList(plateList.size() - 1);
			updateView();
			this.platesDisplay.repaint();
		} 
	}
	
    private void jcbPlateTypeActionPerformed(ActionEvent e) {
        if (jcbPlateTypeEnabled) {
            if (bdhcamHandler.getSelectedPlate() != null) {
                bdhcamHandler.getBdhcam().changePlateType(bdhcamHandler.getIndexSelected(), jcbPlateType.getSelectedIndex());
                updateView();
            }
        }
    }
   

    private void jcbHeightActionPerformed(ActionEvent e) {
        if(jcbHeightEnabled){
            if(bdhcamHandler.getSelectedPlate() != null){
                bdhcamHandler.getSelectedPlate().useZ = jcbHeight.isSelected();
                updateViewPlateSettings();
            }
        }
    }

    private void jsHeightStateChanged(ChangeEvent e) {
        if(jsHeightEnabled){
            if(bdhcamHandler.getSelectedPlate() != null){
                bdhcamHandler.getSelectedPlate().z = (Integer)jsHeight.getValue();
                updateViewPlateSettings();
                updateViewCameraDisplay();
            }
        }
    }

    private void jlTutorialMousePressed(MouseEvent e) {
        try {
            Desktop.getDesktop().browse(new URI("https://pokehacking.com/tutorials/dynamiccameras/"));
        } catch (IOException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }


    private void jbImportBdhcamActionPerformed(ActionEvent e) {
        FileChooserUtils.selectFile(
                "Open BDHCAM",
                new File(ExportPath),
                "Terrain File (*.bdhcam)",
                new String[]{"*." + Bdhcam.fileExtension},
                selectedFile -> {
                    if (selectedFile != null) {
                        try {
                            String path = selectedFile.getPath();
                            handler.setLastBdhcDirectoryUsed(selectedFile.getParent());

                            Bdhcam bdhcam = BdhcamLoader.loadBdhcam(path);
                            bdhcamHandler.setBdhcam(bdhcam);

                            updateView();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Can't open file",
                                    "Error opening BDHCAM file", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
        );
    }

    private void jbExportBdhcamActionPerformed(ActionEvent e) {
        FileChooserUtils.saveFile(
                "Save BDHCAM",
                new File(ExportPath),
                "Camera File (*.bdhcam)",
                new String[]{"*." + Bdhcam.fileExtension},
                selectedFile -> {
                    if (selectedFile != null) {
                        try {
                            String path = selectedFile.getPath();
                            handler.setLastBdhcDirectoryUsed(selectedFile.getParent());

                            BdhcamWriter.writeBdhcamToFile(path, bdhcamHandler.getBdhcam(), handler.getBdhc(), handler.getGameIndex());
                            //BdhcamWriter.writeBdhcamToFile(path, bdhcamHandler.getBdhcam());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Can't save file",
                                    "Error saving BDHCAM", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
        );
    }

    private void BrowseButton(ActionEvent e) {
        File folder = new File(browsePathText);

        FileChooserUtils.selectFile(
                "Select the camera file",
                folder,
                selectedFile -> {
                    if (selectedFile != null && selectedFile.isFile()) {
                        browsePathText = selectedFile.getPath();
                        cameraFilePathField.setText(browsePathText);
                        readCamerBinFile(browsePathText);
                    }
                }
        );
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

    private void Browse(ActionEvent e) {
        // TODO add your code here
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Educational license - Corentin Macé
        panel1 = new JPanel();
        jbImportBdhcam = new JButton();
        jbExportBdhcam = new JButton();
        label11 = new JLabel();
        splitPane1 = new JSplitPane();
        displayContainer = new JPanel();
        platesDisplay = new BdhcamPlatesDisplay();
        splitPane3 = new JSplitPane();
        splitPane2 = new JSplitPane();
        panel2 = new JPanel();
        label2 = new JLabel();
        scrollPane1 = new JScrollPane();
        jlPlates = new JList();
        panel4 = new JPanel();
        jbAddPlate = new JButton();
        jbRemovePlate = new JButton();
        bdhcamDisplay = new BdhcamCameraDisplay();
        panel3 = new JPanel();
        label3 = new JLabel();
        panel8 = new JPanel();
        label1 = new JLabel();
        jcbPlateType = new JComboBox<>();
        jbPlayAnimPosIndep = new JButton();
        jcbHeight = new JCheckBox();
        jsHeight = new JSpinner();
        splitPane4 = new JSplitPane();
        panel5 = new JPanel();
        label5 = new JLabel();
        scrollPane2 = new JScrollPane();
        jlParameters = new JList();
        panel7 = new JPanel();
        jbAddParameter = new JButton();
        jbRemoveParameter = new JButton();
        cardPanel = new JPanel();
        panelPosIndep = new JPanel();
        label8 = new JLabel();
        jcbParamType1 = new JComboBox<>();
        label6 = new JLabel();
        jsFinalValue1 = new JSpinner();
        label7 = new JLabel();
        jsDuration = new JSpinner();
        panelPosDep = new JPanel();
        label4 = new JLabel();
        jcbParamType2 = new JComboBox<>();
        label9 = new JLabel();
        jsFirstValue = new JSpinner();
        label10 = new JLabel();
        jsSecondValue = new JSpinner();
        panel9 = new JPanel();
        label13 = new JLabel();
        distanceSlider = new JSlider();
        distanceSpinner = new JSpinner();
        label14 = new JLabel();
        vRotSlider = new JSlider();
        vRotSpinner = new JSpinner();
        label15 = new JLabel();
        hRotSlider = new JSlider();
        hRotSpinner = new JSpinner();
        label16 = new JLabel();
        zRotSlider = new JSlider();
        zRotSpinner = new JSpinner();
        label17 = new JLabel();
        fovSlider = new JSlider();
        fovSpinner = new JSpinner();
        label18 = new JLabel();
        ncSlider = new JSlider();
        ncSpinner = new JSpinner();
        label19 = new JLabel();
        fcSlider = new JSlider();
        fcSpinner = new JSpinner();
        label20 = new JLabel();
        xOffsetSlider = new JSlider();
        xOffsetSpinner = new JSpinner();
        label21 = new JLabel();
        yOffsetSlider = new JSlider();
        yOffsetSpinner = new JSpinner();
        label22 = new JLabel();
        zOffsetSlider = new JSlider();
        zOffsetSpinner = new JSpinner();
        isOrthoCheckbox = new JCheckBox();
        cameraFilePathField = new JTextField();
        BrowseButton = new JButton();
        panel6 = new JPanel();
        label12 = new JLabel();
        jlTutorial = new JLabel();

        //======== this ========
        setModal(true);
        setTitle("BDHCAM Editor");
        Container contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            "insets 0,hidemode 3,gap 5 5",
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
            "[fill]" +
            "[fill]",
            // rows
            "[fill]" +
            "[grow,fill]" +
            "[]"));

        //======== panel1 ========
        {
            panel1.setLayout(new MigLayout(
                "insets 5,hidemode 3,gap 5 5",
                // columns
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[265,grow,fill]",
                // rows
                "[fill]"));

            //---- jbImportBdhcam ----
            jbImportBdhcam.setText("Import BDHCAM");
            jbImportBdhcam.setIcon(new ImageIcon(getClass().getResource("/icons/ImportTileIcon.png")));
            jbImportBdhcam.addActionListener(e -> jbImportBdhcamActionPerformed(e));
            panel1.add(jbImportBdhcam, "cell 0 0");

            //---- jbExportBdhcam ----
            jbExportBdhcam.setText("Export BDHCAM");
            jbExportBdhcam.setIcon(new ImageIcon(getClass().getResource("/icons/ExportIcon.png")));
            jbExportBdhcam.addActionListener(e -> jbExportBdhcamActionPerformed(e));
            panel1.add(jbExportBdhcam, "cell 1 0");

            //---- label11 ----
            label11.setText("BDHCAM files are saved automatically when pressing the save map button from the main window");
            label11.setIcon(new ImageIcon(getClass().getResource("/icons/informationIcon.png")));
            panel1.add(label11, "cell 3 0");
        }
        contentPane.add(panel1, "cell 0 0 23 1,gapx 5 5,gapy 5 0");

        //======== splitPane1 ========
        {
            splitPane1.setDividerLocation(524);
            splitPane1.setResizeWeight(0.5);
            splitPane1.setLastDividerLocation(512);
            splitPane1.setPreferredSize(null);
            splitPane1.setMinimumSize(new Dimension(400, 512));

            //======== displayContainer ========
            {
                displayContainer.setBorder(new LineBorder(Color.lightGray));
                displayContainer.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        panel1ComponentResized(e);
                    }
                });
                displayContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

                //---- platesDisplay ----
                platesDisplay.setMinimumSize(new Dimension(300, 300));
                platesDisplay.setMaximumSize(new Dimension(3000, 3000));
                displayContainer.add(platesDisplay);
            }
            splitPane1.setLeftComponent(displayContainer);

            //======== splitPane3 ========
            {
                splitPane3.setOrientation(JSplitPane.VERTICAL_SPLIT);
                splitPane3.setDividerLocation(220);
                splitPane3.setPreferredSize(new Dimension(769, 512));
                splitPane3.setResizeWeight(0.5);

                //======== splitPane2 ========
                {
                    splitPane2.setDividerLocation(230);

                    //======== panel2 ========
                    {
                        panel2.setBorder(new LineBorder(Color.lightGray));
                        panel2.setLayout(new GridBagLayout());
                        ((GridBagLayout)panel2.getLayout()).columnWidths = new int[] {202, 0};
                        ((GridBagLayout)panel2.getLayout()).rowHeights = new int[] {0, 132, 0, 0};
                        ((GridBagLayout)panel2.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                        ((GridBagLayout)panel2.getLayout()).rowWeights = new double[] {0.0, 1.0, 0.0, 1.0E-4};

                        //---- label2 ----
                        label2.setText("Camera Plates:");
                        panel2.add(label2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(5, 5, 0, 5), 0, 0));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

                            //---- jlPlates ----
                            jlPlates.addListSelectionListener(e -> jlPlatesValueChanged(e));
                            scrollPane1.setViewportView(jlPlates);
                        }
                        panel2.add(scrollPane1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(5, 5, 5, 5), 0, 0));

                        //======== panel4 ========
                        {
                            panel4.setLayout(new GridLayout(1, 2, 5, 5));

                            //---- jbAddPlate ----
                            jbAddPlate.setText("Add");
                            jbAddPlate.setIcon(new ImageIcon(getClass().getResource("/icons/AddIcon.png")));
                            jbAddPlate.addActionListener(e -> jbAddPlateActionPerformed(e));
                            panel4.add(jbAddPlate);

                            //---- jbRemovePlate ----
                            jbRemovePlate.setText("Remove");
                            jbRemovePlate.setIcon(new ImageIcon(getClass().getResource("/icons/RemoveIcon.png")));
                            jbRemovePlate.addActionListener(e -> jbRemovePlateActionPerformed(e));
                            panel4.add(jbRemovePlate);
                        }
                        panel2.add(panel4, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.BASELINE, GridBagConstraints.HORIZONTAL,
                            new Insets(5, 5, 5, 5), 0, 0));
                    }
                    splitPane2.setLeftComponent(panel2);

                    //---- bdhcamDisplay ----
                    bdhcamDisplay.setBorder(LineBorder.createGrayLineBorder());
                    splitPane2.setRightComponent(bdhcamDisplay);
                }
                splitPane3.setTopComponent(splitPane2);

                //======== panel3 ========
                {
                    panel3.setBorder(new LineBorder(Color.lightGray));
                    panel3.setLayout(new MigLayout(
                        "hidemode 3",
                        // columns
                        "[193,grow,fill]",
                        // rows
                        "[]" +
                        "[]" +
                        "[grow,fill]"));

                    //---- label3 ----
                    label3.setText("Selected Plate Settings:");
                    panel3.add(label3, "cell 0 0");

                    //======== panel8 ========
                    {
                        panel8.setLayout(new GridBagLayout());
                        ((GridBagLayout)panel8.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                        ((GridBagLayout)panel8.getLayout()).rowHeights = new int[] {0, 0, 0};
                        ((GridBagLayout)panel8.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                        ((GridBagLayout)panel8.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                        //---- label1 ----
                        label1.setText("Type: ");
                        panel8.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));

                        //---- jcbPlateType ----
                        jcbPlateType.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Position Independent",
                            "Position Dependent X",
                            "Position Dependent Y"
                        }));
                        jcbPlateType.addActionListener(e -> jcbPlateTypeActionPerformed(e));
                        panel8.add(jcbPlateType, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 5, 5), 0, 0));

                        //---- jbPlayAnimPosIndep ----
                        jbPlayAnimPosIndep.setText("Play Animation");
                        jbPlayAnimPosIndep.setIcon(new ImageIcon(getClass().getResource("/icons/AnimationIcon.png")));
                        jbPlayAnimPosIndep.addActionListener(e -> jbPlayAnimPosIndepActionPerformed(e));
                        panel8.add(jbPlayAnimPosIndep, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                            new Insets(0, 0, 5, 0), 0, 0));

                        //---- jcbHeight ----
                        jcbHeight.setText("Work only at Z:");
                        jcbHeight.addActionListener(e -> jcbHeightActionPerformed(e));
                        panel8.add(jcbHeight, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));

                        //---- jsHeight ----
                        jsHeight.addChangeListener(e -> jsHeightStateChanged(e));
                        panel8.add(jsHeight, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));
                    }
                    panel3.add(panel8, "cell 0 1");

                    //======== splitPane4 ========
                    {
                        splitPane4.setDividerLocation(220);

                        //======== panel5 ========
                        {
                            panel5.setBorder(new LineBorder(Color.lightGray));
                            panel5.setLayout(new MigLayout(
                                "hidemode 3",
                                // columns
                                "[grow,fill]",
                                // rows
                                "[]" +
                                "[grow,fill]" +
                                "[]0" +
                                "[]0" +
                                "[]"));

                            //---- label5 ----
                            label5.setText("Camera Parameters:");
                            panel5.add(label5, "cell 0 0");

                            //======== scrollPane2 ========
                            {
                                scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

                                //---- jlParameters ----
                                jlParameters.addListSelectionListener(e -> jlParametersValueChanged(e));
                                scrollPane2.setViewportView(jlParameters);
                            }
                            panel5.add(scrollPane2, "cell 0 1");

                            //======== panel7 ========
                            {
                                panel7.setLayout(new GridLayout(1, 2, 5, 5));

                                //---- jbAddParameter ----
                                jbAddParameter.setText("Add");
                                jbAddParameter.setIcon(new ImageIcon(getClass().getResource("/icons/AddIcon.png")));
                                jbAddParameter.addActionListener(e -> jbAddParameterActionPerformed(e));
                                panel7.add(jbAddParameter);

                                //---- jbRemoveParameter ----
                                jbRemoveParameter.setText("Remove");
                                jbRemoveParameter.setIcon(new ImageIcon(getClass().getResource("/icons/RemoveIcon.png")));
                                jbRemoveParameter.addActionListener(e -> jbRemoveParameterActionPerformed(e));
                                panel7.add(jbRemoveParameter);
                            }
                            panel5.add(panel7, "cell 0 2");
                        }
                        splitPane4.setLeftComponent(panel5);

                        //======== cardPanel ========
                        {
                            cardPanel.setBorder(new LineBorder(Color.lightGray));
                            cardPanel.setLayout(new CardLayout());

                            //======== panelPosIndep ========
                            {
                                panelPosIndep.setLayout(new MigLayout(
                                    "insets 5,hidemode 3",
                                    // columns
                                    "[110,grow,fill]" +
                                    "[grow,fill]",
                                    // rows
                                    "[]" +
                                    "[]" +
                                    "[]"));

                                //---- label8 ----
                                label8.setText("Parameter Type:");
                                panelPosIndep.add(label8, "cell 0 0");

                                //---- jcbParamType1 ----
                                jcbParamType1.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "(00) Unknown 1",
                                    "(04) Unknown 2",
                                    "(08) Unknown 3",
                                    "(0C) Unknown 4",
                                    "(10) Unknown 5",
                                    "(14) Camera X",
                                    "(18) Camera Y",
                                    "(1C) Camera Z",
                                    "(20) Target X",
                                    "(24) Target Y",
                                    "(28) Target Z",
                                    "(2C) Camera Up X",
                                    "(30) Camera Up Y",
                                    "(34) Camera Up Z",
                                    "(38) Unkown 6",
                                    "(3C) Unkown 7",
                                    "(40) Unkown 8",
                                    "(44) Unkown 9",
                                    "(48) Target Previous X (?)",
                                    "(4C) Target Previous Y (?)",
                                    "(50) Target Previous Z (?)"
                                }));
                                jcbParamType1.addActionListener(e -> jcbParamType1ActionPerformed(e));
                                panelPosIndep.add(jcbParamType1, "cell 1 0");

                                //---- label6 ----
                                label6.setText("Final Value:");
                                panelPosIndep.add(label6, "cell 0 1");

                                //---- jsFinalValue1 ----
                                jsFinalValue1.setModel(new SpinnerNumberModel(0.0F, null, null, 1.0F));
                                jsFinalValue1.addChangeListener(e -> jsFinalValue1StateChanged(e));
                                panelPosIndep.add(jsFinalValue1, "cell 1 1");

                                //---- label7 ----
                                label7.setText("Duration (frames):");
                                panelPosIndep.add(label7, "cell 0 2");

                                //---- jsDuration ----
                                jsDuration.setModel(new SpinnerNumberModel(1, 1, null, 1));
                                jsDuration.addChangeListener(e -> jsDurationStateChanged(e));
                                panelPosIndep.add(jsDuration, "cell 1 2");
                            }
                            cardPanel.add(panelPosIndep, "cardPosIndep");

                            //======== panelPosDep ========
                            {
                                panelPosDep.setLayout(new MigLayout(
                                    "insets 5,hidemode 3",
                                    // columns
                                    "[110,grow,fill]" +
                                    "[grow,fill]",
                                    // rows
                                    "[]" +
                                    "[]" +
                                    "[]"));

                                //---- label4 ----
                                label4.setText("Parameter Type:");
                                panelPosDep.add(label4, "cell 0 0");

                                //---- jcbParamType2 ----
                                jcbParamType2.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "(00) Unknown 1",
                                    "(04) Unknown 2",
                                    "(08) Unknown 3",
                                    "(0C) Unknown 4",
                                    "(10) Unknown 5",
                                    "(14) Camera X",
                                    "(18) Camera Y",
                                    "(1C) Camera Z",
                                    "(20) Target X",
                                    "(24) Target Y",
                                    "(28) Target Z",
                                    "(2C) Camera Up X",
                                    "(30) Camera Up Y",
                                    "(34) Camera Up Z",
                                    "(38) Unkown 6",
                                    "(3C) Unkown 7",
                                    "(40) Unkown 8",
                                    "(44) Unkown 9",
                                    "(48) Target Previous X (?)",
                                    "(4C) Target Previous Y (?)",
                                    "(50) Target Previous Z (?)"
                                }));
                                jcbParamType2.addActionListener(e -> jcbParamType2ActionPerformed(e));
                                panelPosDep.add(jcbParamType2, "cell 1 0");

                                //---- label9 ----
                                label9.setText("First Value:");
                                panelPosDep.add(label9, "cell 0 1");

                                //---- jsFirstValue ----
                                jsFirstValue.setModel(new SpinnerNumberModel(0.0F, null, null, 1.0F));
                                jsFirstValue.addChangeListener(e -> jsFirstValueStateChanged(e));
                                panelPosDep.add(jsFirstValue, "cell 1 1");

                                //---- label10 ----
                                label10.setText("Second Value:");
                                panelPosDep.add(label10, "cell 0 2");

                                //---- jsSecondValue ----
                                jsSecondValue.setModel(new SpinnerNumberModel(0.0F, null, null, 1.0F));
                                jsSecondValue.addChangeListener(e -> jsSecondValueStateChanged(e));
                                panelPosDep.add(jsSecondValue, "cell 1 2");
                            }
                            cardPanel.add(panelPosDep, "cardPosDep");
                        }
                        splitPane4.setRightComponent(cardPanel);
                    }
                    panel3.add(splitPane4, "cell 0 2");
                }
                splitPane3.setBottomComponent(panel3);
            }
            splitPane1.setRightComponent(splitPane3);
        }
        contentPane.add(splitPane1, "cell 0 1");

        //======== panel9 ========
        {
            panel9.setLayout(new MigLayout(
                "flowy,fill,hidemode 3,alignx center",
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
                "[fill]",
                // rows
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]"));

            //---- label13 ----
            label13.setText("Distance");
            panel9.add(label13, "cell 0 0");
            panel9.add(distanceSlider, "cell 0 1 10 1");
            panel9.add(distanceSpinner, "cell 10 1");

            //---- label14 ----
            label14.setText("Vertical Rotation");
            panel9.add(label14, "cell 0 2 11 1");
            panel9.add(vRotSlider, "cell 0 3 10 1");
            panel9.add(vRotSpinner, "cell 10 3");

            //---- label15 ----
            label15.setText("Horizontal Rotation");
            panel9.add(label15, "cell 0 4 11 1");
            panel9.add(hRotSlider, "cell 0 5 10 1");
            panel9.add(hRotSpinner, "cell 10 5");

            //---- label16 ----
            label16.setText("Z Rotation");
            panel9.add(label16, "cell 0 6 11 1");
            panel9.add(zRotSlider, "cell 0 7 10 1");
            panel9.add(zRotSpinner, "cell 10 7");

            //---- label17 ----
            label17.setText("FOV");
            panel9.add(label17, "cell 0 8 11 1");
            panel9.add(fovSlider, "cell 0 9 10 1");
            panel9.add(fovSpinner, "cell 10 9");

            //---- label18 ----
            label18.setText("Near Clip Distance");
            panel9.add(label18, "cell 0 10 11 1");
            panel9.add(ncSlider, "cell 0 11 10 1");
            panel9.add(ncSpinner, "cell 10 11");

            //---- label19 ----
            label19.setText("Far Clip Distance");
            panel9.add(label19, "cell 0 13 11 1");
            panel9.add(fcSlider, "cell 0 14 10 1");
            panel9.add(fcSpinner, "cell 10 14");

            //---- label20 ----
            label20.setText("X Displacement");
            panel9.add(label20, "cell 0 15 11 1");
            panel9.add(xOffsetSlider, "cell 0 16 10 1");
            panel9.add(xOffsetSpinner, "cell 10 16");

            //---- label21 ----
            label21.setText("Y Displacement");
            panel9.add(label21, "cell 0 17 11 1");
            panel9.add(yOffsetSlider, "cell 0 18 10 1");
            panel9.add(yOffsetSpinner, "cell 10 18");

            //---- label22 ----
            label22.setText("Z Displacement");
            panel9.add(label22, "cell 0 19 11 1");
            panel9.add(zOffsetSlider, "cell 0 20 10 1");
            panel9.add(zOffsetSpinner, "cell 10 20");

            //---- isOrthoCheckbox ----
            isOrthoCheckbox.setText("Orthographic");
            panel9.add(isOrthoCheckbox, "cell 0 21 11 1");
            panel9.add(cameraFilePathField, "cell 0 23 10 1");

            //---- BrowseButton ----
            BrowseButton.setText("Browse...");
            BrowseButton.addActionListener(e -> {
			Browse(e);
			BrowseButton(e);
		});
            panel9.add(BrowseButton, "cell 10 23");
        }
        contentPane.add(panel9, "cell 1 1 22 1,dock center");

        //======== panel6 ========
        {
            panel6.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[fill]" +
                "[fill]",
                // rows
                "[]"));

            //---- label12 ----
            label12.setText("Warning: SDSME doesn't import BDHCAM files, use DSPRE instead. Also, BDCAM files need ASM routines in order to work. Follow Mikelan98's tutorial for more info:");
            label12.setIcon(new ImageIcon(getClass().getResource("/icons/WarningIcon.png")));
            panel6.add(label12, "cell 0 0");

            //---- jlTutorial ----
            jlTutorial.setText("<html><body><a href=\"https://pokehacking.com/tutorials/dynamiccameras/\">Tutorial</a></body></html>");
            jlTutorial.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            jlTutorial.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    jlTutorialMousePressed(e);
                }
            });
            panel6.add(jlTutorial, "cell 1 0");
        }
        contentPane.add(panel6, "cell 0 2");
        setSize(1750, 945);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    public void init(MapEditorHandler handler, BufferedImage mapImage) {
        this.handler = handler;

        this.bdhcamHandler = new BdhcamHandler(handler, this);
        platesDisplay.init(handler, bdhcamHandler, mapImage);

        bdhcamDisplay.setHandler(handler, bdhcamHandler);
        bdhcamDisplay.requestUpdate();

        updateView();
    }

    public void updateView() {
        updateViewPlateList();
        updateViewPlateData();
        platesDisplay.repaint();
    }

    public void updateViewPlateData(){
        updateViewParameterList();
        updateViewPlateSettings();
        updateViewCardPanel();
        updateViewParameterSettings();
        updateViewCameraDisplay();
    }

    public void updateViewPlateList(int indexSelected) {
        DefaultListModel<Camplate> model = new DefaultListModel<>();
        for (Camplate camplate : bdhcamHandler.getBdhcam().getPlates()) {
            model.addElement(camplate);
        }
        jlPlatesEnabled = false;
        jlPlates.setModel(model);
        jlPlatesEnabled = true;

        if (indexSelected >= model.getSize()) {
            indexSelected = model.getSize() - 1;
        }
        if (indexSelected >= 0) {
            bdhcamHandler.setSelectedPlate(indexSelected);
        } else {
            bdhcamHandler.setSelectedPlate(0);
        }
        bdhcamHandler.stopAnimation();

        jlPlates.setSelectedIndex(bdhcamHandler.getIndexSelected());
    }

    public void updateViewPlateList() {
        updateViewPlateList(bdhcamHandler.getIndexSelected());
    }

    public void updateViewParameterList(int indexSelected) {
        if (bdhcamHandler.getBdhcam().getPlates().size() > 0) {
            Camplate plate = bdhcamHandler.getSelectedPlate();
            DefaultListModel<CamParameter> model = new DefaultListModel<>();
            for (CamParameter param : plate.parameters) {
                model.addElement(param);
            }

            jlParamsEnabled = false;
            jlParameters.setModel(model);
            jlParamsEnabled = true;

            if (indexSelected >= model.getSize()) {
                indexSelected = model.getSize() - 1;
            }
            if (indexSelected >= 0) {
                bdhcamHandler.setIndexParamSelected(indexSelected);
            } else {
                bdhcamHandler.setIndexParamSelected(0);
            }
            jlParameters.setSelectedIndex(bdhcamHandler.getIndexParamSelected());
        } else {
            jlParameters.setModel(new DefaultListModel<>());
        }
    }

    public void updateViewParameterList() {
        updateViewParameterList(bdhcamHandler.getIndexParamSelected());
    }

    public void updateViewCardPanel() {
        if (bdhcamHandler.getBdhcam().getPlates().size() > 0) {
            CardLayout cl = (CardLayout) (cardPanel.getLayout());

            Camplate plate = bdhcamHandler.getSelectedPlate();
            if (plate.type.ID == Camplate.Type.POS_INDEPENDENT.ID) {
                cl.show(cardPanel, "cardPosIndep");
            } else {
                cl.show(cardPanel, "cardPosDep");
            }
        }
    }

    public void updateViewParameterSettings() {
        if (bdhcamHandler.getBdhcam().getPlates().size() > 0) {
            Camplate plate = bdhcamHandler.getSelectedPlate();
            //if (plate.parameters.size() > 0) {
            if (plate.type.ID == Camplate.Type.POS_INDEPENDENT.ID) {
                updateViewParamPosIndep();
            } else {
                updateViewParamPosDep();
            }
            //}
        }
    }

    public void updateViewParamPosIndep() {
        CamParameterPosIndep param = (CamParameterPosIndep) bdhcamHandler.getSelectedParameter();
        if (param != null) {
            jcbParamType1.setEnabled(true);
            jsFinalValue1.setEnabled(true);
            jsDuration.setEnabled(true);

            jcbParamType1Enabled = false;
            jcbParamType1.setSelectedIndex(param.type.index);
            jcbParamType1Enabled = true;

            jSFinalValueEnabled = false;
            jsFinalValue1.setValue(param.finalValue);
            jSFinalValueEnabled = true;

            jsDurationEnabled = false;
            jsDuration.setValue(param.duration);
            jsDurationEnabled = true;

        } else {
            jcbParamType1.setEnabled(false);
            jsFinalValue1.setEnabled(false);
            jsDuration.setEnabled(false);
        }
    }

    public void updateViewParamPosDep() {
        CamParameterPosDep param = (CamParameterPosDep) bdhcamHandler.getSelectedParameter();
        if (param != null) {
            jcbParamType2.setEnabled(true);
            jsSecondValue.setEnabled(true);
            jsFirstValue.setEnabled(true);

            jcbParamType2Enabled = false;
            jcbParamType2.setSelectedIndex(param.type.index);
            jcbParamType2Enabled = true;

            jsSecondValueEnabled = false;
            jsSecondValue.setValue(param.secondValue);
            jsSecondValueEnabled = true;

            jsFirstValueEnabled = false;
            jsFirstValue.setValue(param.firstValue);
            jsFirstValueEnabled = true;
        } else {
            jcbParamType2.setEnabled(false);
            jsSecondValue.setEnabled(false);
            jsFirstValue.setEnabled(false);

        }
    }

    public void updateViewCameraDisplay() {
        if (bdhcamHandler.getBdhcam().getPlates().size() > 0) {
            bdhcamDisplay.setCamera(new CameraSettings(bdhcamHandler.getSelectedPlate(), 0.0f));
            //bdhcamDisplay.setCamera(new CameraSettings());
            bdhcamDisplay.repaint();
        }
    }

    public void updateViewPlateSettings(){
        if(bdhcamHandler.getSelectedPlate() != null){
            jcbPlateTypeEnabled = false;
            jcbPlateType.setSelectedIndex(bdhcamHandler.getSelectedPlate().type.ID);
            jcbPlateType.setForeground(bdhcamHandler.getSelectedPlate().type.color);
            //(jcbPlateType.get).setIcon(plateIcons[bdhcamHandler.getSelectedPlate().type.ID]);
            jcbPlateTypeEnabled = true;

            jcbHeightEnabled = false;
            jcbHeight.setSelected(bdhcamHandler.getSelectedPlate().useZ);
            jcbHeightEnabled = true;

            jsHeightEnabled = false;
            jsHeight.setValue(bdhcamHandler.getSelectedPlate().z);
            jsHeightEnabled = true;
            if(bdhcamHandler.getSelectedPlate().useZ){
                jsHeight.setEnabled(true);
            }else{
                jsHeight.setEnabled(false);
            }

        }
    }

    public BdhcamCameraDisplay getBdhcamDisplay() {
        return bdhcamDisplay;
    }

    public BdhcamPlatesDisplay getPlatesDisplay() {
        return platesDisplay;
    }

    private class PlateListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel jlabel = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Camplate plate = (Camplate) value;
            Color c = plate.getFillColor();
            String colorHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            jlabel.setText(
                    "<html><b>Plate " + String.valueOf(index) + "</b>" +
                            " [<font color='" + colorHex + "'> " +
                            plate.type.name +
                            "<font color='black'>" + "]" + "</font></html>");

            return jlabel;
        }
    }

    private class ParameterListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel jlabel = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            CamParameter parameter = (CamParameter) value;
            Color c = parameter.type.color;
            String colorHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            jlabel.setText(
                    "<html><b><font color='" + colorHex + "'>" + parameter.type.name + "</font></b>" +
                            "<font color='black'></font></html>");

            return jlabel;
        }
    }

    private class ParameterTypeListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel jlabel = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            CamParameter.Type parameter = (CamParameter.Type) value;
            Color c = parameter.color;
            String colorHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            jlabel.setText(
                    "<html><font color='" + colorHex + "'>" + parameter.name + "</font>" +
                            "<font color='black'></font></html>");

            return jlabel;
        }
    }

    private class PlateTypeCellRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Camplate.Type type = (Camplate.Type) value;
            c.setForeground(type.color);
            c.setIcon(plateIcons[type.ID]);
            c.setText(type.name);

            return c;
        }

    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Educational license - Corentin Macé
    private JPanel panel1;
    private JButton jbImportBdhcam;
    private JButton jbExportBdhcam;
    private JLabel label11;
    private JSplitPane splitPane1;
    private JPanel displayContainer;
    private BdhcamPlatesDisplay platesDisplay;
    private JSplitPane splitPane3;
    private JSplitPane splitPane2;
    private JPanel panel2;
    private JLabel label2;
    private JScrollPane scrollPane1;
    private JList jlPlates;
    private JPanel panel4;
    private JButton jbAddPlate;
    private JButton jbRemovePlate;
    private BdhcamCameraDisplay bdhcamDisplay;
    private JPanel panel3;
    private JLabel label3;
    private JPanel panel8;
    private JLabel label1;
    private JComboBox<String> jcbPlateType;
    private JButton jbPlayAnimPosIndep;
    private JCheckBox jcbHeight;
    private JSpinner jsHeight;
    private JSplitPane splitPane4;
    private JPanel panel5;
    private JLabel label5;
    private JScrollPane scrollPane2;
    private JList jlParameters;
    private JPanel panel7;
    private JButton jbAddParameter;
    private JButton jbRemoveParameter;
    private JPanel cardPanel;
    private JPanel panelPosIndep;
    private JLabel label8;
    private JComboBox<String> jcbParamType1;
    private JLabel label6;
    private JSpinner jsFinalValue1;
    private JLabel label7;
    private JSpinner jsDuration;
    private JPanel panelPosDep;
    private JLabel label4;
    private JComboBox<String> jcbParamType2;
    private JLabel label9;
    private JSpinner jsFirstValue;
    private JLabel label10;
    private JSpinner jsSecondValue;
    private JPanel panel9;
    private JLabel label13;
    private JSlider distanceSlider;
    private JSpinner distanceSpinner;
    private JLabel label14;
    private JSlider vRotSlider;
    private JSpinner vRotSpinner;
    private JLabel label15;
    private JSlider hRotSlider;
    private JSpinner hRotSpinner;
    private JLabel label16;
    private JSlider zRotSlider;
    private JSpinner zRotSpinner;
    private JLabel label17;
    private JSlider fovSlider;
    private JSpinner fovSpinner;
    private JLabel label18;
    private JSlider ncSlider;
    private JSpinner ncSpinner;
    private JLabel label19;
    private JSlider fcSlider;
    private JSpinner fcSpinner;
    private JLabel label20;
    private JSlider xOffsetSlider;
    private JSpinner xOffsetSpinner;
    private JLabel label21;
    private JSlider yOffsetSlider;
    private JSpinner yOffsetSpinner;
    private JLabel label22;
    private JSlider zOffsetSlider;
    private JSpinner zOffsetSpinner;
    private JCheckBox isOrthoCheckbox;
    private JTextField cameraFilePathField;
    private JButton BrowseButton;
    private JPanel panel6;
    private JLabel label12;
    private JLabel jlTutorial;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
