package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.resolution.ArtifactResult;

public class MavenRegistryArtifactResolverWithCleanup implements MavenRegistryArtifactResolver {

    private final MavenArtifactResolver resolver;
    private final boolean cleanupTimestampedVersions;

    public MavenRegistryArtifactResolverWithCleanup(MavenArtifactResolver resolver, boolean cleanupOldTimestampedVersions) {
        this.resolver = Objects.requireNonNull(resolver, "resolver can't be null");
        this.cleanupTimestampedVersions = cleanupOldTimestampedVersions;
    }

    @Override
    public Path resolve(Artifact artifact) throws BootstrapMavenException {
        return resolveAndCleanupOldTimestampedVersions(resolver, artifact, cleanupTimestampedVersions).getArtifact().getFile()
                .toPath();
    }

    @Override
    public Path findArtifactDirectory(Artifact artifact) throws BootstrapMavenException {
        final LocalRepositoryManager localRepo = resolver.getSession().getLocalRepositoryManager();
        return localRepo.getRepository().getBasedir().toPath().resolve(localRepo.getPathForLocalArtifact(artifact))
                .getParent();
    }

    @Override
    public String getLatestVersionFromRange(Artifact artifact, String versionRange) throws BootstrapMavenException {
        return resolver.getLatestVersionFromRange(artifact, versionRange);
    }

    /**
     * This method resolves an artifact and then will attempt to remove old timestamped SNAPSHOT versions.
     *
     * <p>
     * IMPORTANT: it does not remove all the timestamped SNAPSHOT versions because otherwise, the artifacts
     * will always be resolved from a remote repository even if the update policy does not require that.
     *
     * @param resolver Maven artifact resolver
     * @param artifact artifact to resolve
     * @param cleanupOldTimestampedVersions whether to remove old timestamped SNAPSHOT versions
     * @return artifact resolution result
     * @throws BootstrapMavenException in case the artifact could not be resolved
     */
    protected static ArtifactResult resolveAndCleanupOldTimestampedVersions(MavenArtifactResolver resolver, Artifact artifact,
            boolean cleanupOldTimestampedVersions) throws BootstrapMavenException {
        if (!artifact.isSnapshot() || !cleanupOldTimestampedVersions) {
            return resolver.resolve(artifact);
        }

        final LocalRepositoryManager localRepoManager = resolver.getSession().getLocalRepositoryManager();
        final File jsonDir = new File(localRepoManager.getRepository().getBasedir(),
                localRepoManager.getPathForLocalArtifact(artifact)).getParentFile();
        final List<String> existingFiles = jsonDir.exists() ? Arrays.asList(jsonDir.list()) : Collections.emptyList();

        final ArtifactResult result = resolver.resolve(artifact);

        final File[] jsonDirContent = jsonDir.listFiles();
        if (jsonDirContent != null && jsonDirContent.length > existingFiles.size()) {
            final String fileName = result.getArtifact().getFile().getName();
            for (File c : jsonDirContent) {
                if (c.getName().length() > fileName.length()
                        && c.getName().startsWith(artifact.getArtifactId())
                        && c.getName().endsWith(artifact.getClassifier())
                        && existingFiles.contains(c.getName())) {
                    c.deleteOnExit();
                }
            }
        }
        return result;
    }
}
