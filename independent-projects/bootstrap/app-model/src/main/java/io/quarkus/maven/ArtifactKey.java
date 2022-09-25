package io.quarkus.maven;

import java.io.Serializable;
import java.util.Objects;

import io.quarkus.maven.dependency.GACT;

@Deprecated(forRemoval = true)
/**
 * WARNING: DO NOT USE THIS CLASS
 * <p>
 *
 * @deprecated in favor of {@link io.quarkus.maven.dependency.ArtifactKey}
 */
public class ArtifactKey implements io.quarkus.maven.dependency.ArtifactKey, Serializable {

    public static ArtifactKey fromString(String str) {
        return new ArtifactKey(GACT.split(str, new String[4], str.length()));
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;

    public ArtifactKey(String[] parts) {
        this.groupId = parts[0];
        this.artifactId = parts[1];
        if (parts.length == 2 || parts[2] == null) {
            this.classifier = ArtifactCoords.DEFAULT_CLASSIFIER;
        } else {
            this.classifier = parts[2];
        }
        if (parts.length <= 3 || parts[3] == null) {
            this.type = ArtifactCoords.TYPE_JAR;
        } else {
            this.type = parts[3];
        }
    }

    public ArtifactKey(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public ArtifactKey(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, null);
    }

    public ArtifactKey(String groupId, String artifactId, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
        this.type = type;
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

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, classifier, groupId, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof io.quarkus.maven.dependency.ArtifactKey))
            return false;
        io.quarkus.maven.dependency.ArtifactKey other = (io.quarkus.maven.dependency.ArtifactKey) obj;
        return Objects.equals(artifactId, other.getArtifactId()) && Objects.equals(classifier, other.getClassifier())
                && Objects.equals(groupId, other.getGroupId()) && Objects.equals(type, other.getType());
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        if (!classifier.isEmpty()) {
            buf.append(':').append(classifier);
        } else if (type != null) {
            buf.append(':');
        }
        if (type != null) {
            buf.append(':').append(type);
        }
        return buf.toString();
    }
}
