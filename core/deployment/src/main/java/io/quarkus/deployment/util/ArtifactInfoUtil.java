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

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

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
            URL jarLocation = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (jarLocation.toString().endsWith(".jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(Paths.get(jarLocation.toURI()),
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
                Path path = Paths.get(jarLocation.toURI());
                for (AppDependency i : curateOutcomeBuildItem.getEffectiveModel().getFullDeploymentDeps()) {
                    for (Path p : i.getArtifact().getPaths()) {
                        if (path.equals(p)) {

                            String artifactId = i.getArtifact().getArtifactId();
                            if (artifactId.endsWith(DEPLOYMENT)) {
                                artifactId = artifactId.substring(0, artifactId.length() - DEPLOYMENT.length());
                            }
                            return new AbstractMap.SimpleEntry<>(i.getArtifact().getGroupId(), artifactId);
                        }
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
     * Returns a Map.Entry containing the groupId and the artifactId of the module the contains the BuildItem
     * <p>
     * The way this works is by depending on the pom.properties file that should be present in the deployment jar
     *
     * @return the result, or null if no maven metadata were found
     */
    public static Map.Entry<String, String> groupIdAndArtifactId(FileSystem fs) throws IOException {
        Path metaInfPath = fs.getPath("/META-INF");
        return doGroupIdAndArtifactId(metaInfPath);
    }

    private static AbstractMap.SimpleEntry<String, String> doGroupIdAndArtifactId(Path metaInfPath) throws IOException {
        Optional<Path> pomProperties = Files.walk(metaInfPath)
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
