package io.quarkus.registry;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.json.JsonBuilder;
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
public class CatalogMergeUtility {

    // TODO: should be Package private
    public static ExtensionCatalog merge(List<? extends ExtensionCatalog> catalogs) {

        if (catalogs.isEmpty()) {
            throw new IllegalArgumentException("No catalogs provided");
        }
        if (catalogs.size() == 1) {
            return JsonBuilder.buildIfBuilder(catalogs.get(0));
        }

        final List<ExtensionCatalog> roots = detectRoots(catalogs);
        if (roots.size() == 1) {
            return JsonBuilder.buildIfBuilder(roots.get(0));
        }

        final ExtensionCatalog.Mutable combined = ExtensionCatalog.builder();

        final Map<String, Category> categories = new LinkedHashMap<>();
        final Map<String, ExtensionOrigin> derivedFrom = new LinkedHashMap<>();
        final Map<ArtifactKey, Extension.Mutable> extensions = new LinkedHashMap<>();
        final Map<String, Object> metadata = new HashMap<>();

        final Map<String, ExtensionCatalog> originCatalogs = new HashMap<>();
        catalogs.forEach(c -> {
            ExtensionCatalog catalog = JsonBuilder.buildIfBuilder(c);
            originCatalogs.put(catalog.getId(), catalog);
        });

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
                final Extension.Mutable copy = extensions.computeIfAbsent(e.getArtifact().getKey(),
                        k -> e.mutable().setOrigins(new ArrayList<>()));
                for (ExtensionOrigin origin : e.getOrigins()) {
                    ExtensionCatalog c = originCatalogs.get(origin.getId());
                    copy.getOrigins().add(c == null ? origin : c);
                }
            });

            catalog.getMetadata().forEach(metadata::putIfAbsent);

            if (combined.getQuarkusCoreVersion() == null && catalog.getQuarkusCoreVersion() != null) {
                combined.setQuarkusCoreVersion(catalog.getQuarkusCoreVersion());
            }

            if (combined.getUpstreamQuarkusCoreVersion() == null && catalog.getUpstreamQuarkusCoreVersion() != null
                    && !combined.getQuarkusCoreVersion().equals(catalog.getUpstreamQuarkusCoreVersion())) {
                combined.setUpstreamQuarkusCoreVersion(catalog.getUpstreamQuarkusCoreVersion());
            }
        }

        combined.setCategories(new ArrayList<>(categories.values()))
                .setDerivedFrom(new ArrayList<>(derivedFrom.values()))
                .setExtensions(new ArrayList<>(extensions.values()))
                .setMetadata(metadata);

        return combined.build();
    }

    // Package private.
    static PlatformCatalog mergePlatformCatalogs(List<PlatformCatalog> catalogs) {
        if (catalogs.isEmpty()) {
            throw new IllegalArgumentException("No catalogs provided");
        }
        if (catalogs.size() == 1) {
            return catalogs.get(0);
        }
        final PlatformCatalog.Mutable merged = PlatformCatalog.builder();
        final Map<String, Platform.Mutable> platformMap = new HashMap<>();

        for (PlatformCatalog c : catalogs) {
            for (Platform p : c.getPlatforms()) {
                final Platform.Mutable mergedPlatform = platformMap.computeIfAbsent(p.getPlatformKey(), k -> {
                    final Platform.Mutable pl = Platform.builder()
                            .setPlatformKey(k);
                    merged.addPlatform(pl);
                    return pl;
                });

                for (PlatformStream s : p.getStreams()) {
                    PlatformStream.Mutable mergedStream = (PlatformStream.Mutable) mergedPlatform.getStream(s.getId());
                    if (mergedStream == null) {
                        mergedStream = PlatformStream.builder()
                                .setId(s.getId());
                        mergedPlatform.addStream(mergedStream);
                    }
                    for (PlatformRelease r : s.getReleases()) {
                        final PlatformRelease release = mergedStream.getRelease(r.getVersion());
                        if (release == null) {
                            mergedStream.addRelease(r);
                        }
                    }
                    Map<String, Object> metadata = mergedStream.getMetadata();
                    s.getMetadata().forEach(metadata::putIfAbsent);
                }
                p.getMetadata().forEach(mergedPlatform.getMetadata()::putIfAbsent);
            }
            c.getMetadata().forEach(merged.getMetadata()::putIfAbsent);
        }
        return merged.build();
    }

    private static List<ExtensionCatalog> detectRoots(List<? extends ExtensionCatalog> catalogs) {
        final Set<String> allDerivedFrom = new HashSet<>(catalogs.size());
        for (ExtensionCatalog catalog : catalogs) {
            for (ExtensionOrigin o : catalog.getDerivedFrom()) {
                allDerivedFrom.add(o.getId());
            }
        }
        final List<ExtensionCatalog> roots = new ArrayList<>(catalogs.size());
        for (ExtensionCatalog catalog : catalogs) {
            if (catalog.getId() == null || !allDerivedFrom.contains(catalog.getId())) {
                roots.add(catalog);
            }
        }
        return roots;
    }
}
