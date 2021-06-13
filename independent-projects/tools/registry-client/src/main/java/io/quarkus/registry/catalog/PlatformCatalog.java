package io.quarkus.registry.catalog;

import java.util.Collection;
import java.util.Map;

public interface PlatformCatalog {

    Collection<Platform> getPlatforms();

    Map<String, Object> getMetadata();

    Platform getPlatform(String platformId);

    default Platform getRecommendedPlatform() {
        final Collection<Platform> platforms = getPlatforms();
        return platforms.isEmpty() ? null : platforms.iterator().next();
    }
}
