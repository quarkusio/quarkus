package io.quarkus.maven.dependency;

public interface ArtifactCoords {

    static ArtifactCoords fromString(String str) {
        return new GACTV(GACTV.split(str, new String[5]));
    }

    static ArtifactCoords pom(String groupId, String artifactId, String version) {
        return new GACTV(groupId, artifactId, null, TYPE_POM, version);
    }

    static ArtifactCoords jar(String groupId, String artifactId, String version) {
        return new GACTV(groupId, artifactId, null, TYPE_JAR, version);
    }

    static ArtifactCoords jar(String groupId, String artifactId, String classifier, String version) {
        return new GACTV(groupId, artifactId, classifier, TYPE_JAR, version);
    }

    static ArtifactCoords of(String groupId, String artifactId, String classifier, String type, String version) {
        return new GACTV(groupId, artifactId, classifier, type, version);
    }

    String TYPE_JAR = "jar";
    String TYPE_POM = "pom";
    String DEFAULT_CLASSIFIER = "";

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getType();

    String getVersion();

    ArtifactKey getKey();

    default boolean isJar() {
        return TYPE_JAR.equals(getType());
    }

    default boolean isSnapshot() {
        return getVersion() != null && getVersion().endsWith("-SNAPSHOT");
    }

    default String toGACTVString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getClassifier() + ":" + getType() + ":" + getVersion();
    }

    default String toCompactCoords() {
        final StringBuilder b = new StringBuilder();
        b.append(getGroupId()).append(':').append(getArtifactId()).append(':');
        if (!getClassifier().isEmpty()) {
            b.append(getClassifier()).append(':');
        }
        if (!isJar()) {
            b.append(getType()).append(':');
        }
        b.append(getVersion());
        return b.toString();
    }
}
