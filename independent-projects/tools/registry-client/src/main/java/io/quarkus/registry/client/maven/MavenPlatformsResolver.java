package io.quarkus.registry.client.maven;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.client.RegistryPlatformsResolver;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class MavenPlatformsResolver implements RegistryPlatformsResolver {

    private final RegistryPlatformsConfig config;
    private final MavenRegistryArtifactResolver artifactResolver;
    private final MessageWriter log;

    public MavenPlatformsResolver(RegistryPlatformsConfig config, MavenRegistryArtifactResolver artifactResolver,
            MessageWriter log) {
        this.config = Objects.requireNonNull(config);
        this.artifactResolver = Objects.requireNonNull(artifactResolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public PlatformCatalog resolvePlatforms(String quarkusVersion) throws RegistryResolutionException {
        final ArtifactCoords baseCoords = config.getArtifact();
        final Artifact catalogArtifact = new DefaultArtifact(baseCoords.getGroupId(), baseCoords.getArtifactId(),
                quarkusVersion, baseCoords.getType(), baseCoords.getVersion());
        log.debug("Resolving platform catalog %s", catalogArtifact);
        final Path jsonFile;
        try {
            jsonFile = artifactResolver.resolve(catalogArtifact);
        } catch (Exception e) {
            log.debug("Failed to resolve platform catalog %s", catalogArtifact);
            return null;
        }
        try {
            return JsonCatalogMapperHelper.deserialize(jsonFile, JsonPlatformCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException(
                    "Failed to load platform catalog from " + jsonFile, e);
        }
    }
}
