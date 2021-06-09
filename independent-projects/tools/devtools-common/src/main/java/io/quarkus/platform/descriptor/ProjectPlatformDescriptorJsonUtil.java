package io.quarkus.platform.descriptor;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonExtensionOrigin;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectPlatformDescriptorJsonUtil {

    public static JsonExtensionCatalog resolveCatalog(AppModelResolver resolver, List<AppArtifact> depConstraints)
            throws AppModelResolverException {
        final List<JsonExtensionCatalog> platforms = new ArrayList<>(2);
        final Set<ArtifactKey> processedPlatforms = new HashSet<>();
        for (int i = 0; i < depConstraints.size(); ++i) {
            final AppArtifact artifact = depConstraints.get(i);
            if (!artifact.getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)
                    && !artifact.getType().equals("json")) {
                continue;
            }
            if (!processedPlatforms.add(new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getClassifier(), artifact.getType()))) {
                continue;
            }
            final Path json = resolver.resolve(artifact);
            final JsonExtensionCatalog platform;
            try {
                platform = JsonCatalogMapperHelper.deserialize(json, JsonExtensionCatalog.class);
            } catch (IOException e) {
                throw new AppModelResolverException("Failed to deserialize a platform descriptor from " + json, e);
            }
            platform.getDerivedFrom().forEach(o -> processedPlatforms.add(ArtifactCoords.fromString(o.getId()).getKey()));
            platforms.add(platform);
        }
        if (platforms.isEmpty()) {
            return null;
        }

        final List<ExtensionOrigin> derivedFrom = new ArrayList<>();
        final List<Extension> extensions = new ArrayList<>();
        final Set<ArtifactKey> extensionKeys = new HashSet<>();
        final List<Category> categories = new ArrayList<>();
        final Set<String> categoryIds = new HashSet<>();
        final Map<String, Object> metadata = new HashMap<>();

        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        catalog.setPlatform(false);
        catalog.setDerivedFrom(derivedFrom);
        catalog.setExtensions(extensions);
        catalog.setCategories(categories);
        catalog.setMetadata(metadata);

        final JsonExtensionCatalog dominatingPlatform = platforms.get(0);
        catalog.setQuarkusCoreVersion(dominatingPlatform.getQuarkusCoreVersion());
        catalog.setUpstreamQuarkusCoreVersion(dominatingPlatform.getUpstreamQuarkusCoreVersion());

        for (int i = 0; i < platforms.size(); ++i) {
            final JsonExtensionCatalog platform = platforms.get(i);
            if (platform.getBom() != null) {
                catalog.setBom(platform.getBom());
            }
            final JsonExtensionOrigin origin = new JsonExtensionOrigin();
            origin.setId(platform.getId());
            origin.setPlatform(platform.isPlatform());
            origin.setBom(platform.getBom());
            derivedFrom.add(origin);

            for (Extension e : platform.getExtensions()) {
                if (extensionKeys.add(e.getArtifact().getKey())) {
                    extensions.add(e);
                }
            }

            for (Category c : platform.getCategories()) {
                if (categoryIds.add(c.getId())) {
                    categories.add(c);
                }
            }

            for (Map.Entry<String, Object> entry : platform.getMetadata().entrySet()) {
                if (!metadata.containsKey(entry.getKey())) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return catalog;
    }
}
