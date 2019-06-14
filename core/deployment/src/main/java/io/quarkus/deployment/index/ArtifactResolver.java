package io.quarkus.deployment.index;

/**
 * Resolves maven artifacts for indexing.
 */
public interface ArtifactResolver {

    ResolvedArtifact getArtifact(String groupId, String artifactId, String classifier);

}
