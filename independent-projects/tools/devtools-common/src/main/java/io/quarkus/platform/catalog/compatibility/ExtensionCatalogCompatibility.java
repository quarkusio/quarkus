package io.quarkus.platform.catalog.compatibility;

import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Extension catalog compatibility info.
 */
public class ExtensionCatalogCompatibility {

    /**
     * Collects extension compatibility info for a given catalog. This method simply calls
     * {@code forExtensions(catalog.getExtensions(), catalog)}.
     *
     * @param catalog extension catalog
     * @return extension compatibility info for all the extensions in the catalog
     */
    public static ExtensionCatalogCompatibility forCatalog(ExtensionCatalog catalog) {
        return forExtensions(catalog.getExtensions(), catalog);
    }

    /**
     * Collects extension compatibility info for specific extensions from the extension catalog.
     *
     * @param extensions extensions to collect the compatibility info for
     * @param catalog extension catalog
     * @return extension compatibility info for the provided extensions
     */
    public static ExtensionCatalogCompatibility forExtensions(Iterable<Extension> extensions, ExtensionCatalog catalog) {

        final Map<ArtifactKey, Extension> catalogExtMap = new HashMap<>(catalog.getExtensions().size());
        for (Extension e : catalog.getExtensions()) {
            catalogExtMap.put(e.getArtifact().getKey(), e);
        }

        final Map<ArtifactKey, Map<String, CapabilityInfo>> capInfoMap = new HashMap<>();
        final List<ExtensionCapabilityInfo> branches = new ArrayList<>();
        for (Extension e : extensions) {
            final List<ArtifactKey> depKeys = extensionDependencies(e.getMetadata());
            Map<String, CapabilityInfo> allCaps = Collections.emptyMap();
            Map<String, CapabilityInfo> extCaps = providedCapabilities(e, capInfoMap);
            if (!extCaps.isEmpty()) {
                allCaps = new HashMap<>(extCaps);
            }
            for (ArtifactKey key : depKeys) {
                final Extension dep = catalogExtMap.get(key);
                // normally, the catalog should contain all of them
                if (dep == null) {
                    continue;
                }
                extCaps = providedCapabilities(dep, capInfoMap);
                if (!extCaps.isEmpty()) {
                    if (allCaps.isEmpty()) {
                        allCaps = new HashMap<>(extCaps);
                    } else {
                        allCaps.putAll(extCaps);
                    }
                }
            }
            if (!allCaps.isEmpty()) {
                branches.add(new ExtensionCapabilityInfo(e, allCaps));
            }
        }

        List<ExtensionCompatibility> conflictingExtensions = Collections.emptyList();
        for (ExtensionCapabilityInfo extCapInfo : branches) {
            Map<ArtifactKey, Extension> conflicts = null;
            for (ExtensionCapabilityInfo otherExtCapInfo : branches) {
                if (otherExtCapInfo == extCapInfo) {
                    continue;
                }
                if (extCapInfo.isInConflictWith(otherExtCapInfo)) {
                    if (conflicts == null) {
                        conflicts = new HashMap<>();
                    }
                    conflicts.put(otherExtCapInfo.e.getArtifact().getKey(), otherExtCapInfo.e);
                }
            }
            if (conflicts != null) {
                if (conflictingExtensions.isEmpty()) {
                    conflictingExtensions = new ArrayList<>();
                }
                conflictingExtensions.add(new ExtensionCompatibility(extCapInfo.e, conflicts));
            }
        }
        return new ExtensionCatalogCompatibility(conflictingExtensions);
    }

    private final List<ExtensionCompatibility> compatibilityInfo;

    private ExtensionCatalogCompatibility(List<ExtensionCompatibility> conflictingExtensions) {
        this.compatibilityInfo = Objects.requireNonNull(conflictingExtensions);
    }

    public Collection<ExtensionCompatibility> getExtensionCompatibility() {
        return compatibilityInfo;
    }

    public boolean isEmpty() {
        return compatibilityInfo.isEmpty();
    }

    private static Map<String, CapabilityInfo> providedCapabilities(Extension e,
            Map<ArtifactKey, Map<String, CapabilityInfo>> capInfoMap) {
        Map<String, CapabilityInfo> map = capInfoMap.get(e.getArtifact().getKey());
        if (map == null) {
            final List<String> providedNames = providedCapabilities(e);
            if (providedNames.isEmpty()) {
                map = Collections.emptyMap();
            } else {
                map = new HashMap<>(providedNames.size());
                for (String name : providedNames) {
                    map.put(name, new CapabilityInfo(name, e));
                }
            }
            capInfoMap.put(e.getArtifact().getKey(), map);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static List<String> providedCapabilities(Extension e) {
        Map<?, ?> map = (Map<?, ?>) e.getMetadata().getOrDefault("capabilities", Collections.emptyMap());
        if (map.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> caps = (List<String>) map.get("provides");
        return caps == null ? Collections.emptyList() : caps;
    }

    @SuppressWarnings("unchecked")
    private static List<ArtifactKey> extensionDependencies(Map<String, Object> metadata) {
        final List<String> extDeps = (List<String>) metadata.getOrDefault("extension-dependencies", Collections.emptyList());
        if (extDeps.isEmpty()) {
            return Collections.emptyList();
        }
        return extDeps.stream().map(ArtifactKey::fromString).collect(Collectors.toList());
    }

    private static class ExtensionCapabilityInfo {
        final Extension e;
        final Map<String, CapabilityInfo> caps;

        ExtensionCapabilityInfo(Extension e, Map<String, CapabilityInfo> caps) {
            this.e = e;
            this.caps = caps;
        }

        boolean isInConflictWith(ExtensionCapabilityInfo other) {
            if (other.caps.isEmpty() || caps.isEmpty()) {
                return false;
            }
            for (CapabilityInfo otherCap : other.caps.values()) {
                final CapabilityInfo provided = caps.get(otherCap.name);
                if (provided != null && !provided.providerKey().equals(otherCap.providerKey())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class CapabilityInfo {

        final String name;
        final Extension provider;

        CapabilityInfo(String name, Extension provider) {
            this.name = Objects.requireNonNull(name);
            this.provider = Objects.requireNonNull(provider);
        }

        ArtifactKey providerKey() {
            return provider.getArtifact().getKey();
        }
    }
}
