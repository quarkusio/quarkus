package io.quarkus.maven.dependency;

public interface ArtifactKey {

    static ArtifactKey fromString(String s) {
        return GACT.fromString(s);
    }

    static ArtifactKey of(String groupId, String artifactId, String classifier, String type) {
        return new GACT(groupId, artifactId, classifier, type);
    }

    /**
     * Creates an artifact key that consists of an artifact's groupId and artifactId.
     * The classifier and type will be left empty.
     *
     * @param groupId artifact groupId
     * @param artifactId artifact id
     * @return artifact key that consists of an artifact's groupId and artifactId
     */
    static ArtifactKey ga(String groupId, String artifactId) {
        return new GACT(groupId, artifactId);
    }

    /**
     * @deprecated
     *
     *             Creates an artifact key that consists of groupId:artifactId:classifer and a {@code null} type.
     *
     * @param groupId artifact groupId
     * @param artifactId artifact id
     * @param classifier artifact classifier
     * @return artifact key
     */
    @Deprecated(forRemoval = true)
    static ArtifactKey gac(String groupId, String artifactId, String classifier) {
        return new GACT(groupId, artifactId, classifier);
    }

    /**
     * @deprecated in favor of {@link ArtifactKey#of(String, String, String, String)}
     *
     *             Creates an artifact key for a given groupId:artifactId:classifier:type
     *
     * @param groupId artifact groupId
     * @param artifactId artifact id
     * @param classifier artifact classifier
     * @param type artifact type
     * @return artifact key
     */
    @Deprecated(forRemoval = true)
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
