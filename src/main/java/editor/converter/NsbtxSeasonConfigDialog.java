package editor.converter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.*;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import editor.MainFrame;
import editor.buildingeditor2.tileset.BuildTileset;
import editor.mapmatrix.MapMatrix;
import jogamp.graph.font.typecast.ot.Fixed;
import net.miginfocom.swing.*;
import utils.FileChooserUtils;

import static editor.MainFrame.prefs;
import static editor.mapmatrix.MapMatrix.ExportPath;

/**
 * @author Corentin
 */
public class NsbtxSeasonConfigDialog extends JDialog {
    public NsbtxSeasonConfigDialog(Window owner) {
        super(owner);
        initComponents();
        try {
            String springTilesetPath = prefs.get("SpringTileset", "");
            springTextureFoldertxt.setText(springTilesetPath);
            String summerTilesetPath = prefs.get("SummerTileset", "");
            SummerTextureFolderTxt.setText(summerTilesetPath);
            String fallTilesetPath = prefs.get("FallTileset", "");
            FallTextureFolderTxt.setText(fallTilesetPath);
            String winterTilesetPath = prefs.get("WinterTileset", "");
            WinterTextureFolderTxt.setText(winterTilesetPath);
            String fixedTexturePath = prefs.get("FixedTextureTileset", "");
            FixedTextureTilesetTxt.setText(fixedTexturePath);
        } catch (Exception ex) {
            System.err.println("Failed to get preferences");
        }
    }

    private void ok(ActionEvent e) {
        prefs.put("SpringTileset", springTextureFoldertxt.getText());
        prefs.put("SummerTileset",  SummerTextureFolderTxt.getText());
        prefs.put("WinterTileset",  WinterTextureFolderTxt.getText());
        prefs.put("FallTileset",  FallTextureFolderTxt.getText());
        prefs.put("FixedTextureTileset", FixedTextureTilesetTxt.getText());
        this.dispose();
    }

    private void browseSpringTextureFolder(ActionEvent e) {
        globalBrowseEventHandler("Select the folder of the spring textures", path -> {
            if (path != null) {
                springTextureFoldertxt.setText(path);
            }
        });
    }

    private void browseSummerTextureFolder(ActionEvent e) {
        globalBrowseEventHandler("Select the folder of the summer textures", path -> {
            if (path != null) {
                SummerTextureFolderTxt.setText(path);
            }
        });
    }

    private void browseFallTextureFolder(ActionEvent e) {
        globalBrowseEventHandler("Select the folder of the fall textures", path -> {
            if (path != null) {
                FallTextureFolderTxt.setText(path);
            }
        });
    }

    private void browseWinterTextureFolder(ActionEvent e) {
        globalBrowseEventHandler("Select the folder of the winter textures", path -> {
            if (path != null) {
                WinterTextureFolderTxt.setText(path);
            }
        });
    }

    private void browseTilesetFixedBtn(ActionEvent e) {
        File folder = new File(ExportPath);

        FileChooserUtils.selectFile(
                "Select the tileset with the fixed textures and palette",
                folder,
                "NSBTX (*.nsbtx)",
                new String[]{"*." + BuildTileset.ext },
                selectedFile -> {
                    if (selectedFile != null && selectedFile.exists()) {
                        FixedTextureTilesetTxt.setText(selectedFile.getPath());
                    }
                }
        );
    }

