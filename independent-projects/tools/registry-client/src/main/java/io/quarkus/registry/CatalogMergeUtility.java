package io.quarkus.registry;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.registry.catalog.ExtensionImpl;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for merging catalog data.
 * Shared only within the package (between ExtensionCatalogResolvers)
 */
class CatalogMergeUtility {

    static ExtensionCatalog merge(List<ExtensionCatalog> catalogs) {

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

        final ExtensionCatalogImpl.Builder combined = new ExtensionCatalogImpl.Builder();

        final Map<String, Category> categories = new LinkedHashMap<>();
        final Map<String, ExtensionOrigin> derivedFrom = new LinkedHashMap<>();
        final Map<ArtifactKey, Extension> extensions = new LinkedHashMap<>();
        final Map<String, Object> metadata = new HashMap<>();

        for (ExtensionCatalog catalog : roots) {
            if (combined.getBom() == null) {
                combined.withBom(catalog.getBom());
            }

            if (catalog.getId() != null) {
                derivedFrom.putIfAbsent(catalog.getId(), catalog);
            }
            catalog.getDerivedFrom().forEach(o -> derivedFrom.putIfAbsent(o.getId(), o));

            catalog.getCategories().forEach(c -> categories.putIfAbsent(c.getId(), c));
            catalog.getExtensions().forEach(e -> {
                final Extension copy = extensions.get(e.getArtifact().getKey());
                if (copy == null) {
                    extensions.put(e.getArtifact().getKey(), ExtensionImpl.builder(e));
                } else {
                    copy.getOrigins().addAll(e.getOrigins());
                }
            });

            catalog.getMetadata().entrySet().forEach(entry -> metadata.putIfAbsent(entry.getKey(), entry.getValue()));

            if (combined.getQuarkusCoreVersion() == null
                    && catalog.getQuarkusCoreVersion() != null) {
                combined.withQuarkusCoreVersion(catalog.getQuarkusCoreVersion());
            }

            if (combined.getUpstreamQuarkusCoreVersion() == null
                    && catalog.getUpstreamQuarkusCoreVersion() != null
                    && !combined.getQuarkusCoreVersion().equals(catalog.getUpstreamQuarkusCoreVersion())) {
                combined.withUpstreamQuarkusCoreVersion(catalog.getUpstreamQuarkusCoreVersion());
            }
        }

        combined.withCategories(new ArrayList<>(categories.values()))
                .withDerivedFrom(new ArrayList<>(derivedFrom.values()))
                .withExtensions(new ArrayList<>(extensions.values()))
                .withMetadata(metadata);
        return combined;
    }

    public static PlatformCatalog mergePlatformCatalogs(List<PlatformCatalog> catalogs) {
        if (catalogs.isEmpty()) {
            throw new IllegalArgumentException("No catalogs provided");
        }
        if (catalogs.size() == 1) {
            return catalogs.get(0);
        }
        final JsonPlatformCatalog merged = new JsonPlatformCatalog();
        final Map<String, JsonPlatform> platformMap = new HashMap<>();
        for (PlatformCatalog c : catalogs) {
            for (Platform p : c.getPlatforms()) {
                final JsonPlatform mergedPlatform = platformMap.computeIfAbsent(p.getPlatformKey(), k -> {
                    final JsonPlatform pl = new JsonPlatform();
                    pl.setPlatformKey(p.getPlatformKey());
                    merged.addPlatform(pl);
                    return pl;
                });
                for (PlatformStream s : p.getStreams()) {
                    JsonPlatformStream mergedStream = (JsonPlatformStream) mergedPlatform.getStream(s.getId());
                    if (mergedStream == null) {
                        mergedStream = new JsonPlatformStream();
                        mergedStream.setId(s.getId());
                        mergedPlatform.addStream(mergedStream);
                    }
                    for (PlatformRelease r : s.getReleases()) {
                        final PlatformRelease release = mergedStream.getRelease(r.getVersion());
                        if (release == null) {
                            mergedStream.addRelease(r);
                        }
                    }
                    final Map<String, Object> mergedStreamMetadata = mergedStream.getMetadata();
                    s.getMetadata().entrySet()
                            .forEach(entry -> mergedStreamMetadata.putIfAbsent(entry.getKey(), entry.getValue()));
                }
                p.getMetadata().entrySet()
                        .forEach(entry -> mergedPlatform.getMetadata().putIfAbsent(entry.getKey(), entry.getValue()));
            }
            c.getMetadata().entrySet().forEach(entry -> merged.getMetadata().putIfAbsent(entry.getKey(), entry.getValue()));
        }
        return merged;
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
