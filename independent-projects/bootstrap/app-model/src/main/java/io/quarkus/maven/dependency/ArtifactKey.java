package io.quarkus.maven.dependency;

public interface ArtifactKey {

    static ArtifactKey fromString(String s) {
        return GACT.fromString(s);
    }

    static ArtifactKey ga(String groupId, String artifactId) {
        return new GACT(groupId, artifactId);
    }

    static ArtifactKey gac(String groupId, String artifactId, String classifier) {
        return new GACT(groupId, artifactId, classifier);
    }

    static ArtifactKey gact(String groupId, String artifactId, String classifier, String type) {
        return new GACT(groupId, artifactId, classifier, type);
    }

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getType();

    default String toGacString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(getGroupId()).append(':').append(getArtifactId());
        if (!getClassifier().isEmpty()) {
            buf.append(':').append(getClassifier());
        }
        return buf.toString();
    }

}
