package io.quarkus.registry.client.maven;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.client.RegistryPlatformsResolver;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;

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
        final ArtifactResult artifactResult;
        try {
            artifactResult = artifactResolver.resolveArtifact(catalogArtifact);
        } catch (Exception e) {
            log.debug("Failed to resolve platform catalog %s", catalogArtifact);
            return null;
        }
        final Path jsonFile = artifactResult.getArtifact().getFile().toPath();
        final JsonPlatformCatalog catalog;
        try {
            catalog = JsonCatalogMapperHelper.deserialize(jsonFile, JsonPlatformCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException(
                    "Failed to load platform catalog from " + jsonFile, e);
        }

        try {
            final Metadata mavenMetadata = artifactResolver.resolveMetadata(artifactResult);
            if (mavenMetadata != null) {
                final String lastUpdated = mavenMetadata.getVersioning() == null ? null
                        : mavenMetadata.getVersioning().getLastUpdated();
                if (lastUpdated != null) {
                    /*
                     * This is how it can be parsed
                     * java.util.TimeZone timezone = java.util.TimeZone.getTimeZone("UTC");
                     * java.text.DateFormat fmt = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
                     * fmt.setTimeZone(timezone);
                     * final Date date = fmt.parse(lastUpdated);
                     */
                    catalog.getMetadata().put(Constants.LAST_UPDATED, lastUpdated);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve Maven metadata for %s", catalogArtifact);
        }
        return catalog;
    }
}
