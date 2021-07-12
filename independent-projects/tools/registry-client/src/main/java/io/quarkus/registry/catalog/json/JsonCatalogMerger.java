package io.quarkus.registry.catalog.json;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonCatalogMerger {

    public static ExtensionCatalog merge(List<ExtensionCatalog> catalogs) {

        if (catalogs.isEmpty()) {
            throw new IllegalArgumentException("No catalogs provided");
        }
        if (catalogs.size() == 1) {
            return catalogs.get(0);
        }

        final List<ExtensionCatalog> roots = detectRoots(catalogs);
        if (roots.size() == 1) {
            return roots.get(0);
        }

        final JsonExtensionCatalog combined = new JsonExtensionCatalog();

        final Map<String, Category> categories = new LinkedHashMap<>();
        final Map<String, ExtensionOrigin> derivedFrom = new LinkedHashMap<>();
        final Map<ArtifactKey, Extension> extensions = new LinkedHashMap<>();
        final Map<String, Object> metadata = new HashMap<>();
        for (ExtensionCatalog catalog : roots) {
            if (combined.getBom() == null) {
                combined.setBom(catalog.getBom());
            }

            if (catalog.getId() != null) {
                derivedFrom.putIfAbsent(catalog.getId(), catalog);
            }
            catalog.getDerivedFrom().forEach(o -> derivedFrom.putIfAbsent(o.getId(), o));

            catalog.getCategories().forEach(c -> categories.putIfAbsent(c.getId(), c));
            catalog.getExtensions().forEach(e -> {
                final Extension copy = extensions.get(e.getArtifact().getKey());
                if (copy == null) {
                    extensions.put(e.getArtifact().getKey(), JsonExtension.copy(e));
                } else {
                    copy.getOrigins().addAll(e.getOrigins());
                }
            });
            catalog.getMetadata().entrySet().forEach(entry -> metadata.putIfAbsent(entry.getKey(), entry.getValue()));

            if (combined.getQuarkusCoreVersion() == null && catalog.getQuarkusCoreVersion() != null) {
                combined.setQuarkusCoreVersion(catalog.getQuarkusCoreVersion());
            }
            if (combined.getUpstreamQuarkusCoreVersion() == null && catalog.getUpstreamQuarkusCoreVersion() != null
                    && !combined.getQuarkusCoreVersion().equals(catalog.getUpstreamQuarkusCoreVersion())) {
                combined.setUpstreamQuarkusCoreVersion(catalog.getUpstreamQuarkusCoreVersion());
            }
        }

        combined.setCategories(new ArrayList<>(categories.values()));
        combined.setDerivedFrom(new ArrayList<>(derivedFrom.values()));
        combined.setExtensions(new ArrayList<>(extensions.values()));
        combined.setMetadata(metadata);
        return combined;
    }

    private static List<ExtensionCatalog> detectRoots(List<ExtensionCatalog> catalogs) {
        final Set<String> allDerivedFrom = new HashSet<>(catalogs.size());
        for (ExtensionCatalog catalog : catalogs) {
            for (ExtensionOrigin o : catalog.getDerivedFrom()) {
                allDerivedFrom.add(o.getId());
            }
        }
        final List<ExtensionCatalog> roots = new ArrayList<>(catalogs.size());
        for (ExtensionCatalog catalog : catalogs) {
            if (catalog.getId() == null) {
                roots.add(catalog);
            } else if (!allDerivedFrom.contains(catalog.getId())) {
                roots.add(catalog);
            }
        }
        return roots;
    }
}
