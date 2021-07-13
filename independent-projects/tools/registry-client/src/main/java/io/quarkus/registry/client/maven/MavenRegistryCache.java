package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.client.RegistryCache;
import io.quarkus.registry.config.RegistryConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.eclipse.aether.artifact.DefaultArtifact;

public class MavenRegistryCache implements RegistryCache {

    private final RegistryConfig config;
    private final Collection<ArtifactCoords> artifacts;
    private final MavenRegistryArtifactResolver resolver;
    private final MessageWriter log;

    public MavenRegistryCache(RegistryConfig config, MavenRegistryArtifactResolver resolver,
            MessageWriter log) {
        this.config = config;
        this.artifacts = Arrays.asList(config.getDescriptor().getArtifact(),
                config.getNonPlatformExtensions().getArtifact(), config.getPlatforms().getArtifact());
        this.resolver = Objects.requireNonNull(resolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public void clearCache() throws RegistryResolutionException {
        log.debug("%s clearCache", config.getId());
        for (ArtifactCoords coords : artifacts) {
            final Path dir;
            try {
                dir = resolver.findArtifactDirectory(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                        coords.getClassifier(), coords.getType(), coords.getVersion()));
            } catch (BootstrapMavenException e) {
                throw new RegistryResolutionException("Failed to resolve " + coords + " locally", e);
            }
            if (Files.exists(dir)) {
                try {
                    Files.list(dir).forEach(path -> path.toFile().deleteOnExit());
                } catch (IOException e) {
                    throw new RegistryResolutionException("Failed to read directory " + dir, e);
                }
            }
        }
    }
}
