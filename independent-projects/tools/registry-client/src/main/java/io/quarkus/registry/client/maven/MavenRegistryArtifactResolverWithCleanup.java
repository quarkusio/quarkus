package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.metadata.Metadata.Nature;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

public class MavenRegistryArtifactResolverWithCleanup implements MavenRegistryArtifactResolver {

    private final MavenArtifactResolver resolver;
    private final boolean cleanupTimestampedVersions;

    public MavenRegistryArtifactResolverWithCleanup(MavenArtifactResolver resolver, boolean cleanupOldTimestampedVersions) {
        this.resolver = Objects.requireNonNull(resolver, "resolver can't be null");
        this.cleanupTimestampedVersions = cleanupOldTimestampedVersions;
    }

    @Override
    public ArtifactResult resolveArtifact(Artifact artifact) throws BootstrapMavenException {
        return resolveAndCleanupOldTimestampedVersions(resolver, artifact, cleanupTimestampedVersions);
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

    @Override
    public org.apache.maven.artifact.repository.metadata.Metadata resolveMetadata(ArtifactResult result)
            throws BootstrapMavenException {
        final Artifact artifact = result.getArtifact();
        Metadata md = new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.isSnapshot() ? artifact.getBaseVersion() : artifact.getVersion(),
                "maven-metadata.xml", artifact.isSnapshot() ? Nature.SNAPSHOT : Nature.RELEASE);

        final MetadataRequest mdr = new MetadataRequest().setMetadata(md);
        final String repoId = result.getRepository().getId();
        if (repoId != null && !repoId.equals("local")) {
            for (RemoteRepository r : resolver.getRepositories()) {
                if (r.getId().equals(repoId)) {
                    mdr.setRepository(r);
                    break;
                }
            }
        }

        final List<MetadataResult> mdResults = resolver.getSystem().resolveMetadata(resolver.getSession(), Arrays.asList(mdr));
        if (!mdResults.isEmpty()) {
            md = mdResults.get(0).getMetadata();
            if (md != null && md.getFile() != null && md.getFile().exists()) {
                try (BufferedReader reader = new BufferedReader(new java.io.FileReader(md.getFile()))) {
                    return new MetadataXpp3Reader().read(reader);
                } catch (Exception e) {
                    // ignore for now
                }
            }
        }
        return null;
    }
}
