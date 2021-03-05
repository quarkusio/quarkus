package io.quarkus.maven;

import java.io.Serializable;
import java.util.Objects;

public class ArtifactCoords implements Serializable {

    public static final String TYPE_JAR = "jar";
    public static final String TYPE_POM = "pom";

    public static ArtifactCoords fromString(String str) {
        return new ArtifactCoords(split(str, new String[5]));
    }

    public static ArtifactCoords pom(String groupId, String artifactId, String version) {
        return new ArtifactCoords(groupId, artifactId, null, TYPE_POM, version);
    }

    protected static String[] split(String str, String[] parts) {
        final int versionSep = str.lastIndexOf(':');
        if (versionSep <= 0 || versionSep == str.length() - 1) {
            throw new IllegalArgumentException("One of type, version or separating them ':' is missing from '" + str + "'");
        }
        parts[4] = str.substring(versionSep + 1);
        ArtifactKey.split(str, parts, versionSep);
        return parts;
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
        this(groupId, artifactId, "", TYPE_JAR, version);
    }

    public ArtifactCoords(String groupId, String artifactId, String type, String version) {
        this(groupId, artifactId, "", type, version);
    }

    public ArtifactCoords(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactCoords that = (ArtifactCoords) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(classifier, that.classifier) &&
                Objects.equals(type, that.type) &&
                Objects.equals(version, that.version);
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
}
