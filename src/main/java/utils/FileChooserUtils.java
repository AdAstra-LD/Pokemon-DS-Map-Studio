package utils;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class FileChooserUtils {

    /**
     * Ouvre un sélecteur de dossier JavaFX avec interface native
     *
     * @param title Titre de la fenêtre
     * @param initialDirectory Dossier initial (peut être null)
     * @param onDirectorySelected Callback appelé avec le dossier sélectionné (peut être null si annulé)
     */
    public static void selectDirectory(String title, File initialDirectory, Consumer<File> onDirectorySelected) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.exists() && initialDirectory.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        Platform.runLater(() -> {
            File selectedDirectory = directoryChooser.showDialog(null);

            // Retour vers le thread Swing avec le résultat
            SwingUtilities.invokeLater(() -> {
                onDirectorySelected.accept(selectedDirectory);
            });
        });
    }

    /**
     * Version simplifiée avec juste le titre
     */
    public static void selectDirectory(String title, Consumer<File> onDirectorySelected) {
        selectDirectory(title, null, onDirectorySelected);
    }

    public static void selectFile(String title, File initialDirectory, Consumer<File> onFileSelected) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        Platform.runLater(() -> {
            File selectedFile = fileChooser.showOpenDialog(null);

            SwingUtilities.invokeLater(() -> {
                onFileSelected.accept(selectedFile);
            });
        });
    }

    public static void selectFile(String title, File initialDirectory,
                                  String filterDescription, String[] extensions,
                                  Consumer<File> onFileSelected) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        if (extensions != null && extensions.length > 0) {
            FileChooser.ExtensionFilter filter =
                    new FileChooser.ExtensionFilter(filterDescription, extensions);
            fileChooser.getExtensionFilters().add(filter);
        }

        Platform.runLater(() -> {
            File selectedFile = fileChooser.showOpenDialog(null);

            SwingUtilities.invokeLater(() -> {
                onFileSelected.accept(selectedFile);
            });
        });
    }

    public static void saveFile(String title, File initialDirectory,
                                String filterDescription, String[] extensions,
                                Consumer<File> onFileSaved) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        if (extensions != null && extensions.length > 0) {
            FileChooser.ExtensionFilter filter =
                    new FileChooser.ExtensionFilter(filterDescription, extensions);
            fileChooser.getExtensionFilters().add(filter);
        }

        Platform.runLater(() -> {
            File selectedFile = fileChooser.showSaveDialog(null);

            SwingUtilities.invokeLater(() -> {
                onFileSaved.accept(selectedFile);
            });
        });
    }

    public static void selectFileOrDirectory(String title, File initialDirectory,
                                             String filterDescription, String[] extensions,
                                             Consumer<File> onSelected) {
        Platform.runLater(() -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(title + " (Directory)");

            if (initialDirectory != null && initialDirectory.exists()) {
                directoryChooser.setInitialDirectory(initialDirectory);
            }

            File selectedDirectory = directoryChooser.showDialog(null);

            if (selectedDirectory == null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle(title + " (File)");

                if (initialDirectory != null && initialDirectory.exists()) {
                    fileChooser.setInitialDirectory(initialDirectory);
                }

                if (extensions != null && extensions.length > 0) {
                    FileChooser.ExtensionFilter filter =
                            new FileChooser.ExtensionFilter(filterDescription, extensions);
                    fileChooser.getExtensionFilters().add(filter);
                }

                File selectedFile = fileChooser.showOpenDialog(null);

                SwingUtilities.invokeLater(() -> {
                    onSelected.accept(selectedFile);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    onSelected.accept(selectedDirectory);
                });
            }
        });
    }

    public static void selectMultipleFiles(String title, File initialDirectory,
                                           String filterDescription, String[] extensions,
                                           Consumer<List<File>> onFilesSelected) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        if (extensions != null && extensions.length > 0) {
            FileChooser.ExtensionFilter filter =
                    new FileChooser.ExtensionFilter(filterDescription, extensions);
            fileChooser.getExtensionFilters().add(filter);
        }

        Platform.runLater(() -> {
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

            SwingUtilities.invokeLater(() -> {
                onFilesSelected.accept(selectedFiles);
            });
        });
    }
}