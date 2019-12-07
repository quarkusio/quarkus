package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.util.PropertyUtils;


/**
 *
 * @author Alexey Loubyansky
 */
public class ModelUtils {

    private static final String STATE_ARTIFACT_INITIAL_VERSION = "1";

    /**
     * Returns the provisioning state artifact for the given application artifact
     *
     * @param appArtifact  application artifact
     * @return  provisioning state artifact
     */
    public static AppArtifact getStateArtifact(AppArtifact appArtifact) {
        return new AppArtifact(appArtifact.getGroupId() + ".quarkus.curate",
                appArtifact.getArtifactId(),
                "",
                "pom",
                STATE_ARTIFACT_INITIAL_VERSION);
    }

    /**
     * Filters out non-platform from application POM dependencies.
     *
     * @param deps  POM model application dependencies
     * @param appDeps  resolved application dependencies
     * @return  dependencies that can be checked for updates
     * @throws AppCreatorException  in case of a failure
     */
    public static List<AppDependency> getUpdateCandidates(List<Dependency> deps, List<AppDependency> appDeps, Set<String> groupIds) throws IOException {
        final Map<AppArtifactKey, AppDependency> appDepMap = new LinkedHashMap<>(appDeps.size());
        for(AppDependency appDep : appDeps) {
            final AppArtifact appArt = appDep.getArtifact();
            appDepMap.put(new AppArtifactKey(appArt.getGroupId(), appArt.getArtifactId(), appArt.getClassifier()), appDep);
        }
        final List<AppDependency> updateCandidates = new ArrayList<>(deps.size());
        // it's critical to preserve the order of the dependencies from the pom
        for(Dependency dep : deps) {
            if(!groupIds.contains(dep.getGroupId()) || "test".equals(dep.getScope())) {
                continue;
            }
            final AppDependency appDep = appDepMap.remove(new AppArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier()));
            if(appDep == null) {
                // This normally would be a dependency that's missing <scope>test</scope> in the artifact's pom
                // but is marked as such in one of artifact's parent poms
                //throw new AppCreatorException("Failed to locate dependency " + new AppArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion()) + " present in pom.xml among resolved application dependencies");
                continue;
            }
            updateCandidates.add(appDep);
        }
        for(AppDependency appDep : appDepMap.values()) {
            if(groupIds.contains(appDep.getArtifact().getGroupId())) {
                updateCandidates.add(appDep);
            }
        }
        return updateCandidates;
    }

    public static AppArtifact resolveAppArtifact(Path appJar) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, (ClassLoader) null)) {
            final Path metaInfMaven = fs.getPath("META-INF", "maven");
            if (Files.exists(metaInfMaven)) {
                try (DirectoryStream<Path> groupIds = Files.newDirectoryStream(metaInfMaven)) {
                    for (Path groupIdPath : groupIds) {
                        if (!Files.isDirectory(groupIdPath)) {
                            continue;
                        }
                        try (DirectoryStream<Path> artifactIds = Files.newDirectoryStream(groupIdPath)) {
                            for (Path artifactIdPath : artifactIds) {
                                if (!Files.isDirectory(artifactIdPath)) {
                                    continue;
                                }
                                final Path propsPath = artifactIdPath.resolve("pom.properties");
                                if (Files.exists(propsPath)) {
                                    final Properties props = loadPomProps(appJar, artifactIdPath);
                                    return new AppArtifact(props.getProperty("groupId"), props.getProperty("artifactId"), props.getProperty("version"));
                                }
                            }
                        }
                    }
                }
            }
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        }
    }

    public static Model readAppModel(Path appJar, AppArtifact appArtifact) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, (ClassLoader) null)) {
            final Path pomXml = fs.getPath("META-INF", "maven", appArtifact.getGroupId(), appArtifact.getArtifactId(), "pom.xml");
            if(!Files.exists(pomXml)) {
                throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
            }
            return readModel(pomXml);
        }
    }

    static Model readAppModel(Path appJar) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(appJar, (ClassLoader) null)) {
            final Path metaInfMaven = fs.getPath("META-INF", "maven");
            if (Files.exists(metaInfMaven)) {
                try (DirectoryStream<Path> groupIds = Files.newDirectoryStream(metaInfMaven)) {
                    for (Path groupIdPath : groupIds) {
                        if (!Files.isDirectory(groupIdPath)) {
                            continue;
                        }
                        try (DirectoryStream<Path> artifactIds = Files.newDirectoryStream(groupIdPath)) {
                            for (Path artifactIdPath : artifactIds) {
                                if (!Files.isDirectory(artifactIdPath)) {
                                    continue;
                                }
                                final Path pomXml = artifactIdPath.resolve("pom.xml");
                                if (Files.exists(pomXml)) {
                                    return readModel(pomXml);
                                }
                            }
                        }
                    }
                }
            }
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.xml in " + appJar);
        }
    }

    public static String getGroupId(Model model) {
        String groupId = model.getGroupId();
        if(groupId != null) {
            return groupId;
        }
        final Parent parent = model.getParent();
        if (parent != null) {
            groupId = parent.getGroupId();
            if(groupId != null) {
                return groupId;
            }
        }
        throw new IllegalStateException("Failed to determine groupId for project model");
    }

    public static String getVersion(Model model) {
        String version = model.getVersion();
        if(version != null) {
            return version;
        }
        final Parent parent = model.getParent();
        if (parent != null) {
            version = parent.getVersion();
            if(version != null) {
                return version;
            }
        }
        throw new IllegalStateException("Failed to determine version for project model");
    }

    /**
     * If the model contains properties, this method overrides those that appear to be
     * defined as system properties.
     */
    public static Model applySystemProperties(Model model) {
        final Properties props = model.getProperties();
        for(Map.Entry<Object, Object> prop : model.getProperties().entrySet()) {
            final String systemValue = PropertyUtils.getProperty(prop.getKey().toString());
            if(systemValue != null) {
                props.put(prop.getKey(), systemValue);
            }
        }
        return model;
    }

    private static Properties loadPomProps(Path appJar, Path artifactIdPath) throws IOException {
        final Path propsPath = artifactIdPath.resolve("pom.properties");
        if(!Files.exists(propsPath)) {
            throw new IOException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
        }
        final Properties props = new Properties();
        try(BufferedReader reader = Files.newBufferedReader(propsPath)) {
            props.load(reader);
        }
        return props;
    }

    public static Model readModel(final Path pomXml) throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(pomXml)) {
            return new MavenXpp3Reader().read(reader);
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse application POM model", e);
        }
    }

    public static void persistModel(Path pomFile, Model model) throws IOException {
        final MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
        try (BufferedWriter pomFileWriter = Files.newBufferedWriter(pomFile)) {
            xpp3Writer.write(pomFileWriter, model);
        }
    }
}
