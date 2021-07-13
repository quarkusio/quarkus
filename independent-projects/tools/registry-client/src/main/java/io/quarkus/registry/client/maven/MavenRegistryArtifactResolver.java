package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;

public interface MavenRegistryArtifactResolver {

    Path resolve(Artifact artifact) throws BootstrapMavenException;

    Path findArtifactDirectory(Artifact artifact) throws BootstrapMavenException;

    String getLatestVersionFromRange(Artifact artifact, String versionRange) throws BootstrapMavenException;
}
