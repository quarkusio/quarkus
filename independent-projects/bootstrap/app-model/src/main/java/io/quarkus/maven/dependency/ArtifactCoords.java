package io.quarkus.maven.dependency;

public interface ArtifactCoords {

    String TYPE_JAR = "jar";
    String TYPE_POM = "pom";

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getType();

    String getVersion();

    ArtifactKey getKey();

    default String toGACTVString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getClassifier() + ":" + getType() + ":" + getVersion();
    }

    default String toCompactCoords() {
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
