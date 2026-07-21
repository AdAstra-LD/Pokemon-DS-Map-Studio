package editor.remap;

import editor.grid.MapGrid;
import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

public final class ReplaceRemapDialog extends JDialog {

    private static final int SCOPE_ACTIVE = 0;
    private static final int SCOPE_SELECTED = 1;
    private static final int SCOPE_ALL = 2;

    private final MapEditorHandler handler;
    private final PdsmsRemapProjectAccess access;
    private final MappingTableModel mappingModel = new MappingTableModel();
    private final PreviewTableModel previewModel = new PreviewTableModel();
    private final DefaultListModel<Point> mapModel = new DefaultListModel<>();
    private final DefaultListModel<Integer> layerModel = new DefaultListModel<>();

    private final JTable mappingTable = new JTable(mappingModel);
    private final JTable previewTable = new JTable(previewModel);
    private final JList<Point> mapList = new JList<>(mapModel);
    private final JList<Integer> layerList = new JList<>(layerModel);
    private final JComboBox<String> scope = new JComboBox<>(new String[]{
            "Active map", "Selected maps", "All maps"
    });
    private final JCheckBox replaceTiles = new JCheckBox("Tiles", true);
    private final JCheckBox remapPermissions = new JCheckBox("Permissions");
    private final JCheckBox remapTypes = new JCheckBox("Types");
    private final JTextArea diagnostics = new JTextArea();
    private final JProgressBar progress = new JProgressBar();
    private final JButton previewButton = new JButton("Preview");
    private final JButton applyButton = new JButton("Apply");
    private final JButton cancelButton = new JButton("Close");

    private ReplaceRemapPlan currentPlan;
    private SwingWorker<?, ?> worker;

    public ReplaceRemapDialog(Frame owner, MapEditorHandler handler) {
        super(owner, "Replace and Remap", true);
        this.handler = handler;
        this.access = new PdsmsRemapProjectAccess(handler);
        initializeModels();
        initializeComponents();
        buildLayout();
        pack();
        setMinimumSize(new Dimension(780, 600));
        setSize(new Dimension(900, 680));
        setLocationRelativeTo(owner);
    }

    private void initializeModels() {
        for (Point point : access.getMapCoordinates()) {
            mapModel.addElement(point);
        }
        for (int layer = 0; layer < MapGrid.numLayers; layer++) {
            layerModel.addElement(layer);
        }
        int source = handler.getTileset().size() == 0 ? 0
                : Math.min(handler.getTileIndexSelected(), handler.getTileset().size() - 1);
        mappingModel.add(source, source);
    }

