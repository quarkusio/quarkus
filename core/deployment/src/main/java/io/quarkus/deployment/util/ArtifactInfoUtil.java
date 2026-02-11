package io.quarkus.deployment.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ResolvedDependency;

public final class ArtifactInfoUtil {

    public static final String DEPLOYMENT = "-deployment";

    /**
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @return the result, or throws
     * @deprecated use {@link #groupIdAndArtifactId(Class, CurateOutcomeBuildItem)}
     */
    @Deprecated
    public static Map.Entry<String, String> groupIdAndArtifactId(Class<?> clazz) {
        return groupIdAndArtifactId(clazz, null);
    }

    /**
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @param clazz the caller clazz. (must not be {@code null})
     * @param curateOutcomeBuildItem the application model that gets searched for the caller clazz. optional
     * @return the result, or throws
     */
    public static Map.Entry<String, String> groupIdAndArtifactId(Class<?> clazz,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        try {
            URL codeLocation = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (curateOutcomeBuildItem != null) {
                Path path = Paths.get(codeLocation.toURI());
                String pathAsString = path.toString();
                // Workspace artifacts paths are resolved to the maven module, not the jar file
                // If the clazz is inside a jar, but does not contain the artifact name, we have to open
                // the jar to read the pom properties, which gets done as next attempt.
                // This also acts as a fast path to prevent expensive path comparisons.
                boolean isJar = pathAsString.endsWith(".jar");
                // Check if this is a target/classes directory (workspace module loaded from classes)
                boolean isTargetClasses = !isJar
                        && pathAsString.endsWith("target" + path.getFileSystem().getSeparator() + "classes");
                for (ResolvedDependency i : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
                    if (isJar && !pathAsString.contains(i.getArtifactId())) {
                        continue;
                    }
                    for (Path p : i.getResolvedPaths()) {
                        if (path.equals(p)) {
                            String artifactId = i.getArtifactId();
                            if (artifactId.endsWith(DEPLOYMENT)) {
                                artifactId = artifactId.substring(0, artifactId.length() - DEPLOYMENT.length());
                            }
                            return new AbstractMap.SimpleEntry<>(i.getGroupId(), artifactId);
                        }
                        // Handle case where class is loaded from target/classes but dependency points to target/*.jar
                        // This happens in multi-module projects where workspace modules are resolved to their JAR files
                        if (isTargetClasses && p.toString().endsWith(".jar")) {
                            // Check if the JAR is in the same target directory as our classes
                            // path: .../module/target/classes
                            // p:    .../module/target/artifact-version.jar
                            Path classesTargetDir = path.getParent(); // .../module/target
                            Path jarParentDir = p.getParent(); // .../module/target (if JAR is in target dir)
                            if (classesTargetDir != null && classesTargetDir.equals(jarParentDir)) {
                                String artifactId = i.getArtifactId();
                                if (artifactId.endsWith(DEPLOYMENT)) {
                                    artifactId = artifactId.substring(0, artifactId.length() - DEPLOYMENT.length());
                                }
                                return new AbstractMap.SimpleEntry<>(i.getGroupId(), artifactId);
                            }
                        }
                    }
                }
            }

            if (codeLocation.toString().endsWith(".jar")) {
                // Search inside the jar for pom properties, needed for workspace artifacts
                try (FileSystem fs = ZipUtils.newFileSystem(Paths.get(codeLocation.toURI()),
                        Thread.currentThread().getContextClassLoader())) {
                    Entry<String, String> ret = groupIdAndArtifactId(fs);
                    if (ret == null) {
                        throw new RuntimeException("Unable to determine groupId and artifactId of the jar that contains "
                                + clazz.getName() + " because the jar doesn't contain the necessary metadata");
                    }
                    return ret;
                }
            } else if ("file".equals(codeLocation.getProtocol())) {
                // E.g. /quarkus/extensions/arc/deployment/target/classes/io/quarkus/arc/deployment/devconsole
                // This can happen if you run an example app in dev mode
                // and this app is part of a multi-module project which also declares the extension
                // Just try to locate the pom.properties file in the target/maven-archiver directory
                // Note that this hack will not work if addMavenDescriptor=false or if the pomPropertiesFile is overridden
                Path location = Paths.get(codeLocation.toURI());
                while (!isTargetClasses(location) && location.getParent() != null) {
                    location = location.getParent();
                }
                if (isTargetClasses(location)) {
                    Path mavenArchiver = location.getParent().resolve("maven-archiver");
                    if (mavenArchiver.toFile().canRead()) {
                        Entry<String, String> ret = groupIdAndArtifactId(mavenArchiver);
                        if (ret == null) {
                            throw new RuntimeException(
                                    "Unable to determine groupId and artifactId of the extension that contains "
                                            + clazz.getName()
                                            + " because the directory doesn't contain the necessary metadata");
                        }
                        return ret;
                    }
                }
                return new AbstractMap.SimpleEntry<>("unspecified", "unspecified");
            } else {
                return new AbstractMap.SimpleEntry<>("unspecified", "unspecified");
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Unable to determine groupId and artifactId of the jar that contains " + clazz.getName(),
                    e);
        }
    }

    /**
     * Checks if the given path represents a Maven target/classes directory.
     * This works for any module type (deployment, runtime, spi, etc.).
     */
    static boolean isTargetClasses(Path location) {
        if (location == null || location.getFileName() == null) {
            return false;
        }
        if (!location.getFileName().toString().equals("classes")) {
            return false;
        }
        Path target = location.getParent();
        return !(target == null || target.getFileName() == null || !target.getFileName().toString().equals("target"));
    }

    /**
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @return the result, or null if no maven metadata were found
     */
    public static Map.Entry<String, String> groupIdAndArtifactId(FileSystem fs) throws IOException {
        Path metaInfPath = fs.getPath("/META-INF");
        return groupIdAndArtifactId(metaInfPath);
    }

    public static AbstractMap.SimpleEntry<String, String> groupIdAndArtifactId(Path pomPropertiesContainer) throws IOException {
        Optional<Path> pomProperties = Files.walk(pomPropertiesContainer)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("pom.properties"))
                .findFirst();
        if (pomProperties.isPresent()) {
            Properties props = new Properties();
            props.load(Files.newInputStream(pomProperties.get()));
            String artifactId = props.getProperty("artifactId");
            if (artifactId.endsWith(DEPLOYMENT)) {
                artifactId = artifactId.substring(0, artifactId.length() - DEPLOYMENT.length());
            }
            return new AbstractMap.SimpleEntry<>(props.getProperty("groupId"), artifactId);
        } else {
            return null;
        }
    }
}
