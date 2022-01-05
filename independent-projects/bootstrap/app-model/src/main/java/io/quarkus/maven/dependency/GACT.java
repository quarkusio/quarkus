package io.quarkus.maven.dependency;

import java.io.Serializable;
import java.util.Objects;

public class GACT implements ArtifactKey, Serializable {

    public static GACT fromString(String str) {
        return new GACT(split(str, new String[4], str.length()));
    }

    protected static String[] split(String str, String[] parts, int fromIndex) {
        int i = str.lastIndexOf(':', fromIndex - 1);
        if (i <= 0) {
            throw new IllegalArgumentException("GroupId and artifactId separating ':' is absent or not in the right place in '"
                    + str.substring(0, fromIndex) + "'");
        }
        parts[3] = str.substring(i + 1, fromIndex);
        fromIndex = i;
        i = str.lastIndexOf(':', fromIndex - 1);
        if (i < 0) {
            parts[0] = str.substring(0, fromIndex);
            if ((parts[1] = parts[3]).isEmpty()) {
                throw new IllegalArgumentException("ArtifactId is empty in `" + str + "`");
            }
            parts[2] = "";
            parts[3] = null;
            return parts;
        }
        if (i == 0) {
            throw new IllegalArgumentException(
                    "One of groupId or artifactId is missing from '" + str.substring(0, fromIndex) + "'");
        }
        if (i == fromIndex - 1) {
            parts[2] = "";
        } else {
            parts[2] = str.substring(i + 1, fromIndex);
        }

        fromIndex = i;
        i = str.lastIndexOf(':', fromIndex - 1);
        if (i < 0) {
            parts[0] = str.substring(0, fromIndex);
            if ((parts[1] = parts[2]).isEmpty()) {
                throw new IllegalArgumentException("ArtifactId is empty in `" + str + "`");
            }
            parts[2] = parts[3];
            parts[3] = null;
            return parts;
        }
        if (i == 0 || i == fromIndex - 1) {
            throw new IllegalArgumentException(
                    "One of groupId or artifactId is missing from '" + str.substring(0, fromIndex) + "'");
        }

        parts[0] = str.substring(0, i);
        parts[1] = str.substring(i + 1, fromIndex);
        if (parts[3].isEmpty()) {
            parts[3] = null;
        }
        return parts;
    }

    protected final String groupId;
    protected final String artifactId;
    protected final String classifier;
    protected final String type;

    public GACT(String[] parts) {
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

    public GACT(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public GACT(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, null);
    }

    public GACT(String groupId, String artifactId, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
        this.type = type;
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
    public int hashCode() {
        return Objects.hash(artifactId, classifier, groupId, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GACT other = (GACT) obj;
        return Objects.equals(artifactId, other.artifactId) && Objects.equals(classifier, other.classifier)
                && Objects.equals(groupId, other.groupId) && Objects.equals(type, other.type);
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