    private void initializeComponents() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeOrCancel();
            }
        });

        int maxTile = Math.max(0, handler.getTileset().size() - 1);
        TableColumn sourceColumn = mappingTable.getColumnModel().getColumn(0);
        TableColumn replacementColumn = mappingTable.getColumnModel().getColumn(1);
        sourceColumn.setCellEditor(new SpinnerCellEditor(maxTile));
        replacementColumn.setCellEditor(new SpinnerCellEditor(maxTile));
        mappingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mappingTable.setRowHeight(24);

        mapList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mapList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                Point point = (Point) value;
                return super.getListCellRendererComponent(list,
                        access.getMapName(point) + "  " + ReplaceRemapRequest.formatPoint(point),
                        index, selected, focus);
            }
        });
        layerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        layerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        "Layer " + (((Integer) value) + 1), index, selected, focus);
            }
        });
        layerList.setSelectedIndex(handler.getActiveLayerIndex());
        selectActiveMap();
        mapList.setEnabled(false);

        ReplaceRemapRequest.Capabilities capabilities =
                ReplaceRemapRequest.Capabilities.forGame(handler.getGameIndex());
        remapPermissions.setEnabled(capabilities.canRemapPermissions());
        remapTypes.setEnabled(capabilities.canRemapTypes());
        if (!capabilities.canRemapPermissions()) {
            remapPermissions.setToolTipText(capabilities.getPermissionUnavailableReason());
        }
        if (!capabilities.canRemapTypes()) {
            remapTypes.setToolTipText(capabilities.getTypeUnavailableReason());
        }

        diagnostics.setEditable(false);
        diagnostics.setLineWrap(true);
        diagnostics.setWrapStyleWord(true);
        diagnostics.setRows(5);
        diagnostics.setText("Preview required before Apply.");
        previewTable.setFillsViewportHeight(true);
        previewTable.setAutoCreateRowSorter(true);

        progress.setIndeterminate(true);
        progress.setVisible(false);
        applyButton.setEnabled(false);

        mappingModel.addTableModelListener(event -> {
            if (event.getType() != TableModelEvent.HEADER_ROW) {
                invalidatePreview();
            }
        });
        mapList.addListSelectionListener(event -> invalidatePreview());
        layerList.addListSelectionListener(event -> invalidatePreview());
        replaceTiles.addActionListener(event -> invalidatePreview());
        remapPermissions.addActionListener(event -> invalidatePreview());
        remapTypes.addActionListener(event -> invalidatePreview());
        scope.addActionListener(event -> {
            updateScopeSelection();
            invalidatePreview();
        });
        previewButton.addActionListener(event -> startPreview());
        applyButton.addActionListener(event -> startApply());
        cancelButton.addActionListener(event -> closeOrCancel());
    }

    private void buildLayout() {
        JPanel mappings = new JPanel(new MigLayout("insets 6, fill", "[grow][]", "[grow]"));
        mappings.setBorder(BorderFactory.createTitledBorder("Replacements"));
        mappings.add(new JScrollPane(mappingTable), "grow");
        JPanel mappingButtons = new JPanel(new MigLayout("insets 0, wrap 1", "[]", "[][]push"));
        JButton add = new JButton(new ImageIcon(getClass().getResource("/icons/AddIcon.png")));
        add.setToolTipText("Add replacement");
        add.addActionListener(event -> {
            mappingModel.add(0, 0);
            mappingTable.changeSelection(mappingModel.getRowCount() - 1, 0, false, false);
        });
        JButton remove = new JButton(new ImageIcon(getClass().getResource("/icons/RemoveIcon.png")));
        remove.setToolTipText("Remove selected replacement");
        remove.addActionListener(event -> {
            int row = mappingTable.getSelectedRow();
            if (row >= 0) {
                mappingModel.remove(mappingTable.convertRowIndexToModel(row));
            }
        });
        mappingButtons.add(add, "w 34!, h 34!");
        mappingButtons.add(remove, "w 34!, h 34!");
        mappings.add(mappingButtons, "top");

        JPanel targets = new JPanel(new MigLayout("insets 6, fill", "[grow][grow]", "[][grow]"));
        targets.setBorder(BorderFactory.createTitledBorder("Targets"));
        targets.add(scope, "span 2, growx");
        JScrollPane mapsScroll = new JScrollPane(mapList);
        mapsScroll.setBorder(BorderFactory.createTitledBorder("Maps"));
        JScrollPane layersScroll = new JScrollPane(layerList);
        layersScroll.setBorder(BorderFactory.createTitledBorder("Layers"));
        targets.add(mapsScroll, "grow");
        targets.add(layersScroll, "grow");

        JSplitPane inputSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mappings, targets);
        inputSplit.setResizeWeight(0.55);

        JPanel options = new JPanel(new MigLayout("insets 4", "[][][]push", "[]"));
        options.setBorder(BorderFactory.createTitledBorder("Remap"));
        options.add(replaceTiles);
        options.add(remapPermissions);
        options.add(remapTypes);

        JPanel preview = new JPanel(new MigLayout("insets 0, fill", "[grow]", "[grow][pref!]"));
        JScrollPane previewScroll = new JScrollPane(previewTable);
        previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));
        JScrollPane diagnosticsScroll = new JScrollPane(diagnostics);
        diagnosticsScroll.setBorder(BorderFactory.createTitledBorder("Diagnostics"));
        preview.add(previewScroll, "grow, wrap");
        preview.add(diagnosticsScroll, "growx");

        JPanel actions = new JPanel(new MigLayout("insets 6", "[grow][pref!][pref!][pref!]", "[]"));
        actions.add(progress, "growx");
        actions.add(previewButton, "w 90!");
        actions.add(applyButton, "w 90!");
        actions.add(cancelButton, "w 90!");

        setLayout(new MigLayout("insets 8, fill", "[grow]", "[220!][pref!][grow][pref!]"));
        add(inputSplit, "grow, wrap");
        add(options, "growx, wrap");
        add(preview, "grow, wrap");
        add(actions, "growx");
    }

    private void updateScopeSelection() {
        int selectedScope = scope.getSelectedIndex();
        mapList.setEnabled(selectedScope == SCOPE_SELECTED);
        if (selectedScope == SCOPE_ACTIVE) {
            selectActiveMap();
        } else if (selectedScope == SCOPE_ALL && mapModel.size() > 0) {
            mapList.setSelectionInterval(0, mapModel.size() - 1);
        } else if (selectedScope == SCOPE_SELECTED && mapList.isSelectionEmpty()) {
            selectActiveMap();
        }
    }

    private void selectActiveMap() {
        Point active = handler.getMapSelected();
        for (int i = 0; i < mapModel.size(); i++) {
            if (mapModel.get(i).equals(active)) {
                mapList.setSelectedIndex(i);
                mapList.ensureIndexIsVisible(i);
                return;
            }
        }
        mapList.clearSelection();
    }

    private ReplaceRemapRequest createRequest() {
        if (mappingTable.isEditing()) {
            mappingTable.getCellEditor().stopCellEditing();
        }
        LinkedHashMap<Integer, Integer> replacements = new LinkedHashMap<>();
        for (MappingRow row : mappingModel.rows) {
            if (replacements.put(row.source, row.replacement) != null) {
                throw new IllegalArgumentException("Source tile " + row.source
                        + " appears more than once.");
            }
        }
        LinkedHashSet<Point> maps = new LinkedHashSet<>();
        for (Point point : mapList.getSelectedValuesList()) {
            maps.add(new Point(point));
        }
        LinkedHashSet<Integer> layers = new LinkedHashSet<>(layerList.getSelectedValuesList());
        return new ReplaceRemapRequest(replacements, maps, layers,
                replaceTiles.isSelected(), remapPermissions.isSelected(), remapTypes.isSelected(),
                handler.getTileset().size(),
                ReplaceRemapRequest.Capabilities.forGame(handler.getGameIndex()));
    }

    private void startPreview() {
        ReplaceRemapRequest request;
        try {
            request = createRequest();
        } catch (IllegalArgumentException exception) {
            showDiagnostics(Collections.singletonList(exception.getMessage()), Collections.emptyList());
            return;
        }
        setWorking(true, "Cancel");
        worker = new SwingWorker<ReplaceRemapPlan, Void>() {
            @Override
            protected ReplaceRemapPlan doInBackground() {
                RemapProjectSnapshot snapshot = access.captureSnapshot(request);
                return new ReplaceRemapPlanner().plan(request, snapshot,
                        access.getMapCoordinates(), access.getTerrainLayerCount(), this::isCancelled);
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        currentPlan = get();
                        previewModel.setRows(currentPlan.getSummaries());
                        showDiagnostics(currentPlan.getValidationProblems(),
                                currentPlan.getSkippedEntries());
                        applyButton.setEnabled(currentPlan.canApply());
                    }
                } catch (CancellationException exception) {
                    diagnostics.setText("Preview cancelled.");
                } catch (Exception exception) {
                    diagnostics.setText("Preview failed: " + rootMessage(exception));
                } finally {
                    worker = null;
                    setWorking(false, "Close");
                }
            }
        };
        worker.execute();
    }

    private void startApply() {
        ReplaceRemapPlan plan = currentPlan;
        if (plan == null || !plan.canApply()) {
            return;
        }
        setWorking(true, "Cancel");
        AtomicReference<ReplaceRemapState> committedState = new AtomicReference<>();
        worker = new SwingWorker<ReplaceRemapState, Void>() {
            @Override
            protected ReplaceRemapState doInBackground() {
                ReplaceRemapState before = ReplaceRemapExecutor.apply(
                        plan, access, this::isCancelled);
                committedState.set(before);
                return before;
            }

            @Override
            protected void done() {
                try {
                    ReplaceRemapState before = committedState.get();
                    if (before != null) {
                        handler.addMapState(before);
                        refresh(plan);
                        dispose();
                    } else if (!isCancelled()) {
                        get();
                    } else {
                        diagnostics.setText("Apply cancelled; no changes were committed.");
                    }
                } catch (CancellationException exception) {
                    diagnostics.setText("Apply cancelled; no changes were committed.");
                } catch (Exception exception) {
                    diagnostics.setText("Apply failed and was rolled back: " + rootMessage(exception));
                    currentPlan = null;
                    applyButton.setEnabled(false);
                } finally {
                    worker = null;
                    setWorking(false, "Close");
                }
            }
        };
        worker.execute();
    }

    private void refresh(ReplaceRemapPlan plan) {
        for (Point point : plan.getAffectedMaps()) {
            MapData map = handler.getMapMatrix().getMap(point);
            if (map == null) {
                continue;
            }
            for (Integer layer : plan.getAffectedTerrainLayers()) {
                map.getGrid().updateMapLayerGL(layer, handler.useRealTimePostProcessing());
            }
            map.updateMapThumbnail();
        }
        handler.getMainFrame().getThumbnailLayerSelector().drawAllLayerThumbnails();
        handler.getMainFrame().getThumbnailLayerSelector().repaint();
        handler.getMainFrame().getMapDisplay().repaint();
        handler.getMainFrame().updateMapMatrixDisplay();
        handler.getMainFrame().updateViewMapInfo();
    }

    private void closeOrCancel() {
        if (worker != null) {
            worker.cancel(true);
        } else {
            dispose();
        }
    }

    private void setWorking(boolean working, String cancelText) {
        progress.setVisible(working);
        previewButton.setEnabled(!working);
        applyButton.setEnabled(!working && currentPlan != null && currentPlan.canApply());
        mappingTable.setEnabled(!working);
        scope.setEnabled(!working);
        mapList.setEnabled(!working && scope.getSelectedIndex() == SCOPE_SELECTED);
        layerList.setEnabled(!working);
        replaceTiles.setEnabled(!working);
        ReplaceRemapRequest.Capabilities capabilities =
                ReplaceRemapRequest.Capabilities.forGame(handler.getGameIndex());
        remapPermissions.setEnabled(!working && capabilities.canRemapPermissions());
        remapTypes.setEnabled(!working && capabilities.canRemapTypes());
        cancelButton.setText(cancelText);
    }

    private void invalidatePreview() {
        if (worker != null) {
            return;
        }
        currentPlan = null;
        applyButton.setEnabled(false);
        previewModel.setRows(Collections.emptyList());
        diagnostics.setText("Preview required before Apply.");
    }

    private void showDiagnostics(List<String> problems, List<String> skipped) {
        StringBuilder text = new StringBuilder();
        for (String problem : problems) {
            text.append("Problem: ").append(problem).append('\n');
        }
        for (String item : skipped) {
            text.append("Skipped: ").append(item).append('\n');
        }
        if (text.length() == 0) {
            text.append("Ready to apply.");
        }
        diagnostics.setText(text.toString().trim());
        diagnostics.setCaretPosition(0);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static final class MappingRow {
        private int source;
        private int replacement;

        private MappingRow(int source, int replacement) {
            this.source = source;
            this.replacement = replacement;
        }
    }

    private static final class MappingTableModel extends AbstractTableModel {
        private final List<MappingRow> rows = new ArrayList<>();

        private void add(int source, int replacement) {
            rows.add(new MappingRow(source, replacement));
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        private void remove(int row) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Source tile ID" : "Replacement tile ID";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MappingRow row = rows.get(rowIndex);
            return columnIndex == 0 ? row.source : row.replacement;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            int number = ((Number) value).intValue();
            if (columnIndex == 0) {
                rows.get(rowIndex).source = number;
            } else {
                rows.get(rowIndex).replacement = number;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static final class PreviewTableModel extends AbstractTableModel {
        private final String[] columns = {"Map", "Coordinates", "Surface", "Matches", "Changes", "Skipped"};
        private List<ReplaceRemapPlan.SummaryRow> rows = Collections.emptyList();

        private void setRows(List<ReplaceRemapPlan.SummaryRow> rows) {
            this.rows = new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ReplaceRemapPlan.SummaryRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.getMapName();
                case 1:
                    return ReplaceRemapRequest.formatPoint(row.getMapCoordinates());
                case 2:
                    return row.getSurface();
                case 3:
                    return row.getMatches();
                case 4:
                    return row.getChanges();
                default:
                    return row.getSkipped();
            }
        }
    }

    private static final class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;

        private SpinnerCellEditor(int maximum) {
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, maximum, 1));
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean selected, int row, int column) {
            spinner.setValue(value);
            return spinner;
        }
    }
}
