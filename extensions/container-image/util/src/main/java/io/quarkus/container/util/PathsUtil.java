package io.quarkus.container.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;

public final class PathsUtil {

    private PathsUtil() {
    }

    /**
     * Return a Map.Entry (which is used as a Tuple) containing the main sources root as the key
     * and the project root as the value
     */
    public static AbstractMap.SimpleEntry<Path, Path> findMainSourcesRoot(Path outputDirectory) {
        Path currentPath = outputDirectory;
        do {
            Path toCheck = currentPath.resolve(Paths.get("src", "main"));
            if (toCheck.toFile().exists()) {
                return new AbstractMap.SimpleEntry<>(toCheck, currentPath);
            }
            if (currentPath.getParent() != null && Files.exists(currentPath.getParent())) {
                currentPath = currentPath.getParent();
            } else {
                return null;
            }
        } while (true);
    }
}
