package io.quarkus.maven.dependency;

public interface ArtifactKey {

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
