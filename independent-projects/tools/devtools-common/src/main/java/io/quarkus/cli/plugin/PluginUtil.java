package io.quarkus.cli.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.dependency.GACTV;

public final class PluginUtil {

    private PluginUtil() {
        //Utility
    }

    public static boolean shouldSync(Path projectRoot, PluginCatalog catalog) {
        return shouldSync(Optional.ofNullable(projectRoot), catalog);
    }

    public static boolean shouldSync(Optional<Path> projectRoot, PluginCatalog catalog) {
        LocalDateTime catalogTime = catalog.getLastUpdateDate();
        LocalDateTime lastBuildFileModifiedTime = getLastBuildFileModifiedTime(projectRoot);
        return catalogTime.isBefore(lastBuildFileModifiedTime) || LocalDateTime.now().minusDays(7).isAfter(catalogTime);
    }

    /**
     * Get the {@link PluginType} that corresponds the the specified location.
     *
     * @param the location
     * @return the {@link PluginType} that corresponds to the location.
     */
    public static PluginType getType(String location) {
        Optional<URL> url = checkUrl(location);
        Optional<Path> path = checkPath(location);
        Optional<GACTV> gactv = checkGACTV(location);
        return getType(gactv, url, path);
    }

    /**
     * Get the {@link PluginType} that corresponds the the specified locations.
     *
     * @param url the url
     * @param path the path
     * @param gactv the gactv
     * @return the {@link PluginType} that corresponds to the location.
     */
    public static PluginType getType(Optional<GACTV> gactv, Optional<URL> url, Optional<Path> path) {

        return gactv.map(i -> PluginType.maven)
                .or(() -> url.map(u -> u.getPath()).or(() -> path.map(Path::toAbsolutePath).map(Path::toString))
                        .filter(f -> f.endsWith(".jar") || f.endsWith(".java")) // java or jar files
                        .map(f -> f.substring(f.lastIndexOf(".") + 1)) // get extension
                        .map(PluginType::valueOf)) // map to type
                .or(() -> path.filter(p -> p.toFile().exists()).map(i -> PluginType.executable))
                .orElse(PluginType.jbang);
    }

    /**
     * Check if the plugin can be found.
     * The method is used to determined the plugin can be located.
     *
     * @return true if path is not null and points to an existing file.
     */
    public static boolean shouldRemove(Plugin p) {
        if (p == null) {
            return true;
        }
        if (!p.getLocation().isPresent()) {
            return true;
        }
        if (p.getType() == PluginType.executable) {
            return !checkPath(p.getLocation()).map(Path::toFile).map(File::exists).orElse(false);
        }
        if (checkUrl(p.getLocation()).isPresent()) { //We don't want to remove remotely located plugins
            return false;
        }
        if (checkGACTV(p.getLocation()).isPresent() && p.getType() != PluginType.extension) { //We don't want to remove remotely located plugins
            return false;
        }
        if (p.getLocation().map(PluginUtil::isLocalFile).orElse(false)) {
            return false;
        }
        return true;
    }

    /**
     * Chekcs if specified {@link String} is a valid {@URL}.
     *
     * @param location The string to check
     * @return The {@link URL} wrapped in {@link Optional} if valid, empty otherwise.
     */
    public static Optional<URL> checkUrl(String location) {
        try {
            return Optional.of(new URL(location));
        } catch (MalformedURLException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<URL> checkUrl(Optional<String> location) {
        return location.flatMap(PluginUtil::checkUrl);
    }

    /**
     * Chekcs if specified {@link String} is a valid {@URL}.
     *
     * @param location The string to check
     * @return The {@link URL} wrapped in {@link Optional} if valid, empty otherwise.
     */
    public static Optional<GACTV> checkGACTV(String location) {
        try {
            return Optional.of(GACTV.fromString(location));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<GACTV> checkGACTV(Optional<String> location) {
        return location.flatMap(PluginUtil::checkGACTV);
    }

    /**
     * Chekcs if specified {@link String} is a valid path.
     *
     * @param location The string to check
     * @return The {@link Path} wrapped in {@link Optional} if valid, empty otherwise.
     */
    public static Optional<Path> checkPath(String location) {
        try {
            return Optional.of(Paths.get(location));
        } catch (InvalidPathException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<Path> checkPath(Optional<String> location) {
        return location.flatMap(PluginUtil::checkPath);
    }

    /**
     * Chekcs if specified {@link String} contains a valid remote catalog
     *
     * @param location The string to check
     * @return The catalog wrapped in {@link Optional} if valid, empty otherwise.
     */
    public static Optional<String> checkRemoteCatalog(String location) {
        return Optional.ofNullable(location)
                .filter(l -> l.contains("@"))
                .map(l -> l.substring(l.lastIndexOf("@") + 1))
                .filter(l -> !l.isEmpty());
    }

    public static Optional<String> checkRemoteCatalog(Optional<String> location) {
        return location.flatMap(PluginUtil::checkRemoteCatalog);
    }

    /**
     * Checks if location is remote.
     *
     * @param location the specifiied location.
     * @return true if location is url or gactv
     */
    public static boolean isRemoteLocation(String location) {
        return checkUrl(location).isPresent() || checkGACTV(location).isPresent() || checkRemoteCatalog(location).isPresent();
    }

    /**
     * Checks if location is a file that does exists locally.
     *
     * @param location the specifiied location.
     * @return true if location is url or gactv
     */
    public static boolean isLocalFile(String location) {
        return checkPath(location).map(p -> p.toFile().exists()).orElse(false);
    }

    /**
     * Checks if location is a file that does exists under the project root.
     *
     * @param projectRoot the root of the project.
     * @param location the specifiied location.
     * @return true if location is url or gactv
     */
    public static boolean isProjectFile(Path projectRoot, String location) {
        return checkPath(location)
                .map(Path::normalize)
                .map(Path::toFile)
                .filter(f -> f.getAbsolutePath().startsWith(projectRoot.normalize().toAbsolutePath().toString()))
                .map(File::exists)
                .orElse(false);
    }

    private static List<Path> getBuildFiles(Optional<Path> projectRoot) {
        List<Path> buildFiles = new ArrayList<>();
        if (projectRoot == null) {
            return buildFiles;
        }
        projectRoot.ifPresent(root -> {
            BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(root);
            if (buildTool != null) {
                for (String buildFile : buildTool.getBuildFiles()) {
                    buildFiles.add(root.resolve(buildFile));
                }
            }
        });
        return buildFiles;
    }

    private static LocalDateTime getLastBuildFileModifiedTime(Optional<Path> projectRoot) {
        long lastModifiedMillis = getBuildFiles(projectRoot).stream().map(Path::toFile).filter(File::exists)
                .map(File::lastModified)
                .max(Long::compare).orElse(0L);
        return Instant.ofEpochMilli(lastModifiedMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