    private void globalBrowseEventHandler(String description, Consumer<String> onFolderSelected) {
        File folder = new File(ExportPath);

        FileChooserUtils.selectDirectory(
                description,
                folder,
                selectedDirectory -> {
                    if (selectedDirectory != null && selectedDirectory.isDirectory()) {
                        onFolderSelected.accept(selectedDirectory.getPath());
                    } else {
                        onFolderSelected.accept(null);
                    }
                }
        );
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Educational license - Corentin Macé
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        springTextureFoldertxt = new JTextField();
        browseSpringTextureFolder = new JButton();
        label2 = new JLabel();
        SummerTextureFolderTxt = new JTextField();
        browseSummerTextureFolder = new JButton();
        label3 = new JLabel();
        FallTextureFolderTxt = new JTextField();
        browseFallTextureFolder = new JButton();
        label4 = new JLabel();
        WinterTextureFolderTxt = new JTextField();
        browseWinterTextureFolder = new JButton();
        label5 = new JLabel();
        FixedTextureTilesetTxt = new JTextField();
        browseTilesetFixedBtn = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setMinimumSize(new Dimension(640, 300));
        setPreferredSize(new Dimension(640, 300));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new MigLayout(
                    "fill,insets dialog,hidemode 3",
                    // columns
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]",
                    // rows
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]"));

                //---- label1 ----
                label1.setText("Spring Texture Folder");
                contentPanel.add(label1, "cell 0 0");

                //---- springTextureFoldertxt ----
                springTextureFoldertxt.setEditable(false);
                contentPanel.add(springTextureFoldertxt, "cell 1 0 2 1");

                //---- browseSpringTextureFolder ----
                browseSpringTextureFolder.setText("Browse");
                browseSpringTextureFolder.addActionListener(e -> browseSpringTextureFolder(e));
                contentPanel.add(browseSpringTextureFolder, "cell 3 0");

                //---- label2 ----
                label2.setText("Summer Texture Folder");
                contentPanel.add(label2, "cell 0 1");
                contentPanel.add(SummerTextureFolderTxt, "cell 1 1 2 1");

                //---- browseSummerTextureFolder ----
                browseSummerTextureFolder.setText("Browse");
                browseSummerTextureFolder.addActionListener(e -> browseSummerTextureFolder(e));
                contentPanel.add(browseSummerTextureFolder, "cell 3 1");

                //---- label3 ----
                label3.setText("Fall Texture Folder");
                contentPanel.add(label3, "cell 0 2");
                contentPanel.add(FallTextureFolderTxt, "cell 1 2 2 1");

                //---- browseFallTextureFolder ----
                browseFallTextureFolder.setText("Browse");
                browseFallTextureFolder.addActionListener(e -> browseFallTextureFolder(e));
                contentPanel.add(browseFallTextureFolder, "cell 3 2");

                //---- label4 ----
                label4.setText("Winter Texture Folder");
                contentPanel.add(label4, "cell 0 3");
                contentPanel.add(WinterTextureFolderTxt, "cell 1 3 2 1");

                //---- browseWinterTextureFolder ----
                browseWinterTextureFolder.setText("Browse");
                browseWinterTextureFolder.addActionListener(e -> browseWinterTextureFolder(e));
                contentPanel.add(browseWinterTextureFolder, "cell 3 3");

                //---- label5 ----
                label5.setText("Fixed Texture  Tileset");
                contentPanel.add(label5, "cell 0 4");
                contentPanel.add(FixedTextureTilesetTxt, "cell 1 4 2 1");

                //---- browseTilesetFixedBtn ----
                browseTilesetFixedBtn.setText("Browse");
                browseTilesetFixedBtn.addActionListener(e -> browseTilesetFixedBtn(e));
                contentPanel.add(browseTilesetFixedBtn, "cell 3 4");
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                    "insets dialog,alignx right",
                    // columns
                    "[button,fill]",
                    // rows
                    null));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> ok(e));
                buttonBar.add(okButton, "cell 0 0");
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Educational license - Corentin Macé
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JTextField springTextureFoldertxt;
    private JButton browseSpringTextureFolder;
    private JLabel label2;
    private JTextField SummerTextureFolderTxt;
    private JButton browseSummerTextureFolder;
    private JLabel label3;
    private JTextField FallTextureFolderTxt;
    private JButton browseFallTextureFolder;
    private JLabel label4;
    private JTextField WinterTextureFolderTxt;
    private JButton browseWinterTextureFolder;
    private JLabel label5;
    private JTextField FixedTextureTilesetTxt;
    private JButton browseTilesetFixedBtn;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
