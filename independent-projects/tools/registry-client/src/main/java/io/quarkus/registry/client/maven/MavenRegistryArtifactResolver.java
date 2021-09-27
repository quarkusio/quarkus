package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import java.nio.file.Path;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;

public interface MavenRegistryArtifactResolver {

    default Path resolve(Artifact artifact) throws BootstrapMavenException {
        return resolveArtifact(artifact).getArtifact().getFile().toPath();
    }

    ArtifactResult resolveArtifact(Artifact artifact) throws BootstrapMavenException;

    Path findArtifactDirectory(Artifact artifact) throws BootstrapMavenException;

    String getLatestVersionFromRange(Artifact artifact, String versionRange) throws BootstrapMavenException;

    Metadata resolveMetadata(ArtifactResult result) throws BootstrapMavenException;
}
