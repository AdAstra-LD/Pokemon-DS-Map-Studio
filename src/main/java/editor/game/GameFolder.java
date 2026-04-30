package editor.game;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameFolder {

    public enum Layout {
        NDSTOOL,
        DSROM
    }

    private static final String LEGACY_HEADER_PATH = "header.bin";
    private static final String DSROM_HEADER_PATH = "header.yaml";
    private static final Pattern YAML_GAME_CODE_PATTERN = Pattern.compile(
            "^\\s*(game[_-]?code|game[_-]?id|id[_-]?code|rom[_-]?code)\\s*:\\s*['\"]?([A-Za-z0-9]{4})['\"]?.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_OVERLAY_PATTERN = Pattern.compile("^overlay/overlay_(\\d+)\\.bin$");
    private static final Pattern DSROM_OVERLAY_PATTERN = Pattern.compile("^arm9_overlays/ov(\\d+)\\.bin$");

    private final File rootFolder;
    private final Layout layout;

    public GameFolder(String rootFolderPath) {
        this.rootFolder = new File(rootFolderPath);
        this.layout = new File(rootFolder, DSROM_HEADER_PATH).isFile() ? Layout.DSROM : Layout.NDSTOOL;
    }

    public Layout getLayout() {
        return layout;
    }

    public boolean isDsromLayout() {
        return layout == Layout.DSROM;
    }

    public String getRootFolderPath() {
        return rootFolder.getPath();
    }

    public File getFile(String relativePath) {
        return new File(rootFolder, resolveRelativePath(relativePath));
    }

    public String getPath(String relativePath) {
        return getFile(relativePath).getPath();
    }

    public boolean exists(String relativePath) {
        return getFile(relativePath).exists();
    }

    public String resolveRelativePath(String relativePath) {
        String normalizedPath = normalizePath(relativePath);
        String resolvedPath = layout == Layout.DSROM
                ? toDsromRelativePath(normalizedPath)
                : toLegacyRelativePath(normalizedPath);
        return toPlatformPath(resolvedPath);
    }

    public String readGameCode() throws IOException {
        if (layout == Layout.DSROM) {
            return readGameCodeFromHeaderYaml();
        }
        return readGameCodeFromHeaderBin();
    }

    public boolean hasGameCode(String gameCode) throws IOException {
        if (layout == Layout.DSROM) {
            String headerText = readHeaderYamlText();
            String yamlGameCode = parseGameCodeFromYamlText(headerText);
            if (yamlGameCode != null) {
                return yamlGameCode.equalsIgnoreCase(gameCode);
            }
            return headerText.toUpperCase().contains(gameCode.toUpperCase());
        }
        return readGameCodeFromHeaderBin().equals(gameCode);
    }

    private String readGameCodeFromHeaderBin() throws IOException {
        byte[] headerData = Files.readAllBytes(getFile(LEGACY_HEADER_PATH).toPath());
        if (headerData.length < 16) {
            throw new IOException("Invalid header.bin");
        }
        return new String(headerData, 12, 4, StandardCharsets.US_ASCII);
    }

    private String readGameCodeFromHeaderYaml() throws IOException {
        String gameCode = parseGameCodeFromYamlText(readHeaderYamlText());
        if (gameCode == null) {
            throw new IOException("Unable to find the game code in header.yaml");
        }
        return gameCode;
    }

    private String readHeaderYamlText() throws IOException {
        return new String(Files.readAllBytes(getFile(DSROM_HEADER_PATH).toPath()), StandardCharsets.UTF_8);
    }

    private static String parseGameCodeFromYamlText(String yamlText) {
        String[] lines = yamlText.split("\\R");
        for (String line : lines) {
            Matcher matcher = YAML_GAME_CODE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private static String toDsromRelativePath(String path) {
        if (path.equals("data")) {
            return "files";
        } else if (path.startsWith("data/")) {
            return "files/" + path.substring("data/".length());
        } else if (path.equals("arm9.bin")) {
            return "arm9/arm9.bin";
        } else if (path.equals("arm7.bin")) {
            return "arm7/arm7.bin";
        } else if (path.equals("overlay")) {
            return "arm9_overlays";
        } else if (path.startsWith("overlay/")) {
            return toDsromOverlayPath(path);
        } else if (path.equals("y9.bin")) {
            return "arm9_overlays/overlays.yaml";
        } else if (path.equals("y7.bin")) {
            return "arm7_overlays/overlays.yaml";
        } else if (path.equals("header.bin")) {
            return DSROM_HEADER_PATH;
        } else if (path.equals("banner.bin")) {
            return "banner/banner.yaml";
        }
        return path;
    }

    private static String toLegacyRelativePath(String path) {
        if (path.equals("files")) {
            return "data";
        } else if (path.startsWith("files/")) {
            return "data/" + path.substring("files/".length());
        } else if (path.equals("arm9/arm9.bin")) {
            return "arm9.bin";
        } else if (path.equals("arm7/arm7.bin")) {
            return "arm7.bin";
        } else if (path.equals("arm9_overlays/overlays.yaml")) {
            return "y9.bin";
        } else if (path.equals("arm7_overlays/overlays.yaml")) {
            return "y7.bin";
        } else if (path.equals("arm9_overlays")) {
            return "overlay";
        } else if (path.startsWith("arm9_overlays/")) {
            return toLegacyOverlayPath(path);
        } else if (path.equals(DSROM_HEADER_PATH)) {
            return LEGACY_HEADER_PATH;
        } else if (path.equals("banner/banner.yaml")) {
            return "banner.bin";
        }
        return path;
    }

    private static String toDsromOverlayPath(String path) {
        Matcher matcher = LEGACY_OVERLAY_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return path;
        }
        return "arm9_overlays/ov" + formatOverlayNumber(matcher.group(1), 3) + ".bin";
    }

    private static String toLegacyOverlayPath(String path) {
        Matcher matcher = DSROM_OVERLAY_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return path;
        }
        return "overlay/overlay_" + formatOverlayNumber(matcher.group(1), 4) + ".bin";
    }

    private static String formatOverlayNumber(String number, int minimumDigits) {
        try {
            return String.format("%0" + minimumDigits + "d", Integer.parseInt(number));
        } catch (NumberFormatException ex) {
            return number;
        }
    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private static String toPlatformPath(String path) {
        return path.replace('/', File.separatorChar);
    }

}
