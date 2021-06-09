package io.quarkus.registry.client.maven;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.client.RegistryNonPlatformExtensionsResolver;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class MavenNonPlatformExtensionsResolver
        implements RegistryNonPlatformExtensionsResolver {

    private final RegistryNonPlatformExtensionsConfig config;
    private final MavenRegistryArtifactResolver artifactResolver;
    private final MessageWriter log;

    public MavenNonPlatformExtensionsResolver(RegistryNonPlatformExtensionsConfig config,
            MavenRegistryArtifactResolver artifactResolver, MessageWriter log) {
        this.config = Objects.requireNonNull(config);
        this.artifactResolver = Objects.requireNonNull(artifactResolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public ExtensionCatalog resolveNonPlatformExtensions(String quarkusVersion)
            throws RegistryResolutionException {
        final ArtifactCoords baseCoords = config.getArtifact();
        final Artifact catalogArtifact = new DefaultArtifact(baseCoords.getGroupId(),
                baseCoords.getArtifactId(), quarkusVersion, baseCoords.getType(), baseCoords.getVersion());
        log.debug("Resolving non-platform extension catalog %s", catalogArtifact);

        final Path jsonFile;
        try {
            jsonFile = artifactResolver.resolve(catalogArtifact);
        } catch (Exception e) {
            log.debug("Failed to resolve non-platform extension catalog %s", catalogArtifact);
            return null;
        }

        try {
            return JsonCatalogMapperHelper.deserialize(jsonFile, JsonExtensionCatalog.class);
        } catch (Exception e) {
            throw new RegistryResolutionException("Failed to load non-platform extension catalog from " + jsonFile, e);
        }
    }
}
