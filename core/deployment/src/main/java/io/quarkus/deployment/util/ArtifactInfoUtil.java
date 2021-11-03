package io.quarkus.deployment.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

public final class ArtifactInfoUtil {

    public static final String DEPLOYMENT = "-deployment";

    /**
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @return the result, or throws
     */
    public static Map.Entry<String, String> groupIdAndArtifactId(Class<?> clazz) {
        return groupIdAndArtifactId(clazz, null);
    }

    /**
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @return the result, or throws
     */
    public static Map.Entry<String, String> groupIdAndArtifactId(Class<?> clazz,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        try {
            URL codeLocation = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (codeLocation.toString().endsWith(".jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(Paths.get(codeLocation.toURI()),
                        Thread.currentThread().getContextClassLoader())) {
                    Entry<String, String> ret = groupIdAndArtifactId(fs);
                    if (ret == null) {
                        throw new RuntimeException("Unable to determine groupId and artifactId of the jar that contains "
                                + clazz.getName() + " because the jar doesn't contain the necessary metadata");
                    }
                    return ret;
                }
            } else if (curateOutcomeBuildItem != null) {
                // this is needed only for QuarkusDevModeTest inside Quarkus where the class is read from the corresponding directory
                Path path = Paths.get(codeLocation.toURI());
                for (ResolvedDependency i : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
                    for (Path p : i.getResolvedPaths()) {
                        if (path.equals(p)) {

                            String artifactId = i.getArtifactId();
                            if (artifactId.endsWith(DEPLOYMENT)) {
                                artifactId = artifactId.substring(0, artifactId.length() - DEPLOYMENT.length());
                            }
                            return new AbstractMap.SimpleEntry<>(i.getGroupId(), artifactId);
                        }
                    }
                }
                return new AbstractMap.SimpleEntry<>("unspecified", "unspecified");
            } else if ("file".equals(codeLocation.getProtocol())) {
                // E.g. /quarkus/extensions/arc/deployment/target/classes/io/quarkus/arc/deployment/devconsole
                // This can happen if you run an example app in dev mode
                // and this app is part of a multi-module project which also declares the extension
                // Just try to locate the pom.properties file in the target/maven-archiver directory
                // Note that this hack will not work if addMavenDescriptor=false or if the pomPropertiesFile is overriden
                Path location = Paths.get(codeLocation.toURI());
                while (!isDeploymentTargetClasses(location) && location.getParent() != null) {
                    location = location.getParent();
                }
                if (location != null) {
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

    static boolean isDeploymentTargetClasses(Path location) {
        if (!location.getFileName().toString().equals("classes")) {
            return false;
        }
        Path target = location.getParent();
        if (target == null || !target.getFileName().toString().equals("target")) {
            return false;
        }
        Path deployment = location.getParent().getParent();
        if (deployment == null || !deployment.getFileName().toString().equals("deployment")) {
            return false;
        }
        return true;
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
