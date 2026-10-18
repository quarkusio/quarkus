package io.quarkus.modular.spi.model;

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
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId(groupId)
                .setArtifactId(artifactId)
                .setVersion(version)
                .setResolvedPath(Path.of("/tmp/fake/" + artifactId + "-" + version + ".jar"))
                .build();
    }
}
