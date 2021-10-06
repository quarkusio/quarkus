package io.quarkus.bootstrap.resolver;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsArtifact {

    public static final String DEFAULT_GROUP_ID = "io.quarkus.bootstrap.test";
    public static final String DEFAULT_VERSION = "1";

    public static final String TYPE_JAR = "jar";
    public static final String TYPE_POM = "pom";
    public static final String TYPE_TXT = "txt";

    public static final String EMPTY = "";

    private static final String MODEL_VERSION = "4.0.0";

    public static TsArtifact ga(String artifactId) {
        return ga(DEFAULT_GROUP_ID, artifactId);
    }

    public static TsArtifact ga(String groupId, String artifactId) {
        return new TsArtifact(groupId, artifactId, null);
    }

    public static TsArtifact jar(String artifactId) {
        return jar(artifactId, DEFAULT_VERSION);
    }

    public static TsArtifact jar(String artifactId, String version) {
        return new TsArtifact(DEFAULT_GROUP_ID, artifactId, EMPTY, TYPE_JAR, version);
    }

    public interface ContentProvider {
        Path getPath(Path workDir) throws IOException;
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;

    private List<TsDependency> deps = Collections.emptyList();
    private List<TsQuarkusExt> extDeps = Collections.emptyList();
    private List<TsDependency> managedDeps = Collections.emptyList();

    protected ContentProvider content;

    protected Properties pomProps;
    protected List<Profile> pomProfiles = Collections.emptyList();

    private boolean installed;

    public TsArtifact(String artifactId) {
        this(artifactId, DEFAULT_VERSION);
    }

    public TsArtifact(String artifactId, String version) {
        this(DEFAULT_GROUP_ID, artifactId, EMPTY, TYPE_TXT, version);
    }

    public TsArtifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, EMPTY, TYPE_TXT, version);
    }

    public TsArtifact(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.type = type;
        this.version = version;
    }

    public ArtifactKey getKey() {
        return new GACT(groupId, artifactId);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public TsArtifact setContent(ContentProvider content) {
        this.content = content;
        return this;
    }

    public TsArtifact addDependency(TsArtifact dep) {
        return addDependency(new TsDependency(dep));
    }

    public TsArtifact addDependency(TsArtifact dep, TsArtifact... excludes) {
        return addDependency(new TsDependency(dep).exclude(excludes));
    }

    public TsArtifact addDependency(TsQuarkusExt dep) {
        return addDependency(dep, false);
    }

    public TsArtifact addDependency(TsQuarkusExt dep, boolean optional) {
        return addDependency(dep, () -> new TsDependency(dep.getRuntime(), optional));
    }

    public TsArtifact addDependency(TsQuarkusExt dep, TsArtifact... excludes) {
        return addDependency(dep, () -> new TsDependency(dep.getRuntime(), false).exclude(excludes));
    }

    private TsArtifact addDependency(TsQuarkusExt dep, Supplier<TsDependency> dependencyFactory) {
        if (extDeps.isEmpty()) {
            extDeps = new ArrayList<>(1);
        }
        extDeps.add(dep);
        return addDependency(dependencyFactory.get());
    }

    public TsArtifact addDependency(TsDependency dep) {
        if (deps.isEmpty()) {
            deps = new ArrayList<>();
        }
        deps.add(dep);
        return this;
    }

    public TsArtifact addManagedDependency(TsDependency dep) {
        if (managedDeps.isEmpty()) {
            managedDeps = new ArrayList<>();
        }
        managedDeps.add(dep);
        return this;
    }

    public TsArtifact addProfile(Profile profile) {
        if (pomProfiles.isEmpty()) {
            pomProfiles = new ArrayList<>(1);
        }
        pomProfiles.add(profile);
        return this;
    }

    public String getArtifactFileName() {
        if (artifactId == null) {
            throw new IllegalArgumentException("artifactId is missing");
        }
        if (version == null) {
            throw new IllegalArgumentException("version is missing");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is missing");
        }
        final StringBuilder fileName = new StringBuilder();
        fileName.append(artifactId).append('-').append(version);
        if (classifier != null && !classifier.isEmpty()) {
            fileName.append('-').append(classifier);
        }
        fileName.append('.').append(type);
        return fileName.toString();
    }

    public TsArtifact toPomArtifact() {
        return new TsArtifact(groupId, artifactId, EMPTY, TYPE_POM, version);
    }

    public Model getPomModel() {
        final Model model = new Model();
        model.setModelVersion(MODEL_VERSION);

        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setPackaging(type);
        model.setVersion(version);

        if (pomProps != null) {
            model.setProperties(pomProps);
        }

        if (!deps.isEmpty()) {
            for (TsDependency dep : deps) {
                model.addDependency(dep.toPomDependency());
            }
        }

        if (!managedDeps.isEmpty()) {
            model.setDependencyManagement(new DependencyManagement());
            for (TsDependency dep : managedDeps) {
                model.getDependencyManagement().addDependency(dep.toPomDependency());
            }
        }

        if (!pomProfiles.isEmpty()) {
            model.setProfiles(pomProfiles);
        }
        return model;
    }

    public ArtifactCoords toArtifact() {
        return new GACTV(groupId, artifactId, classifier, type, version);
    }

    /**
     * Installs the artifact including its dependencies.
     *
     * @param repoBuilder
     */
    public void install(TsRepoBuilder repoBuilder) {
        if (installed) {
            return;
        }
        installed = true;
        if (!extDeps.isEmpty()) {
            for (TsQuarkusExt ext : extDeps) {
                ext.install(repoBuilder);
            }
        }
        if (!deps.isEmpty()) {
            for (TsDependency dep : deps) {
                if (dep.artifact.getVersion() != null) {
                    dep.artifact.install(repoBuilder);
                }
            }
        }
        try {
            repoBuilder.install(this, content == null ? null : content.getPath(repoBuilder.workDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to install " + this, e);
        }
    }

    public void setPomProperty(String name, String value) {
        if (pomProps == null) {
            pomProps = new Properties();
        }
        pomProps.setProperty(name, value);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(128);
        buf.append(groupId).append(':').append(artifactId).append(':').append(classifier).append(':').append(type).append(':')
                .append(version);
        return buf.toString();
    }
}
