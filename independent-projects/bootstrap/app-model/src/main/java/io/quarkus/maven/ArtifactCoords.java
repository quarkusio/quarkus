package io.quarkus.maven;

import java.io.Serializable;
import java.util.Objects;

import io.quarkus.maven.dependency.GACT;

@Deprecated(forRemoval = true)
/**
 * WARNING: DO NOT USE THIS CLASS
 * <p>
 *
 * @deprecated in favor of {@link io.quarkus.maven.dependency.ArtifactCoords}
 */
public class ArtifactCoords implements io.quarkus.maven.dependency.ArtifactCoords, Serializable {

    public static ArtifactCoords fromString(String str) {
        return new ArtifactCoords(split(str, new String[5]));
    }

    public static ArtifactCoords pom(String groupId, String artifactId, String version) {
        return new ArtifactCoords(groupId, artifactId, ArtifactCoords.DEFAULT_CLASSIFIER, TYPE_POM, version);
    }

    protected static String[] split(String str, String[] parts) {
        final int versionSep = str.lastIndexOf(':');
        if (versionSep <= 0 || versionSep == str.length() - 1) {
            throw new IllegalArgumentException("One of type, version or separating them ':' is missing from '" + str + "'");
        }
        parts[4] = str.substring(versionSep + 1);
        return GACT.split(str, parts, versionSep);
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;
    protected final String version;

    protected transient ArtifactKey key;

    protected ArtifactCoords(String[] parts) {
        groupId = parts[0];
        artifactId = parts[1];
        classifier = parts[2];
        type = parts[3] == null ? TYPE_JAR : parts[3];
        version = parts[4];
    }

    public ArtifactCoords(ArtifactKey key, String version) {
        this.key = key;
        this.groupId = key.getGroupId();
        this.artifactId = key.getArtifactId();
        this.classifier = key.getClassifier();
        this.type = key.getType();
        this.version = version;
    }

    public ArtifactCoords(String groupId, String artifactId, String version) {
        this(groupId, artifactId, ArtifactCoords.DEFAULT_CLASSIFIER, TYPE_JAR, version);
    }

    public ArtifactCoords(String groupId, String artifactId, String type, String version) {
        this(groupId, artifactId, ArtifactCoords.DEFAULT_CLASSIFIER, type, version);
    }

    public ArtifactCoords(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? ArtifactCoords.DEFAULT_CLASSIFIER : classifier;
        this.type = type == null ? TYPE_JAR : type;
        this.version = version;
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

    public ArtifactKey getKey() {
        return key == null ? key = new ArtifactKey(groupId, artifactId, classifier, type) : key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof io.quarkus.maven.dependency.ArtifactCoords)) {
            return false;
        }
        io.quarkus.maven.dependency.ArtifactCoords that = (io.quarkus.maven.dependency.ArtifactCoords) o;
        return Objects.equals(groupId, that.getGroupId()) &&
                Objects.equals(artifactId, that.getArtifactId()) &&
                Objects.equals(classifier, that.getClassifier()) &&
                Objects.equals(type, that.getType()) &&
                Objects.equals(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, type, version);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        append(buf);
        return buf.toString();
    }

    protected StringBuilder append(final StringBuilder buf) {
        buf.append(groupId).append(':').append(artifactId).append(':');
        if (classifier != null && !classifier.isEmpty()) {
            buf.append(classifier);
        }
        return buf.append(':').append(type).append(':').append(version);
    }

    public String toCompactCoords() {
        final StringBuilder b = new StringBuilder();
        b.append(getGroupId()).append(':').append(getArtifactId()).append(':');
        if (!getClassifier().isEmpty()) {
            b.append(getClassifier()).append(':');
        }
        if (!TYPE_JAR.equals(getType())) {
            b.append(getType()).append(':');
        }
        b.append(getVersion());
        return b.toString();
    }
}
