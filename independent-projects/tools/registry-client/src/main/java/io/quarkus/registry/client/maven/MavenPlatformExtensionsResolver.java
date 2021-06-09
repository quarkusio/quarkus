package io.quarkus.registry.client.maven;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.client.RegistryPlatformExtensionsResolver;
import io.quarkus.registry.util.PlatformArtifacts;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class MavenPlatformExtensionsResolver implements RegistryPlatformExtensionsResolver {

    private final MavenRegistryArtifactResolver artifactResolver;
    private final MessageWriter log;

    public MavenPlatformExtensionsResolver(MavenRegistryArtifactResolver artifactResolver,
            MessageWriter log) {
        this.artifactResolver = Objects.requireNonNull(artifactResolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platformCoords) throws RegistryResolutionException {
        final String version;
        if (platformCoords.getVersion() == null) {
            version = resolveLatestBomVersion(platformCoords, "[0-alpha,)");
        } else if (isVersionRange(platformCoords.getVersion())) {
            version = resolveLatestBomVersion(platformCoords, platformCoords.getVersion());
        } else {
            version = platformCoords.getVersion();
        }
        final String groupId = platformCoords.getGroupId();
        final String artifactId = PlatformArtifacts.ensureCatalogArtifactId(platformCoords.getArtifactId());
        final String classifier = version;
        final Artifact catalogArtifact = new DefaultArtifact(groupId, artifactId, classifier, "json", version);
        log.debug("Resolving platform extension catalog %s", catalogArtifact);
        final Path jsonPath;
        try {
            jsonPath = artifactResolver.resolve(catalogArtifact);
        } catch (Exception e) {
            throw new RegistryResolutionException("Failed to resolve Quarkus extensions catalog " + catalogArtifact,
                    e);
        }
        try {
            return JsonCatalogMapperHelper.deserialize(jsonPath, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to parse Quarkus extensions catalog " + jsonPath, e);
        }
    }

    private String resolveLatestBomVersion(ArtifactCoords bom, String versionRange)
            throws RegistryResolutionException {
        final Artifact bomArtifact = new DefaultArtifact(bom.getGroupId(),
                PlatformArtifacts.ensureBomArtifactId(bom.getArtifactId()),
                "", "pom", bom.getVersion());
        log.debug("Resolving the latest version of %s:%s:%s:%s in the range %s", bom.getGroupId(), bom.getArtifactId(),
                bom.getClassifier(), bom.getType(), versionRange);
        try {
            return artifactResolver.getLatestVersionFromRange(bomArtifact, versionRange);
        } catch (Exception e) {
            throw new RegistryResolutionException("Failed to resolve the latest version of " + bomArtifact.getGroupId()
                    + ":" + bom.getArtifactId() + ":" + bom.getClassifier() + ":" + bom.getType() + ":" + versionRange, e);
        }
    }

    private static boolean isVersionRange(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return false;
        }
        char c = versionStr.charAt(0);
        if (c == '[' || c == '(') {
            return true;
        }
        c = versionStr.charAt(versionStr.length() - 1);
        if (c == ']' || c == ')') {
            return true;
        }
        return versionStr.indexOf(',') >= 0;
    }
}
