package io.quarkus.modular.spi.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

/**
 * Factory for creating {@link ResolvedDependency} instances in tests.
 */
final class TestResolvedDependency {

    private TestResolvedDependency() {
    }

    /**
     * Create a minimal resolved dependency for testing.
     *
     * @param groupId the group ID (must not be {@code null})
     * @param artifactId the artifact ID (must not be {@code null})
     * @param version the version (must not be {@code null})
     * @return the resolved dependency (not {@code null})
     */
    static ResolvedDependency create(String groupId, String artifactId, String version) {
        return create(groupId, artifactId, version, Path.of("/tmp/fake/" + artifactId + "-" + version + ".jar"));
    }

    /**
     * Create a resolved dependency with empty content, for tests that walk the content tree.
     *
     * @param groupId the group ID (must not be {@code null})
     * @param artifactId the artifact ID (must not be {@code null})
     * @param version the version (must not be {@code null})
     * @return the resolved dependency (not {@code null})
     */
    static ResolvedDependency createEmpty(String groupId, String artifactId, String version) {
        try {
            Path dir = Files.createTempDirectory(artifactId);
            dir.toFile().deleteOnExit();
            return create(groupId, artifactId, version, dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a resolved dependency whose content is the given path.
     *
     * @param groupId the group ID (must not be {@code null})
     * @param artifactId the artifact ID (must not be {@code null})
     * @param version the version (must not be {@code null})
     * @param resolvedPath the artifact content, a JAR or a directory (must not be {@code null})
     * @return the resolved dependency (not {@code null})
     */
    static ResolvedDependency create(String groupId, String artifactId, String version, Path resolvedPath) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion(version)
                .setResolvedPath(resolvedPath)
                .build();
    }
}
