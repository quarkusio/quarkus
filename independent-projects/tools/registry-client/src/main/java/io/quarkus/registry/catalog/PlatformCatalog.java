package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

public interface PlatformCatalog {

    List<Platform> getPlatforms();

    Map<String, Object> getMetadata();

    @JsonIgnore
    default Platform getRecommendedPlatform() {
        final List<Platform> platforms = getPlatforms();
        return platforms.isEmpty() ? null : platforms.get(0);
    }
}
