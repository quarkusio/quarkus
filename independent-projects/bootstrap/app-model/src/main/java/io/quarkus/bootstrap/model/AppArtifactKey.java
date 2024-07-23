package io.quarkus.bootstrap.model;

import java.io.Serializable;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

/**
 * GroupId, artifactId and classifier
 *
 * @deprecated in favor of {@link ArtifactKey}
 *
 * @author Alexey Loubyansky
 */
@Deprecated(forRemoval = true, since = "3.11.0")
public class AppArtifactKey implements ArtifactKey, Serializable {

    private static final long serialVersionUID = -6758193261385541101L;

    public static AppArtifactKey fromString(String str) {
        return new AppArtifactKey(GACT.split(str, new String[4], str.length()));
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;

    public AppArtifactKey(String[] parts) {
        this.groupId = parts[0];
        this.artifactId = parts[1];
        if (parts.length == 2 || parts[2] == null) {
            this.classifier = "";
        } else {
            this.classifier = parts[2];
        }
        if (parts.length <= 3 || parts[3] == null) {
            this.type = "jar";
        } else {
            this.type = parts[3];
        }
    }

    public AppArtifactKey(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public AppArtifactKey(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, null);
    }

    public AppArtifactKey(String groupId, String artifactId, String classifier, String type) {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ArtifactKey))
            return false;
        ArtifactKey other = (ArtifactKey) obj;
        if (artifactId == null) {
            if (other.getArtifactId() != null)
                return false;
        } else if (!artifactId.equals(other.getArtifactId()))
            return false;
        if (classifier == null) {
            if (other.getClassifier() != null)
                return false;
        } else if (!classifier.equals(other.getClassifier()))
            return false;
        if (groupId == null) {
            if (other.getGroupId() != null)
                return false;
        } else if (!groupId.equals(other.getGroupId()))
            return false;
        if (type == null) {
            return other.getType() == null;
        } else
            return type.equals(other.getType());
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

    public String toGacString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        if (!classifier.isEmpty()) {
            buf.append(':').append(classifier);
        }
        return buf.toString();
    }
}
