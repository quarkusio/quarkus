package io.quarkus.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains an index of platform release preferences for multiple registries.
 * <p>
 * This class maps a registry index (typically representing a specific registry)
 * to a list of {@link PlatformReleasePreferenceIndex} instances, each corresponding
 * to a particular platform key. It provides efficient retrieval and creation of
 * platform release preference indices for use in managing platform preferences
 * across different registries.
 * </p>
 * <p>
 * Intended for internal use within the registry client to support platform preference
 * resolution logic.
 * </p>
 */
class PlatformPreferenceIndex {

    private final Map<Integer, List<PlatformReleasePreferenceIndex>> releaseIndices = new HashMap<>();

    PlatformReleasePreferenceIndex getReleaseIndex(int registryIndex, String platformKey) {
        var list = releaseIndices.computeIfAbsent(registryIndex, k -> new ArrayList<>(1));
        for (int i = 0; i < list.size(); ++i) {
            final PlatformReleasePreferenceIndex candidate = list.get(i);
            if (candidate.getPlatformKey().equals(platformKey)) {
                return candidate;
            }
        }
        final PlatformReleasePreferenceIndex result = new PlatformReleasePreferenceIndex(platformKey, list.size());
        list.add(result);
        return result;
    }
}
