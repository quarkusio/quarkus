package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PlatformStreamInfo implements Serializable {

    private final String id;
    private final Map<String, PlatformReleaseInfo> releases = new HashMap<>();

    public PlatformStreamInfo(String stream) {
        this.id = stream;
    }

    public String getId() {
        return id;
    }

    boolean isAligned(Collection<AppArtifactCoords> importedBoms) {
        if (releases.isEmpty()) {
            return true;
        }
        for (PlatformReleaseInfo release : releases.values()) {
            if (release.getBoms().containsAll(importedBoms)) {
                return true;
            }
        }
        return false;
    }

    List<List<String>> getPossibleAlignemnts(Collection<AppArtifactCoords> importedPlatformBoms) {
        final Map<AppArtifactKey, String> importedKeys = new HashMap<>(importedPlatformBoms.size());
        for (AppArtifactCoords bom : importedPlatformBoms) {
            importedKeys.put(bom.getKey(), bom.getVersion());
        }
        final List<List<String>> suggestions = new ArrayList<>();
        for (PlatformReleaseInfo release : releases.values()) {
            final Map<AppArtifactKey, AppArtifactCoords> stackBoms = new HashMap<>(release.getBoms().size());
            for (AppArtifactCoords bom : release.getBoms()) {
                stackBoms.put(bom.getKey(), bom);
            }
            if (stackBoms.keySet().containsAll(importedKeys.keySet())) {
                final List<String> suggestion = new ArrayList<>(importedPlatformBoms.size());
                suggestions.add(suggestion);
                for (Map.Entry<AppArtifactKey, String> bomKey : importedKeys.entrySet()) {
                    final AppArtifactCoords stackBom = stackBoms.get(bomKey.getKey());
                    if (!bomKey.getValue().equals(stackBom.getVersion())) {
                        suggestion.add(bomKey.getKey().getGroupId() + ":" + bomKey.getKey().getArtifactId() + ":"
                                + bomKey.getValue() + " -> " + stackBom.getVersion());
                    } else {
                        suggestion
                                .add(stackBom.getGroupId() + ":" + stackBom.getArtifactId() + ":" + stackBom.getVersion());
                    }
                }
            }
        }
        return suggestions;
    }

    void addIfNotPresent(String version, Supplier<PlatformReleaseInfo> release) {
        if (!releases.containsKey(version)) {
            releases.put(version, release.get());
        }
    }

    Collection<PlatformReleaseInfo> getReleases() {
        return releases.values();
    }

    PlatformReleaseInfo getRelease(String version) {
        return releases.get(version);
    }
}