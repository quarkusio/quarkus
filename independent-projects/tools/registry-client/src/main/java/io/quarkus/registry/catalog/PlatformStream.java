package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

public interface PlatformStream {

    String getId();

    String getName();

    List<PlatformRelease> getReleases();

    Map<String, Object> getMetadata();

    @JsonIgnore
    default PlatformRelease getRecommendedRelease() {
        final List<PlatformRelease> releases = getReleases();
        if (releases.isEmpty()) {
            throw new RuntimeException("Stream " + getId() + " does not include any release");
        }
        return releases.get(0);
    }
}
