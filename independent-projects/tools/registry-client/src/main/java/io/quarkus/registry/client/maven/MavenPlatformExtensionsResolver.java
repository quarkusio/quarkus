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
import java.util.List;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

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
            RemoteRepository repo = null;
            Throwable t = e;
            while (t != null) {
                if (t instanceof ArtifactNotFoundException) {
                    repo = ((ArtifactNotFoundException) t).getRepository();
                    break;
                }
                t = t.getCause();
            }
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to resolve Quarkus extension catalog ").append(catalogArtifact);
            if (repo != null) {
                buf.append(" from ").append(repo.getId()).append(" (").append(repo.getUrl()).append(")");
                final List<RemoteRepository> mirrored = repo.getMirroredRepositories();
                if (!mirrored.isEmpty()) {
                    buf.append(" which is a mirror of ");
                    buf.append(mirrored.get(0).getId()).append(" (").append(mirrored.get(0).getUrl()).append(")");
                    for (int i = 1; i < mirrored.size(); ++i) {
                        buf.append(", ").append(mirrored.get(i).getId()).append(" (").append(mirrored.get(i).getUrl())
                                .append(")");
                    }
                    buf.append(". The mirror may be out of sync.");
                }
            }
            throw new RegistryResolutionException(buf.toString(), e);
        }
        try {
            return JsonCatalogMapperHelper.deserialize(jsonPath, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to parse Quarkus extension catalog " + jsonPath, e);
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
