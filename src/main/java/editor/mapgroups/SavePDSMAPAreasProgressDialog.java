package editor.mapgroups;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Progress tracker for the "Split PDSMAP by Area" export, modeled on the
 * NSBMD/NSBTX bulk exporter info dialogs: one row per area with its save
 * status, an overall progress bar and a result summary.
 *
 * @author AdAstra
 */
public class SavePDSMAPAreasProgressDialog extends JDialog {

    private static final Color GREEN = new Color(6, 176, 37);
    private static final Color RED = Color.RED;
    private static final Color GRAY = new Color(128, 128, 128);

    public enum SaveStatus {
        PENDING("PENDING", GRAY),
        SAVING("SAVING...", null),
        SAVED("SAVED", GREEN),
        FAILED("FAILED", RED);

        public final String msg;
        public final Color color;

        SaveStatus(String msg, Color color) {
            this.msg = msg;
            this.color = color;
        }
    }

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[][]{}, new String[]{"Area", "Status"}) {
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel processedLabel = new JLabel("0 / 0");
    private final JLabel savedLabel = new JLabel("0");
    private final JLabel failedLabel = new JLabel("0");
    private final JLabel statusLabel = new JLabel("Saving...");
    private final JLabel resultLabel = new JLabel(" ");
    private final JTextArea errorArea = new JTextArea(4, 20);
    private final JButton acceptButton = new JButton("OK");

    private List<Integer> areaIndices = new ArrayList<>();
    private final ArrayList<String> errorMsgs = new ArrayList<>();
    private int processed = 0;
    private int saved = 0;
    private int failed = 0;

    public SavePDSMAPAreasProgressDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    private void initComponents() {
        //Closing is blocked until the save finishes
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Saving Areas as PDSMAP");
        setModal(true);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusColumnCellRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateErrorArea();
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(420, 220));
        add(tableScrollPane, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout(0, 6));

        JPanel progressPanel = new JPanel(new BorderLayout(8, 0));
        progressPanel.add(new JLabel("Area saving progress:"), BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        infoPanel.add(progressPanel, BorderLayout.NORTH);

        Font boldFont = statusLabel.getFont().deriveFont(Font.BOLD);
        processedLabel.setFont(boldFont);
        savedLabel.setFont(boldFont);
        failedLabel.setFont(boldFont);
        statusLabel.setFont(boldFont);
        resultLabel.setFont(boldFont);

        JPanel countsPanel = new JPanel(new GridLayout(0, 2, 8, 4));
        countsPanel.add(new JLabel("Status:"));
        countsPanel.add(statusLabel);
        countsPanel.add(new JLabel("Areas processed:"));
        countsPanel.add(processedLabel);
        countsPanel.add(new JLabel("Areas saved:"));
        countsPanel.add(savedLabel);
        countsPanel.add(new JLabel("Areas not saved:"));
        countsPanel.add(failedLabel);
        countsPanel.add(new JLabel("Result:"));
        countsPanel.add(resultLabel);
        infoPanel.add(countsPanel, BorderLayout.CENTER);

        errorArea.setEditable(false);
        errorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBorder(BorderFactory.createTitledBorder("Error info"));
        errorPanel.add(new JScrollPane(errorArea), BorderLayout.CENTER);
        infoPanel.add(errorPanel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 6));
        bottomPanel.add(infoPanel, BorderLayout.CENTER);

        acceptButton.setEnabled(false);
        acceptButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.add(acceptButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(acceptButton);

        pack();
        setLocationRelativeTo(getOwner());
    }

    public void init(List<Integer> areaIndices) {
        this.areaIndices = new ArrayList<>(areaIndices);
        errorMsgs.clear();
        tableModel.setRowCount(0);
        for (Integer areaIndex : areaIndices) {
            tableModel.addRow(new Object[]{"Area " + areaIndex, SaveStatus.PENDING});
            errorMsgs.add(null);
        }
        progressBar.setMinimum(0);
        progressBar.setMaximum(Math.max(areaIndices.size(), 1));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0 / " + areaIndices.size());
        processedLabel.setText("0 / " + areaIndices.size());
    }

    public void areaStarted(int areaIndex) {
        SwingUtilities.invokeLater(() -> setRowStatus(areaIndex, SaveStatus.SAVING, null));
    }

    public void areaSaved(int areaIndex) {
        SwingUtilities.invokeLater(() -> {
            setRowStatus(areaIndex, SaveStatus.SAVED, null);
            saved++;
            savedLabel.setForeground(GREEN);
            areaProcessed();
        });
    }

    public void areaFailed(int areaIndex, String errorMsg) {
        SwingUtilities.invokeLater(() -> {
            setRowStatus(areaIndex, SaveStatus.FAILED, errorMsg);
            failed++;
            failedLabel.setForeground(RED);
            areaProcessed();
        });
    }

    public void allFinished() {
        SwingUtilities.invokeLater(() -> {
            if (failed > 0) {
                statusLabel.setForeground(RED);
                statusLabel.setText("Finished with errors");
                resultLabel.setForeground(RED);
                resultLabel.setText(failed + " Area(s) could not be saved as PDSMAP");
            } else {
                statusLabel.setForeground(GREEN);
                statusLabel.setText("Finished");
                resultLabel.setForeground(GREEN);
                resultLabel.setText("All the Areas have been saved as PDSMAP");
            }
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            acceptButton.setEnabled(true);
            acceptButton.requestFocus();
        });
    }

    private void areaProcessed() {
        processed++;
        processedLabel.setText(processed + " / " + areaIndices.size());
        savedLabel.setText(String.valueOf(saved));
        failedLabel.setText(String.valueOf(failed));
        progressBar.setValue(processed);
        progressBar.setString(processed + " / " + areaIndices.size());
    }

    private void setRowStatus(int areaIndex, SaveStatus status, String errorMsg) {
        int row = areaIndices.indexOf(areaIndex);
        if (row < 0) {
            return;
        }
        tableModel.setValueAt(status, row, 1);
        errorMsgs.set(row, errorMsg);
        if (status == SaveStatus.FAILED) {
            table.setRowSelectionInterval(row, row);
        }
        updateErrorArea();
    }

    private void updateErrorArea() {
        int row = table.getSelectedRow();
        String errorMsg = (row >= 0 && row < errorMsgs.size()) ? errorMsgs.get(row) : null;
        errorArea.setText(errorMsg == null ? "" : errorMsg);
    }

    private static class StatusColumnCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (value instanceof SaveStatus) {
                SaveStatus status = (SaveStatus) value;
                label.setText(status.msg);
                label.setForeground(status.color != null ? status.color : table.getForeground());
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            setHorizontalAlignment(JLabel.CENTER);
            return label;
        }
    }
}
