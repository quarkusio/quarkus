package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Map;

public interface PlatformCatalog {

    Collection<Platform> getPlatforms();

    Map<String, Object> getMetadata();

    Platform getPlatform(String platformId);

    @JsonIgnore
    default Platform getRecommendedPlatform() {
        final Collection<Platform> platforms = getPlatforms();
        return platforms.isEmpty() ? null : platforms.iterator().next();
    }
}
