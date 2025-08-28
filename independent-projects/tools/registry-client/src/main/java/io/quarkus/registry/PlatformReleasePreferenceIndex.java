package io.quarkus.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maintains an index of platform releases and their preference order for a specific platform key.
 * <p>
 * This class is used to track the order in which platform releases (by version) are preferred for a given platform,
 * as well as the overall preference index of the platform itself.
 */
class PlatformReleasePreferenceIndex {

    private final String platformKey;
    private final int platformIndex;
    private final List<String> releaseVersions = new ArrayList<>(1);

    /**
     * Constructs a new PlatformReleasePreferenceIndex for the given platform key and index.
     *
     * @param platformKey the unique key identifying the platform (must not be null)
     * @param platformIndex the preference index of the platform
     */
    public PlatformReleasePreferenceIndex(String platformKey, int platformIndex) {
        this.platformKey = Objects.requireNonNull(platformKey, "Platform key is null");
        this.platformIndex = platformIndex;
    }

    /**
     * Returns the unique key identifying the platform.
     *
     * @return the platform key
     */
    String getPlatformKey() {
        return platformKey;
    }

    /**
     * Returns the preference index of the platform.
     *
     * @return the platform index
     */
    int getPlatformIndex() {
        return platformIndex;
    }

    /**
     * Returns the preference index of the given release version for this platform.
     * If the version is not already present, it is added to the end of the list.
     *
     * @param version the release version to look up or add
     * @return the index of the release version in the preference list
     */
    int getReleaseIndex(String version) {
        int i = releaseVersions.indexOf(version);
        if (i < 0) {
            i = releaseVersions.size();
            releaseVersions.add(version);
        }
        return i;
    }
}
