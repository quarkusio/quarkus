package io.quarkus.maven.dependency;

import java.io.Serializable;
import java.util.Objects;

public class GACTV implements ArtifactCoords, Serializable {

    private static final long serialVersionUID = -8362130311897578173L;

    public static GACTV fromString(String str) {
        return new GACTV(split(str, new String[5]));
    }

    public static ArtifactCoords pom(String groupId, String artifactId, String version) {
        return new GACTV(groupId, artifactId, null, TYPE_POM, version);
    }

    public static ArtifactCoords jar(String groupId, String artifactId, String version) {
        return new GACTV(groupId, artifactId, null, TYPE_JAR, version);
    }

    protected static String[] split(String str, String[] parts) {
        final int versionSep = str.lastIndexOf(':');
        if (versionSep <= 0 || versionSep == str.length() - 1) {
            throw new IllegalArgumentException("One of type, version or separating them ':' is missing from '" + str + "'");
        }
        parts[4] = str.substring(versionSep + 1);
        return GACT.split(str, parts, versionSep);
    }

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String type;
    private final String version;

    private transient ArtifactKey key;

    protected GACTV(String[] parts) {
        groupId = parts[0];
        artifactId = parts[1];
        classifier = parts[2] == null ? DEFAULT_CLASSIFIER : parts[2];
        type = parts[3] == null ? TYPE_JAR : parts[3];
        version = parts[4];
    }

    public GACTV(ArtifactKey key, String version) {
        this.key = key;
        this.groupId = key.getGroupId();
        this.artifactId = key.getArtifactId();
        this.classifier = key.getClassifier();
        this.type = key.getType();
        this.version = version;
    }

    public GACTV(String groupId, String artifactId, String version) {
        this(groupId, artifactId, DEFAULT_CLASSIFIER, TYPE_JAR, version);
    }

    public GACTV(String groupId, String artifactId, String type, String version) {
        this(groupId, artifactId, DEFAULT_CLASSIFIER, type, version);
    }

    public GACTV(String groupId, String artifactId, String classifier, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? DEFAULT_CLASSIFIER : classifier;
        this.type = type == null ? TYPE_JAR : type;
        this.version = version;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ArtifactKey getKey() {
        return key == null ? key = new GACT(groupId, artifactId, classifier, type) : key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, type, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ArtifactCoords)) {
            return false;
        }
        ArtifactCoords other = (ArtifactCoords) obj;
        return Objects.equals(artifactId, other.getArtifactId()) && Objects.equals(groupId, other.getGroupId())
                && Objects.equals(version, other.getVersion()) && Objects.equals(type, other.getType())
                && Objects.equals(classifier, other.getClassifier());
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
