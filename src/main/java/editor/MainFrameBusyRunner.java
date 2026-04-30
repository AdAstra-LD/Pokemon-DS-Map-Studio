package editor;

import editor.mapdisplay.MapDisplay;
import editor.mapmatrix.MapMatrixDisplay;
import editor.tileselector.TileSelector;
import editor.tileseteditor.TileDisplay;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class MainFrameBusyRunner {
    private final JFrame frame;
    private final JSplitPane mainWindowSplitPane;
    private final JSplitPane matrixSplitPane;
    private final MapDisplay mapDisplay;
    private final JComponent mapDisplayContainer;
    private final TileDisplay tileDisplay;
    private final TileSelector tileSelector;
    private final MapMatrixDisplay mapMatrixDisplay;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private String statusBackup = "";
    private Timer loadingTimer;
    private int loadingDots = 0;

    MainFrameBusyRunner(MainFrameContext context) {
        this.frame = context.frame;
        this.mainWindowSplitPane = context.mainWindowSplitPane;
        this.matrixSplitPane = context.matrixSplitPane;
        this.mapDisplay = context.mapDisplay;
        this.mapDisplayContainer = context.mapDisplayContainer;
        this.tileDisplay = context.tileDisplay;
        this.tileSelector = context.tileSelector;
        this.mapMatrixDisplay = context.mapMatrixDisplay;
        this.statusLabel = context.statusLabel;
        this.progressBar = createProgressBar(context.statusBarPanel);
    }

    void setGUIBlock(boolean status) {
        System.out.println("Gui block status changed: " + status);
        mapDisplay.setMouseWheelEnabled(!status);
        frame.setEnabled(!status);
        mainWindowSplitPane.setEnabled(!status);
        matrixSplitPane.setEnabled(!status);
        mapDisplay.setEnabled(!status);
        mapDisplayContainer.setEnabled(!status);
        tileDisplay.setEnabled(!status);
        tileSelector.setEnabled(!status);
        mapMatrixDisplay.setEnabled(!status);
    }

    BusyTask startLoading() {
        statusBackup = statusLabel.getText();
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(false);
            progressBar.setVisible(true);
            statusLabel.setText("Loading");
        });

        loadingDots = 0;
        loadingTimer = new Timer(350, e -> {
            loadingDots = (loadingDots + 1) % 4;
            statusLabel.setText("Loading" + ".".repeat(loadingDots));
        });
        loadingTimer.start();
        return new BusyTask(false);
    }

    BusyTask startProgress(String message, int maximum) {
        statusBackup = statusLabel.getText();
        SwingUtilities.invokeLater(() -> {
            stopLoadingTimer();
            progressBar.setIndeterminate(false);
            progressBar.setMinimum(0);
            progressBar.setMaximum(Math.max(maximum, 1));
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setString("0 / " + Math.max(maximum, 1));
            progressBar.setVisible(true);
            statusLabel.setText(message);
        });
        return new BusyTask(true);
    }

    private JProgressBar createProgressBar(JPanel statusBarPanel) {
        JProgressBar bar = new JProgressBar();
        bar.setPreferredSize(new Dimension(140, 14));
        bar.setVisible(false);
        statusBarPanel.add(bar);
        statusBarPanel.revalidate();
        return bar;
    }

    private void stopLoadingTimer() {
        if (loadingTimer != null) {
            loadingTimer.stop();
            loadingTimer = null;
        }
    }

    final class BusyTask implements AutoCloseable {
        private final boolean determinate;
        private boolean finished;

        private BusyTask(boolean determinate) {
            this.determinate = determinate;
        }

        void setMessage(String message) {
            if (determinate) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(message));
            }
        }

        void setProgress(int value) {
            if (determinate) {
                SwingUtilities.invokeLater(() -> {
                    int clampedValue = Math.max(progressBar.getMinimum(), Math.min(value, progressBar.getMaximum()));
                    progressBar.setValue(clampedValue);
                    progressBar.setString(clampedValue + " / " + progressBar.getMaximum());
                });
            }
        }

        void increment() {
            if (determinate) {
                SwingUtilities.invokeLater(() -> {
                    int value = Math.min(progressBar.getValue() + 1, progressBar.getMaximum());
                    progressBar.setValue(value);
                    progressBar.setString(value + " / " + progressBar.getMaximum());
                });
            }
        }

        void finish() {
            if (!finished) {
                finished = true;
                SwingUtilities.invokeLater(() -> {
                    stopLoadingTimer();
                    progressBar.setVisible(false);
                    progressBar.setStringPainted(false);
                    progressBar.setIndeterminate(false);
                    statusLabel.setText(statusBackup);
                });
            }
        }

        @Override
        public void close() {
            finish();
        }
    }
}
