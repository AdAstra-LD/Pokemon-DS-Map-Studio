package editor.converter;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

/** Locates the external Nintendo g3dcvtr tool independently of launch location. */
public final class ConverterLocator {

    private static final String EXECUTABLE = "g3dcvtr.exe";

    private ConverterLocator() {
    }

    public static String getConverterPath() {
        for (Path candidate : getCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        return Paths.get("converter", EXECUTABLE).toAbsolutePath().normalize().toString();
    }

    private static LinkedHashSet<Path> getCandidates() {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        addCandidateAndAncestors(candidates, Paths.get(System.getProperty("user.dir", ".")));
        try {
            URI location = ConverterLocator.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path appPath = Paths.get(location).toAbsolutePath().normalize();
            addCandidateAndAncestors(candidates,
                    Files.isDirectory(appPath) ? appPath : appPath.getParent());
        } catch (Exception ignored) {
        }
        return candidates;
    }

    private static void addCandidateAndAncestors(LinkedHashSet<Path> candidates, Path start) {
        Path current = start == null ? null : start.toAbsolutePath().normalize();
        for (int depth = 0; current != null && depth < 6; depth++) {
            candidates.add(current.resolve("converter").resolve(EXECUTABLE));
            current = current.getParent();
        }
    }
}
