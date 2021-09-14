package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Map;

public interface PlatformStream {

    String getId();

    String getName();

    Collection<PlatformRelease> getReleases();

    Map<String, Object> getMetadata();

    PlatformRelease getRelease(PlatformReleaseVersion version);

    @JsonIgnore
    default PlatformRelease getRecommendedRelease() {
        final Collection<PlatformRelease> releases = getReleases();
        if (releases.isEmpty()) {
            throw new RuntimeException("Stream " + getId() + " does not include any release");
        }
        return releases.iterator().next();
    }
}
